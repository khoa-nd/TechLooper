package com.techlooper.model;

import com.techlooper.entity.JobEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by NguyenDangKhoa on 12/7/15.
 */
public class SalaryReviewJobRepository {

    private static final int LOWER_LIMIT_FOR_EACH_SEARCH_STRATEGY = 10;

    private static final int LIMIT_FOR_JOB_REPOSITORY = 1000;

    private Set<JobEntity> jobs = new HashSet<>();

    public boolean isEnough() {
        return jobs.size() > LOWER_LIMIT_FOR_EACH_SEARCH_STRATEGY;
    }

    public boolean isNotEnough() {
        return !isEnough();
    }

    public boolean isEmpty() {
        return jobs.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public Set<JobEntity> getJobs() {
        return jobs;
    }

    public void addStrategy(JobSearchStrategy strategy) {
        if (jobs.size() < LIMIT_FOR_JOB_REPOSITORY) {
            jobs.addAll(strategy.searchJob());
        }
    }

}
