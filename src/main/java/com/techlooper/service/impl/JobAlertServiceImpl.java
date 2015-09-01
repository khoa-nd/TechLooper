package com.techlooper.service.impl;

import com.techlooper.entity.JobAlertRegistrationEntity;
import com.techlooper.entity.ScrapeJobEntity;
import com.techlooper.model.JobAlertRegistration;
import com.techlooper.model.JobListingCriteria;
import com.techlooper.model.JobResponse;
import com.techlooper.model.Language;
import com.techlooper.repository.userimport.JobAlertRegistrationRepository;
import com.techlooper.repository.userimport.ScrapeJobRepository;
import com.techlooper.service.JobAlertService;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.dozer.Mapper;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.techlooper.util.DateTimeUtils.parseDate2String;
import static com.techlooper.util.DateTimeUtils.parseString2Date;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class JobAlertServiceImpl implements JobAlertService {

    public final static int NUMBER_OF_ITEMS_PER_PAGE = 10;

    private static final long CONFIGURED_JOB_ALERT_LIMIT_REGISTRATION = 5;

    public final static int JOB_ALERT_SENT_OK = 200;

    public final static int JOB_ALERT_JOB_NOT_FOUND = 400;

    public final static int JOB_ALERT_ALREADY_SENT_ON_TODAY = 301;

    @Value("${jobAlert.period}")
    private int CONFIGURED_JOB_ALERT_PERIOD;

    @Value("${jobAlert.launchDate}")
    private String CONFIGURED_JOB_ALERT_LAUNCH_DATE;

    @Resource
    private ScrapeJobRepository scrapeJobRepository;

    @Resource
    private JobAlertRegistrationRepository jobAlertRegistrationRepository;

    @Resource
    private Mapper dozerMapper;

    @Value("${jobAlert.subject.en}")
    private String jobAlertMailSubjectEn;

    @Value("${jobAlert.subject.vi}")
    private String jobAlertMailSubjectVn;

    @Resource
    private Template jobAlertMailTemplateEn;

    @Resource
    private Template jobAlertMailTemplateVi;

    @Resource
    private MimeMessage jobAlertMailMessage;

    @Value("${web.baseUrl}")
    private String webBaseUrl;

    @Resource
    private JavaMailSender mailSender;

    @Override
    public List<ScrapeJobEntity> searchJob(JobAlertRegistrationEntity jobAlertRegistrationEntity) {
        NativeSearchQueryBuilder searchQueryBuilder = getJobAlertSearchQueryBuilder(jobAlertRegistrationEntity);
        List<ScrapeJobEntity> jobs = scrapeJobRepository.search(searchQueryBuilder.build()).getContent();
        return jobs;
    }

    @Override
    public Long countJob(JobAlertRegistrationEntity jobAlertRegistrationEntity) {
        NativeSearchQueryBuilder searchQueryBuilder = getJobAlertSearchQueryBuilder(jobAlertRegistrationEntity);
        return scrapeJobRepository.search(searchQueryBuilder.build()).getTotalElements();
    }

    @Override
    public JobAlertRegistrationEntity registerJobAlert(JobAlertRegistration jobAlertRegistration) throws Exception {
        JobAlertRegistrationEntity jobAlertRegistrationEntity =
                dozerMapper.map(jobAlertRegistration, JobAlertRegistrationEntity.class);

        Date currentDate = new Date();
        jobAlertRegistrationEntity.setJobAlertRegistrationId(currentDate.getTime());
        jobAlertRegistrationEntity.setCreatedDate(parseDate2String(currentDate, "dd/MM/yyyy"));
        jobAlertRegistrationEntity.setBucket(getJobAlertBucketNumber(CONFIGURED_JOB_ALERT_PERIOD));
        return jobAlertRegistrationRepository.save(jobAlertRegistrationEntity);
    }

    @Override
    public List<JobAlertRegistrationEntity> searchJobAlertRegistration(int period) throws Exception {
        List<JobAlertRegistrationEntity> jobAlertRegistrationEntities = new ArrayList<>();
        int bucketNumber = getJobAlertBucketNumber(CONFIGURED_JOB_ALERT_PERIOD);

        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("jobAlertRegistration");
        searchQueryBuilder.withQuery(QueryBuilders.termQuery("bucket", bucketNumber));

        int totalPages = jobAlertRegistrationRepository.search(searchQueryBuilder.build()).getTotalPages();
        int pageIndex = 0;
        while (pageIndex < totalPages) {
            searchQueryBuilder.withPageable(new PageRequest(pageIndex, 50));
            List<JobAlertRegistrationEntity> result = jobAlertRegistrationRepository.search(searchQueryBuilder.build()).getContent();
            jobAlertRegistrationEntities.addAll(result);
            pageIndex++;
        }

        return jobAlertRegistrationEntities;
    }

    @Override
    public void sendEmail(Long numberOfJobs, JobAlertRegistrationEntity jobAlertRegistrationEntity, List<ScrapeJobEntity> scrapeJobEntities) throws Exception {
        String mailSubject = jobAlertRegistrationEntity.getLang() == Language.vi ? jobAlertMailSubjectVn : jobAlertMailSubjectEn;
        Address[] recipientAddresses = InternetAddress.parse(jobAlertRegistrationEntity.getEmail());
        Template template = jobAlertRegistrationEntity.getLang() == Language.vi ? jobAlertMailTemplateVi : jobAlertMailTemplateEn;

        jobAlertMailMessage.setRecipients(Message.RecipientType.TO, recipientAddresses);
        StringWriter stringWriter = new StringWriter();

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("webBaseUrl", webBaseUrl);
        templateModel.put("email", jobAlertRegistrationEntity.getEmail());
        templateModel.put("numberOfJobs", numberOfJobs);
        templateModel.put("keyword", jobAlertRegistrationEntity.getKeyword());
        templateModel.put("location", jobAlertRegistrationEntity.getLocation());
        templateModel.put("locationId", jobAlertRegistrationEntity.getLocationId());
        templateModel.put("jobs", scrapeJobEntities.stream().limit(NUMBER_OF_ITEMS_PER_PAGE).collect(Collectors.toList()));
        templateModel.put("searchPath", buildSearchPath(jobAlertRegistrationEntity));

        template.process(templateModel, stringWriter);
        jobAlertMailMessage.setSubject(MimeUtility.encodeText(mailSubject, "UTF-8", null));
        jobAlertMailMessage.setText(stringWriter.toString(), "UTF-8", "html");

        stringWriter.flush();
        jobAlertMailMessage.saveChanges();
        mailSender.send(jobAlertMailMessage);


        jobAlertRegistrationEntity.setLastEmailSentDateTime(parseDate2String(new Date(), "dd/MM/yyyy HH:mm"));
        jobAlertRegistrationEntity.setLastEmailSentCode(JOB_ALERT_SENT_OK);
        jobAlertRegistrationRepository.save(jobAlertRegistrationEntity);
    }

    @Override
    public List<JobResponse> listJob(JobListingCriteria criteria) {
        List<JobResponse> result = new ArrayList<>();

        NativeSearchQueryBuilder searchQueryBuilder = getJobListingQueryBuilder(criteria);
        List<ScrapeJobEntity> jobs = scrapeJobRepository.search(searchQueryBuilder.build()).getContent();
        if (!jobs.isEmpty()) {
            for (ScrapeJobEntity jobEntity : jobs) {
                JobResponse.Builder builder = new JobResponse.Builder();
                JobResponse job = builder.withUrl(jobEntity.getJobTitleUrl())
                        .withTitle(jobEntity.getJobTitle())
                        .withCompany(jobEntity.getCompany())
                        .withLocation(jobEntity.getLocation())
                        .withSalary(jobEntity.getSalary())
                        .withPostedOn(jobEntity.getCreatedDateTime())
                        .withTopPriority(jobEntity.getTopPriority() != null ? jobEntity.getTopPriority() : false)
                        .withLogoUrl(jobEntity.getCompanyLogoUrl())
                        .withBenefits(jobEntity.getBenefits())
                        .withSkills(jobEntity.getSkills())
                        .withSalaryMin(jobEntity.getSalaryMin())
                        .withSalaryMax(jobEntity.getSalaryMax()).build();
                result.add(job);
            }
        }
        return result;
    }

    @Override
    public List<JobResponse> listNormalJob(JobListingCriteria criteria, int limit, int totalPage) {
        List<JobResponse> result = new ArrayList<>();
        int pageNumber = 1;
        while (result.size() < limit && pageNumber <= totalPage) {
            criteria.setPage(pageNumber);
            List<JobResponse> normalJobs = listJob(criteria).stream().filter(job -> job.getTopPriority() == null || job.getTopPriority() == false)
                    .collect(Collectors.toList());
            if (!normalJobs.isEmpty()) {
                result.addAll(normalJobs);
            }
            pageNumber++;
        }
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public Long countJob(JobListingCriteria criteria) {
        NativeSearchQueryBuilder searchQueryBuilder = getJobListingQueryBuilder(criteria);
        return scrapeJobRepository.search(searchQueryBuilder.build()).getTotalElements();
    }

    private NativeSearchQueryBuilder getJobListingQueryBuilder(JobListingCriteria criteria) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("job");

        QueryBuilder queryBuilder = null;
        if (StringUtils.isEmpty(criteria.getKeyword()) && StringUtils.isEmpty(criteria.getLocation())) {
            queryBuilder = matchAllQuery();
        } else {
            queryBuilder = boolQuery();

            if (StringUtils.isNotEmpty(criteria.getKeyword())) {
                ((BoolQueryBuilder) queryBuilder).must(multiMatchQuery(criteria.getKeyword(), "jobTitle", "company"));
            }

            if (StringUtils.isNotEmpty(criteria.getLocation())) {
                ((BoolQueryBuilder) queryBuilder).should(matchQuery("location", criteria.getLocation()));
            }
        }

        searchQueryBuilder.withQuery(filteredQuery(queryBuilder, FilterBuilders.rangeFilter("createdDateTime").from("now-30d/d")));
        searchQueryBuilder.withSort(SortBuilders.fieldSort("topPriority").order(SortOrder.DESC));
        searchQueryBuilder.withSort(SortBuilders.scoreSort());
        searchQueryBuilder.withSort(SortBuilders.fieldSort("createdDateTime").order(SortOrder.DESC));
        searchQueryBuilder.withPageable(new PageRequest(criteria.getPage() - 1, NUMBER_OF_ITEMS_PER_PAGE));
        return searchQueryBuilder;
    }

    private int getJobAlertBucketNumber(int period) throws Exception {
        Date currentDate = new Date();
        Date launchDate = parseString2Date(CONFIGURED_JOB_ALERT_LAUNCH_DATE, "dd/MM/yyyy");
        int numberOfDays = Days.daysBetween(new DateTime(launchDate), new DateTime(currentDate)).getDays();
        return numberOfDays % period;
    }

//    private List<ScrapeJobEntity> sortJobByDescendingStartDate(List<ScrapeJobEntity> jobs) {
//        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
//        return jobs.stream().sorted((job1, job2) -> {
//            try {
//                long challenge2StartDate = sdf.parse(job2.getCreatedDateTime()).getTime();
//                long challenge1StartDate = sdf.parse(job1.getCreatedDateTime()).getTime();
//                if (challenge2StartDate - challenge1StartDate > 0) {
//                    return 1;
//                } else if (challenge2StartDate - challenge1StartDate < 0) {
//                    return -1;
//                } else {
//                    return 0;
//                }
//            } catch (ParseException e) {
//                return 0;
//            }
//        }).collect(Collectors.toList());
//    }

    public boolean checkIfUserExceedRegistrationLimit(String email) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("jobAlertRegistration");

        if (StringUtils.isNotEmpty(email)) {
            searchQueryBuilder.withQuery(QueryBuilders.matchPhraseQuery("email", email));
        }

        long numberOfRegistrations = jobAlertRegistrationRepository.search(searchQueryBuilder.build()).getTotalElements();
        return numberOfRegistrations >= CONFIGURED_JOB_ALERT_LIMIT_REGISTRATION;
    }

    @Override
    public void updateSendEmailResultCode(JobAlertRegistrationEntity jobAlertRegistrationEntity, Integer code) {
        if (jobAlertRegistrationEntity != null) {
            jobAlertRegistrationEntity.setLastEmailSentCode(code);
            jobAlertRegistrationRepository.save(jobAlertRegistrationEntity);
        }
    }

    private NativeSearchQueryBuilder getJobAlertSearchQueryBuilder(JobAlertRegistrationEntity jobAlertRegistrationEntity) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("job");

        QueryBuilder queryBuilder = null;
        if (StringUtils.isEmpty(jobAlertRegistrationEntity.getKeyword()) && StringUtils.isEmpty(jobAlertRegistrationEntity.getLocation())) {
            queryBuilder = matchAllQuery();
        } else {
            queryBuilder = boolQuery();

            if (StringUtils.isNotEmpty(jobAlertRegistrationEntity.getKeyword())) {
                ((BoolQueryBuilder) queryBuilder).must(multiMatchQuery(jobAlertRegistrationEntity.getKeyword(), "jobTitle", "company"));
            }

            if (StringUtils.isNotEmpty(jobAlertRegistrationEntity.getLocation())) {
                ((BoolQueryBuilder) queryBuilder).should(matchQuery("location", jobAlertRegistrationEntity.getLocation()));
            }
        }

        searchQueryBuilder.withQuery(filteredQuery(queryBuilder, FilterBuilders.rangeFilter("createdDateTime").from("now-7d/d")));
        searchQueryBuilder.withSort(SortBuilders.fieldSort("topPriority").order(SortOrder.DESC));
        searchQueryBuilder.withSort(SortBuilders.scoreSort());
        searchQueryBuilder.withSort(SortBuilders.fieldSort("createdDateTime").order(SortOrder.DESC));
        searchQueryBuilder.withPageable(new PageRequest(0, 5));
        return searchQueryBuilder;
    }

    private String buildSearchPath(JobAlertRegistrationEntity jobAlertRegistrationEntity) {
        StringBuilder pathBuilder = new StringBuilder("");
        if (StringUtils.isNotEmpty(jobAlertRegistrationEntity.getKeyword())) {
            pathBuilder.append(jobAlertRegistrationEntity.getKeyword().replaceAll("\\W", "-"));
        }

        if (jobAlertRegistrationEntity.getLocationId() != null) {
            pathBuilder.append("+");
            pathBuilder.append(jobAlertRegistrationEntity.getLocationId());
        }

        if (StringUtils.isNotEmpty(jobAlertRegistrationEntity.getLocation())) {
            pathBuilder.append("+");
            pathBuilder.append(jobAlertRegistrationEntity.getLocation().replaceAll("\\W", "-"));
        }

        return pathBuilder.toString();
    }
}