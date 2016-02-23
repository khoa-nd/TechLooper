package com.techlooper.service.impl;

import com.techlooper.model.*;
import com.techlooper.normalizer.Normalizer;
import com.techlooper.repository.elasticsearch.SalaryReviewRepository;
import com.techlooper.repository.elasticsearch.VietnamworksJobRepository;
import com.techlooper.service.SalaryAnalyzeReportGenerator;
import com.techlooper.service.SalaryAnalyzeService;
import org.dozer.Mapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Created by NguyenDangKhoa on 2/23/16.
 */
@Service
public class SalaryAnalyzeServiceImpl implements SalaryAnalyzeService {

    @Resource
    private Normalizer<String,String> normalizer;

    @Resource
    private Mapper dozerMapper;

    @Resource
    private VietnamworksJobRepository vietnamworksJobRepository;

    @Resource
    private SalaryReviewRepository salaryReviewRepository;


    private SalaryAnalyzeReportGenerator salaryAnalyzeReportGenerator;

    @Override
    public SalaryReviewResultDto reviewSalary(SalaryReviewDto salaryReviewDto) {
        SalaryReviewCondition salaryReviewCondition = dozerMapper.map(salaryReviewDto, SalaryReviewCondition.class);
        salaryReviewCondition.setJobTitle(normalizer.normalize(salaryReviewCondition.getJobTitle()));

        SalaryReviewJobRepository jobRepository = new SalaryReviewJobRepository();
        JobSearchStrategy searchBySalaryStrategy = new JobSearchBySalaryStrategy(salaryReviewCondition, vietnamworksJobRepository);
        jobRepository.addStrategy(searchBySalaryStrategy);

        JobSearchStrategy searchBySimilarSalaryReviewStrategy = new SimilarSalaryReviewSearchStrategy(
                salaryReviewCondition, salaryReviewRepository);
        jobRepository.addStrategy(searchBySimilarSalaryReviewStrategy);

        if (jobRepository.isNotEmpty() && jobRepository.isNotEnough()) {
            JobSearchStrategy searchBySkillStrategy = new JobSearchBySkillStrategy(vietnamworksJobRepository, salaryReviewCondition);
            jobRepository.addStrategy(searchBySkillStrategy);
        }

        if (jobRepository.isNotEmpty() && jobRepository.isNotEnough()) {
            JobSearchStrategy searchByTitleStrategy = new JobSearchByTitleStrategy(vietnamworksJobRepository, salaryReviewCondition);
            jobRepository.addStrategy(searchByTitleStrategy);
        }


        return null;
    }

}
