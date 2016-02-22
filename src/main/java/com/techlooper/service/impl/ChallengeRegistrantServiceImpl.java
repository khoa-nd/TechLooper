package com.techlooper.service.impl;

import com.techlooper.dto.ChallengeQualificationDto;
import com.techlooper.dto.ChallengeWinnerDto;
import com.techlooper.dto.DraftRegistrantDto;
import com.techlooper.dto.RejectRegistrantDto;
import com.techlooper.entity.*;
import com.techlooper.model.*;
import com.techlooper.repository.elasticsearch.ChallengeRegistrantRepository;
import com.techlooper.repository.elasticsearch.ChallengeRepository;
import com.techlooper.repository.elasticsearch.ChallengeSubmissionRepository;
import com.techlooper.repository.elasticsearch.DraftRegistrantRepository;
import com.techlooper.service.ChallengeEmailService;
import com.techlooper.service.ChallengeRegistrantService;
import com.techlooper.service.ChallengeService;
import com.techlooper.service.ChallengeSubmissionService;
import com.techlooper.util.DataUtils;
import com.techlooper.util.DateTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.dozer.Mapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.techlooper.model.ChallengePhaseEnum.*;
import static com.techlooper.util.DateTimeUtils.currentDate;
import static com.techlooper.util.DateTimeUtils.daysBetween;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;

@Service
public class ChallengeRegistrantServiceImpl implements ChallengeRegistrantService {

    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    private final List<ChallengePhaseEnum> CHALLENGE_PHASES = Arrays.asList(FINAL, PROTOTYPE, UIUX, IDEA);

    @Resource
    private ChallengeService challengeService;

    @Resource
    private ChallengeRegistrantRepository challengeRegistrantRepository;

    @Resource
    private Mapper dozerMapper;

    @Resource
    private ChallengeSubmissionRepository challengeSubmissionRepository;

    @Resource
    private ChallengeSubmissionService challengeSubmissionService;

    @Resource
    private ChallengeRepository challengeRepository;

    @Resource
    private ChallengeEmailService challengeEmailService;

    @Resource
    private DraftRegistrantRepository draftRegistrantRepository;

    public Map<ChallengePhaseEnum, ChallengeRegistrantPhaseItem> countNumberOfRegistrantsByPhase(Long challengeId) {
        Map<ChallengePhaseEnum, ChallengeRegistrantPhaseItem> numberOfRegistrantsByPhase = new HashMap<>();

        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withIndices("techlooper")
                .withTypes("challengeRegistrant").withSearchType(SearchType.COUNT);
        searchQueryBuilder.withQuery(termQuery("challengeId", challengeId));

        Long numberOfRegistrants = elasticsearchTemplate.count(searchQueryBuilder.build());
        numberOfRegistrantsByPhase.put(REGISTRATION, new ChallengeRegistrantPhaseItem(REGISTRATION, numberOfRegistrants));

        searchQueryBuilder.addAggregation(AggregationBuilders.terms("sumOfRegistrants").field("activePhase"));
        Aggregations aggregations = elasticsearchTemplate.query(searchQueryBuilder.build(), SearchResponse::getAggregations);
        Terms terms = aggregations.get("sumOfRegistrants");


        Long previousPhase = 0L;
        for (ChallengePhaseEnum phaseEnum : CHALLENGE_PHASES) {
            Terms.Bucket bucket = terms.getBucketByKey(phaseEnum.getValue());
            if (bucket != null) {
                numberOfRegistrantsByPhase.put(phaseEnum, new ChallengeRegistrantPhaseItem(phaseEnum,
                        bucket.getDocCount() + previousPhase));
                previousPhase = bucket.getDocCount() + previousPhase;
            } else {
                bucket = terms.getBucketByKey(phaseEnum.getValue().toLowerCase());
                if (bucket != null) {
                    numberOfRegistrantsByPhase.put(phaseEnum, new ChallengeRegistrantPhaseItem(phaseEnum,
                            bucket.getDocCount() + previousPhase));
                    previousPhase = bucket.getDocCount() + previousPhase;
                } else {
                    numberOfRegistrantsByPhase.put(phaseEnum, new ChallengeRegistrantPhaseItem(phaseEnum, previousPhase));
                }
            }
        }
        return numberOfRegistrantsByPhase;
    }

