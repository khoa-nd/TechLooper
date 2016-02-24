package com.techlooper.service.impl;

import com.techlooper.entity.JobEntity;
import com.techlooper.model.SalaryReviewDto;
import com.techlooper.model.SalaryReviewResultDto;
import com.techlooper.service.SalaryAnalyzeReportGenerator;
import com.techlooper.util.SalaryAverageFunction;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by NguyenDangKhoa on 2/23/16.
 */
public class SalaryAnalyzeReportGeneratorImpl implements SalaryAnalyzeReportGenerator {

    @Override
    public SalaryReviewResultDto generateReport(SalaryReviewDto salaryReviewDto, Set<JobEntity> jobs) {
        return null;
    }

}
