package com.techlooper.service;

import com.techlooper.entity.JobEntity;
import com.techlooper.model.SalaryReviewDto;
import com.techlooper.model.SalaryReviewResultDto;

import java.util.Set;

/**
 * Created by NguyenDangKhoa on 2/23/16.
 */
public interface SalaryAnalyzeReportGenerator {

    SalaryReviewResultDto generateReport(SalaryReviewDto salaryReviewDto, Set<JobEntity> jobs);
    
}