    public Long countNumberOfFinalists(Long challengeId) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withIndices("techlooper")
                .withTypes("challengeRegistrant").withSearchType(SearchType.COUNT);

        BoolFilterBuilder boolFilterBuilder = boolFilter();
        boolFilterBuilder.must(termFilter("challengeId", challengeId));
        boolFilterBuilder.must(termFilter("activePhase", FINAL.getValue()));
        boolFilterBuilder.mustNot(missingFilter("criteria.score"));
        boolFilterBuilder.mustNot(termFilter("disqualified", true));

        searchQueryBuilder.withQuery(filteredQuery(matchAllQuery(), boolFilterBuilder));
        return elasticsearchTemplate.count(searchQueryBuilder.build());
    }

    @Override
    public int countNumberOfWinners(Long challengeId) {
        ChallengeEntity challengeEntity = challengeRepository.findOne(challengeId);
        if (challengeEntity != null) {
            return challengeEntity.getWinners().isEmpty() ? 0 : challengeEntity.getWinners().size();
        }
        return 0;
    }

    public Set<ChallengeRegistrantDto> findRegistrantsByChallengeIdAndPhase(Long challengeId, ChallengePhaseEnum phase, String ownerEmail) {
        if (!challengeService.isChallengeOwner(ownerEmail, challengeId)) {
            return null;
        }

        if (phase == WINNER) {
            return findWinnerRegistrantsByChallengeId(challengeId);
        }

        BoolQueryBuilder challengeQuery = boolQuery().must(termQuery("challengeId", challengeId));
        BoolQueryBuilder activePhaseQuery = boolQuery();
        BoolQueryBuilder submissionPhaseQuery = boolQuery().should(termQuery("submissionPhase", phase));
        challengeQuery.must(activePhaseQuery);

        if (phase == REGISTRATION) {
            activePhaseQuery.should(filteredQuery(matchAllQuery(), missingFilter("activePhase")));
            submissionPhaseQuery.should(filteredQuery(matchAllQuery(), missingFilter("submissionPhase")));
        }
        for (int i = ALL_CHALLENGE_PHASES.length - 1; i >= 0; i--) {
            activePhaseQuery.should(termQuery("activePhase", ALL_CHALLENGE_PHASES[i]));
//      submissionPhaseQuery.should(QueryBuilders.termQuery("submissionPhase", ALL_CHALLENGE_PHASES[i]));
            if (phase == ALL_CHALLENGE_PHASES[i]) break;
        }

        Set<ChallengeRegistrantDto> registrants = StreamUtils.createStreamFromIterator(challengeRegistrantRepository.search(challengeQuery).iterator())
                .map(registrant -> {
                    ChallengeRegistrantDto dto = dozerMapper.map(registrant, ChallengeRegistrantDto.class);
                    BoolQueryBuilder submissionQuery = QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery("registrantId", registrant.getRegistrantId()))
                            .must(submissionPhaseQuery);
                    List<ChallengeSubmissionDto> submissions = StreamUtils.createStreamFromIterator(challengeSubmissionRepository.search(submissionQuery).iterator())
                            .map(submission -> dozerMapper.map(submission, ChallengeSubmissionDto.class)).collect(toList());
                    dto.setSubmissions(submissions);
                    return dto;
                }).collect(Collectors.toSet());

        return registrants;
    }

    public Set<ChallengeRegistrantDto> findWinnerRegistrantsByChallengeId(Long challengeId) {
        ChallengeEntity challenge = challengeRepository.findOne(challengeId);
        FilteredQueryBuilder winnerQuery = filteredQuery(matchAllQuery(),
                boolFilter()
                        .must(existsFilter("criteria.score"))
                        .must(termFilter("challengeId", challengeId))
                        .must(termFilter("activePhase", FINAL))
                        .mustNot(termFilter("disqualified", Boolean.TRUE)));

        Set<ChallengeRegistrantDto> registrants = StreamUtils.createStreamFromIterator(challengeRegistrantRepository.search(winnerQuery).iterator())
                .map(registrant -> {
                    ChallengeRegistrantDto dto = dozerMapper.map(registrant, ChallengeRegistrantDto.class);
                    Optional<ChallengeWinner> winner = challenge.getWinners().stream().filter(wnn -> dto.getRegistrantId().equals(wnn.getRegistrantId())).findFirst();
                    if (winner.isPresent()) {
                        dto.setReward(winner.get().getReward());
                    }

                    BoolQueryBuilder submissionQuery = boolQuery().must(termQuery("registrantId", registrant.getRegistrantId()))
                            .must(termQuery("submissionPhase", FINAL));
                    List<ChallengeSubmissionDto> submissions = StreamUtils.createStreamFromIterator(challengeSubmissionRepository.search(submissionQuery).iterator())
                            .map(submission -> dozerMapper.map(submission, ChallengeSubmissionDto.class)).collect(Collectors.toList());
                    dto.setSubmissions(submissions);
                    return dto;
                }).collect(Collectors.toSet());
        return registrants;
    }

    public Set<ChallengeWinner> saveWinner(ChallengeWinnerDto challengeWinnerDto, String loginUser) {
        ChallengeWinner challengeWinner = dozerMapper.map(challengeWinnerDto, ChallengeWinner.class);
        Long registrantId = challengeWinner.getRegistrantId();
        ChallengeRegistrantEntity registrant = challengeRegistrantRepository.findOne(registrantId);
        if (!challengeService.isChallengeOwner(loginUser, registrant.getChallengeId())) {
            return null;
        }

        ChallengeEntity challenge = challengeRepository.findOne(registrant.getChallengeId());
        Set<ChallengeWinner> winners = challenge.getWinners();
        winners = winners.stream().filter(wnn -> !wnn.getRegistrantId().equals(registrantId)).collect(Collectors.toSet());
        winners.remove(challengeWinner);
        if (!challengeWinnerDto.getRemovable()) winners.add(challengeWinner);

        challenge.setWinners(winners);
        challenge = challengeRepository.save(challenge);
        return challenge.getWinners();
    }

    public List<ChallengeRegistrantEntity> findRegistrantsByChallengeId(Long challengeId) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("challengeRegistrant");
        searchQueryBuilder.withQuery(filteredQuery(matchAllQuery(), termFilter("challengeId", challengeId)));
        return DataUtils.getAllEntities(challengeRegistrantRepository, searchQueryBuilder);
    }

    public List<ChallengeRegistrantEntity> findRegistrantsByOwner(String ownerEmail) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("challengeRegistrant");
        searchQueryBuilder.withQuery(filteredQuery(matchAllQuery(), termFilter("registrantEmail", ownerEmail)));
        return DataUtils.getAllEntities(challengeRegistrantRepository, searchQueryBuilder);
    }

    @Override
    public ChallengeRegistrantEntity findRegistrantById(Long registrantId) {
        return challengeRegistrantRepository.findOne(registrantId);
    }

    @Override
    public ChallengeRegistrantDto rejectRegistrant(String ownerEmail, RejectRegistrantDto rejectRegistrantDto) {
        Iterator<ChallengeRegistrantEntity> registrantIter = challengeRegistrantRepository.search(QueryBuilders.termQuery("registrantId", rejectRegistrantDto.getRegistrantId())).iterator();
        if (!registrantIter.hasNext()) return null;

        ChallengeRegistrantEntity registrant = registrantIter.next();
        ChallengeEntity challenge = challengeRepository.findOne(registrant.getChallengeId());
        if (!ownerEmail.equalsIgnoreCase(challenge.getAuthorEmail())) {
            return null;
        }

        registrant.setDisqualified(Boolean.TRUE);
        registrant.setDisqualifiedReason(rejectRegistrantDto.getReason());
        registrant = challengeRegistrantRepository.save(registrant);
        return dozerMapper.map(registrant, ChallengeRegistrantDto.class);
    }

    @Override
    public ChallengeRegistrantDto acceptRegistrant(Long registrantId, ChallengePhaseEnum activePhase) {
        ChallengeRegistrantEntity registrant = challengeRegistrantRepository.findOne(registrantId);

        if (registrant != null && activePhase != registrant.getActivePhase()) {
            registrant.setActivePhase(activePhase);
            registrant.setDisqualified(null);
            registrant.setDisqualifiedReason(null);
            registrant = challengeRegistrantRepository.save(registrant);

            challengeEmailService.sendEmailNotifyRegistrantWhenQualified(registrant);
        }

        return dozerMapper.map(registrant, ChallengeRegistrantDto.class);
    }

    @Override
    public List<ChallengeRegistrantDto> qualifyAllRegistrants(String ownerEmail, ChallengeQualificationDto challengeQualificationDto) {
        List<ChallengeRegistrantDto> qualifiedRegistrants = new ArrayList<>();
        ChallengePhaseEnum qualifyingPhase = challengeQualificationDto.getNextPhase();

        for (Long registrantId : challengeQualificationDto.getRegistrantIds()) {
            ChallengeRegistrantDto registrantDto = acceptRegistrant(registrantId, qualifyingPhase);
            if (registrantDto.getActivePhase() == qualifyingPhase) {
                qualifiedRegistrants.add(registrantDto);
            }
        }
        return qualifiedRegistrants;
    }

    @Override
    public Long getNumberOfRegistrants(Long challengeId) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withSearchType(SearchType.COUNT);
        searchQueryBuilder.withFilter(termFilter("challengeId", challengeId));
        return challengeRegistrantRepository.search(searchQueryBuilder.build()).getTotalElements();
    }

    @Override
    public ChallengeRegistrantDto saveRegistrant(String ownerEmail, ChallengeRegistrantDto challengeRegistrantDto) {
        ChallengeRegistrantDto resultChallengeRegistrantDto = challengeRegistrantDto;
        ChallengeEntity challenge = challengeRepository.findOne(challengeRegistrantDto.getChallengeId());
        if (ownerEmail.equalsIgnoreCase(challenge.getAuthorEmail())) {
            ChallengeRegistrantEntity registrant = challengeRegistrantRepository.findOne(challengeRegistrantDto.getRegistrantId());
            challengeRegistrantDto.setRegistrantEmail(registrant.getRegistrantEmail());
            dozerMapper.map(challengeRegistrantDto, registrant);
            registrant = challengeRegistrantRepository.save(registrant);
            resultChallengeRegistrantDto = dozerMapper.map(registrant, ChallengeRegistrantDto.class);
        }
        return resultChallengeRegistrantDto;
    }

    @Override
    public List<ChallengeRegistrantEntity> findChallengeRegistrantWithinPeriod(
            Long challengeId, Long currentDateTime, TimePeriodEnum period) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("challengeRegistrant");

        BoolQueryBuilder boolQueryBuilder = boolQuery();
        boolQueryBuilder.must(termQuery("challengeId", challengeId));

        Long pastTime = currentDateTime - period.getMiliseconds() > 0 ? currentDateTime - period.getMiliseconds() : 0;
        boolQueryBuilder.must(rangeQuery("registrantId").from(pastTime));
        searchQueryBuilder.withQuery(boolQueryBuilder);
        searchQueryBuilder.withSort(fieldSort("registrantId").order(SortOrder.DESC));
        return DataUtils.getAllEntities(challengeRegistrantRepository, searchQueryBuilder);
    }

    public ChallengeRegistrantEntity findRegistrantByChallengeIdAndEmail(Long challengeId, String email, String internalEmail) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("challengeRegistrant");
        BoolQueryBuilder query = boolQuery()
                .must(termQuery("registrantEmail", email))
                .must(termQuery("challengeId", challengeId));
        if (org.springframework.util.StringUtils.hasText(internalEmail)) {
            query.must(termQuery("registrantInternalEmail", internalEmail));
        }
        searchQueryBuilder.withQuery(query);

        List<ChallengeRegistrantEntity> registrantEntities = DataUtils.getAllEntities(challengeRegistrantRepository, searchQueryBuilder);
        if (!registrantEntities.isEmpty()) {
            return registrantEntities.get(0);
        }
        return null;
    }

    public ChallengeRegistrantEntity findRegistrantByChallengeIdAndInternalEmail(Long challengeId, String internalEmail) {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("challengeRegistrant");
        BoolQueryBuilder query = boolQuery()
                .must(matchQuery("registrantInternalEmail", internalEmail).operator(MatchQueryBuilder.Operator.AND))
                .must(termQuery("challengeId", challengeId));
        searchQueryBuilder.withQuery(query);
        List<ChallengeRegistrantEntity> registrantEntities = DataUtils.getAllEntities(challengeRegistrantRepository, searchQueryBuilder);
        if (!registrantEntities.isEmpty()) {
            return registrantEntities.get(0);
        }
        return null;
    }

    public DraftRegistrantEntity findDraftRegistrantEntityByChallengeIdAndEmail(Long challengeId, String email, String internalEmail) {


        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder().withTypes("draftRegistrant");
        BoolQueryBuilder query = boolQuery()
                .must(termQuery("registrantEmail", email))
                .must(termQuery("challengeId", challengeId));
        if (org.springframework.util.StringUtils.hasText(internalEmail)) {
            query.must(termQuery("registrantInternalEmail", internalEmail));
        }
        searchQueryBuilder.withQuery(query);

        List<DraftRegistrantEntity> registrantEntities = DataUtils.getAllEntities(draftRegistrantRepository, searchQueryBuilder);
        if (!registrantEntities.isEmpty()) {
            return registrantEntities.get(0);
        }
        return null;
    }

    @Override
    public List<ChallengeRegistrantFunnelItem> getChallengeRegistrantFunnel(Long challengeId, String ownerEmail) {
        List<ChallengeRegistrantFunnelItem> funnel = new ArrayList<>();
        ChallengeEntity challenge = challengeService.findChallengeById(challengeId);
        Map<ChallengePhaseEnum, ChallengeRegistrantPhaseItem> numberOfRegistrantsByPhase =
                countNumberOfRegistrantsByPhase(challengeId);
        Map<ChallengePhaseEnum, ChallengeSubmissionPhaseItem> numberOfSubmissionsByPhase =
                challengeSubmissionService.countNumberOfSubmissionsByPhase(challengeId, null);
        Map<ChallengePhaseEnum, ChallengeSubmissionPhaseItem> numberOfUnreadSubmissionsByPhase =
                challengeSubmissionService.countNumberOfSubmissionsByPhase(challengeId, Boolean.FALSE);

        for (Entry<ChallengePhaseEnum, ChallengeRegistrantPhaseItem> entry : numberOfRegistrantsByPhase.entrySet()) {
            ChallengePhaseEnum phase = entry.getKey();
            Long participant = entry.getValue().getRegistration();
            Long submission = 0L;
            Long unreadSubmission = 0L;
            if (numberOfSubmissionsByPhase.get(phase) != null) {
                submission = numberOfSubmissionsByPhase.get(phase).getSubmission();
            }
            if (numberOfUnreadSubmissionsByPhase.get(phase) != null) {
                unreadSubmission = numberOfUnreadSubmissionsByPhase.get(phase).getSubmission();
            }

            if (isValidPhase(challenge, phase)) {
                funnel.add(new ChallengeRegistrantFunnelItem(phase, participant, submission, unreadSubmission));
            }
        }

        Long numberOfFinalists = countNumberOfFinalists(challengeId);
        Long numberOfWinners = Long.valueOf(countNumberOfWinners(challengeId));
        funnel.add(new ChallengeRegistrantFunnelItem(WINNER, numberOfFinalists, numberOfWinners, 0L));

        Comparator<ChallengeRegistrantFunnelItem> sortByPhaseComparator = (item1, item2) ->
                item1.getPhase().getOrder() - item2.getPhase().getOrder();
        return funnel.stream().sorted(sortByPhaseComparator).collect(toList());
    }

    private boolean isValidPhase(ChallengeEntity challengeDto, ChallengePhaseEnum phase) {
        switch (phase) {
            case REGISTRATION:
                return StringUtils.isNotEmpty(challengeDto.getRegistrationDateTime());
            case IDEA:
                return StringUtils.isNotEmpty(challengeDto.getIdeaSubmissionDateTime());
            case UIUX:
                return StringUtils.isNotEmpty(challengeDto.getUxSubmissionDateTime());
            case PROTOTYPE:
                return StringUtils.isNotEmpty(challengeDto.getPrototypeSubmissionDateTime());
            case FINAL:
                return StringUtils.isNotEmpty(challengeDto.getSubmissionDateTime());
            default:
                return false;
        }
    }

    @Override
    public Long getTotalNumberOfRegistrants() {
        return challengeRegistrantRepository.count();
    }

    @Override
    public List<ChallengeDashBoardInfo> getChallengeDashBoardInfo(JobSeekerDashBoardCriteria criteria) {
        List<ChallengeDashBoardInfo> challengeDashBoardInfoList = new ArrayList<>();
        List<ChallengeRegistrantEntity> registrantEntities = findRegistrantsByOwner(criteria.getJobSeekerEmail());

        for (ChallengeRegistrantEntity registrantEntity : registrantEntities) {
            Long challengeId = registrantEntity.getChallengeId();
            Long registrantId = registrantEntity.getRegistrantId();
            ChallengeEntity challengeEntity = challengeService.findChallengeById(challengeId);

            boolean isChallengeSelected = challengeEntity != null &&
                    (challengeEntity.getExpired() == null || !challengeEntity.getExpired()) &&
                    isPhaseMatching(challengeEntity, registrantEntity, criteria.getJobSeekerPhase());

            if (isChallengeSelected) {
                ChallengeDashBoardInfo.Builder challengeDashBoardInfoBuilder = new ChallengeDashBoardInfo.Builder();
                challengeDashBoardInfoBuilder.withChallengeId(challengeId);
                challengeDashBoardInfoBuilder.withChallengeName(challengeEntity.getChallengeName());
                challengeDashBoardInfoBuilder.withSubmissionDate(challengeEntity.getSubmissionDateTime());
                ChallengePhaseEnum currentPhase = registrantEntity.getActivePhase() != null ?
                        registrantEntity.getActivePhase() : REGISTRATION;
                challengeDashBoardInfoBuilder.withCurrentPhase(currentPhase);
                challengeDashBoardInfoBuilder.withCurrentPhaseSubmissionDate(getCurrentPhaseSubmissionDate(challengeEntity, currentPhase));
                challengeDashBoardInfoBuilder.withDisqualified(registrantEntity.getDisqualified());

                Integer numberOfSubmissions = challengeSubmissionService.findChallengeSubmissionByRegistrant(registrantId).size();
                challengeDashBoardInfoBuilder.withNumberOfSubmissions(numberOfSubmissions);

                ChallengeWinner winner = getChallengeWinner(challengeEntity, registrantId);
                if (winner != null) {
                    challengeDashBoardInfoBuilder.withRank(winner.getReward().getOrder());
                    challengeDashBoardInfoBuilder.withPrize(getWinnerPrize(challengeEntity, winner));
                }

                challengeDashBoardInfoBuilder.withScore(getRegistrantSubmissionScore(registrantEntity));
                challengeDashBoardInfoBuilder.withCriteria(registrantEntity.getCriteria());
                challengeDashBoardInfoBuilder.withSubmissions(challengeSubmissionService.findChallengeSubmissionByRegistrant(registrantId));
                challengeDashBoardInfoBuilder.withChallengeType(challengeEntity.getChallengeType());
                challengeDashBoardInfoBuilder.withJobSeekerPhase(getJobSeekerPhase(challengeEntity, registrantEntity));
                challengeDashBoardInfoList.add(challengeDashBoardInfoBuilder.build());
            }
        }
        return challengeDashBoardInfoList.stream()
                .sorted((source, destination) -> source.getJobSeekerPhase().getOrder() - destination.getJobSeekerPhase().getOrder())
                .collect(toList());
    }

    private boolean isPhaseMatching(ChallengeEntity challengeEntity, ChallengeRegistrantEntity registrantEntity,
                                    JobSeekerPhaseEnum phase) {
        ChallengePhaseEnum jobSeekerCurrentPhase = registrantEntity.getActivePhase() == null ? REGISTRATION : registrantEntity.getActivePhase();
        List<ChallengePhaseEnum> activePhases = Arrays.asList(REGISTRATION, IDEA, UIUX, PROTOTYPE, FINAL);
        boolean isChallengeClosed = daysBetween(challengeEntity.getSubmissionDateTime(), currentDate()) > 0;
        switch (phase) {
            case ALL:
                return true;
            case ACTIVE:
                return !isChallengeClosed && activePhases.contains(jobSeekerCurrentPhase);
            case FINISHED:
                return isChallengeClosed;
            case DISQUALIFIED:
                return registrantEntity.getDisqualified() == null ? false : registrantEntity.getDisqualified();
            default:
                return true;
        }
    }

    private JobSeekerPhaseEnum getJobSeekerPhase(ChallengeEntity challengeEntity, ChallengeRegistrantEntity registrantEntity) {
        ChallengePhaseEnum jobSeekerCurrentPhase = registrantEntity.getActivePhase() == null ?
                REGISTRATION : registrantEntity.getActivePhase();
        List<ChallengePhaseEnum> activePhases = Arrays.asList(REGISTRATION, IDEA, UIUX, PROTOTYPE, FINAL);
        boolean isChallengeClosed = daysBetween(challengeEntity.getSubmissionDateTime(), currentDate()) > 0;

        if (!isChallengeClosed && activePhases.contains(jobSeekerCurrentPhase)) {
            return JobSeekerPhaseEnum.ACTIVE;
        } else if (registrantEntity.getDisqualified() != null && registrantEntity.getDisqualified()) {
            return JobSeekerPhaseEnum.DISQUALIFIED;
        } else {
            return JobSeekerPhaseEnum.FINISHED;
        }
    }

    public Set<ChallengeRegistrantDto> getChallengeWinners(Long challengeId) {
        ChallengeEntity challenge = challengeRepository.findOne(challengeId);
        if (Boolean.TRUE.equals(challenge.getVisibleWinners())) {
            return findWinnerRegistrantsByChallengeId(challengeId);
        }
        return null;
    }

    private Double getRegistrantSubmissionScore(ChallengeRegistrantEntity registrantEntity) {
        Set<ChallengeRegistrantCriteria> criteria = registrantEntity.getCriteria();

        Double totalScore = 0D;
        if (criteria != null) {
            for (ChallengeRegistrantCriteria criterion : criteria) {
                if (criterion.getScore() != null) {
                    totalScore += criterion.getWeight() * criterion.getScore();
                }
            }
        }
        return totalScore;
    }

    private Integer getWinnerPrize(ChallengeEntity challengeEntity, ChallengeWinner winner) {
        switch (winner.getReward()) {
            case FIRST_PLACE:
                return challengeEntity.getFirstPlaceReward();
            case SECOND_PLACE:
                return challengeEntity.getSecondPlaceReward();
            case THIRD_PLACE:
                return challengeEntity.getThirdPlaceReward();
            default:
                return null;
        }
    }

    private ChallengeWinner getChallengeWinner(ChallengeEntity challengeEntity, Long registrantId) {
        Set<ChallengeWinner> winners = challengeEntity.getWinners();
        for (ChallengeWinner winner : winners) {
            if (winner.getRegistrantId().equals(registrantId)) {
                return winner;
            }
        }
        return null;
    }

    private String getCurrentPhaseSubmissionDate(ChallengeEntity challengeEntity, ChallengePhaseEnum currentPhase) {
        switch (currentPhase) {
            case REGISTRATION:
                return challengeEntity.getRegistrationDateTime();
            case IDEA:
                return challengeEntity.getIdeaSubmissionDateTime();
            case UIUX:
                return challengeEntity.getUxSubmissionDateTime();
            case PROTOTYPE:
                return challengeEntity.getPrototypeSubmissionDateTime();
            case FINAL:
                return challengeEntity.getSubmissionDateTime();
            default:
                return "";
        }
    }

    public DraftRegistrantDto saveDraftRegistrant(DraftRegistrantEntity draftRegistrantEntity) {
        ChallengeRegistrantEntity existingRegistrant = this.findRegistrantByChallengeIdAndInternalEmail(draftRegistrantEntity.getChallengeId(), draftRegistrantEntity.getRegistrantInternalEmail());
        if (existingRegistrant != null) {
            return null;
        }

        DraftRegistrantEntity draft = findDraftRegistrantEntityByChallengeIdAndEmail(draftRegistrantEntity.getChallengeId(),
                draftRegistrantEntity.getRegistrantEmail(), draftRegistrantEntity.getRegistrantInternalEmail());
        draftRegistrantEntity.setRegistrantId(draft == null ? DateTimeUtils.currentDateTime() : draft.getRegistrantId());
        draft = dozerMapper.map(draftRegistrantEntity, DraftRegistrantEntity.class);
        Integer passcode = DataUtils.getRandomNumberInRange(1000, 9999);
        draft.setPasscode(passcode);
        draft = draftRegistrantRepository.save(draft);
        challengeEmailService.sendEmailToVerifyRegistrantOfInternalChallenge(draft);
        return dozerMapper.map(draft, DraftRegistrantDto.class);
    }

    @Override
    public List<JobSeekerChallengeStats> countNumberOfChallengesByJobSeekerPhase(JobSeekerDashBoardCriteria criteria) {
        List<JobSeekerChallengeStats> result = new ArrayList<>();
        Map<JobSeekerPhaseEnum, Integer> challengeStats = new HashMap<>();
        List<ChallengeRegistrantEntity> registrantEntities = findRegistrantsByOwner(criteria.getJobSeekerEmail());

        for (ChallengeRegistrantEntity registrantEntity : registrantEntities) {
            Long challengeId = registrantEntity.getChallengeId();
            ChallengeEntity challengeEntity = challengeService.findChallengeById(challengeId);

            boolean isChallengeSelected = challengeEntity != null &&
                    (challengeEntity.getExpired() == null || !challengeEntity.getExpired());

            if (isChallengeSelected) {
                for (JobSeekerPhaseEnum phase : JobSeekerPhaseEnum.values()) {
                    putInTheBucket(challengeStats, registrantEntity, challengeEntity, phase);
                }
            }
        }

        result.addAll(challengeStats.entrySet().stream().map(
                entry -> new JobSeekerChallengeStats(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
        return result.stream()
                .sorted((challengeA, challengeB) -> challengeA.getPhase().getOrder() - challengeB.getPhase().getOrder())
                .collect(toList());
    }

    private void putInTheBucket(Map<JobSeekerPhaseEnum, Integer> challengeStats, ChallengeRegistrantEntity registrantEntity,
                                ChallengeEntity challengeEntity, JobSeekerPhaseEnum phase) {
        if (isPhaseMatching(challengeEntity, registrantEntity, phase)) {
            Integer count = challengeStats.get(phase);
            if (count != null) {
                count++;
            } else {
                count = 1;
            }
            challengeStats.put(phase, count);
        }
    }
}
