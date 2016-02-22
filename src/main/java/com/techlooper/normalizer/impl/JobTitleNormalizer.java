package com.techlooper.normalizer.impl;

import com.techlooper.normalizer.Normalizer;
import com.techlooper.service.SuggestionService;

import java.util.List;

/**
 * Created by NguyenDangKhoa on 2/22/16.
 */
public class JobTitleNormalizer implements Normalizer<String, String> {

    private SuggestionService suggestionService;

    public JobTitleNormalizer(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Override
    public String normalize(String inputJobTitle) {
        List<String> jobTitleCandidates = suggestionService.searchJobTitles(inputJobTitle);
        return jobTitleCandidates.isEmpty() ? null : jobTitleCandidates.get(0);
    }
}
