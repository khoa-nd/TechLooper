package com.techlooper.service;

import com.techlooper.model.SalaryReviewDto;
import com.techlooper.model.SalaryReviewResultDto;

/**
 * Created by NguyenDangKhoa on 2/23/16.
 */
public interface SalaryAnalyzeService {

    SalaryReviewResultDto reviewSalary(SalaryReviewDto salaryReviewDto);
    
}
