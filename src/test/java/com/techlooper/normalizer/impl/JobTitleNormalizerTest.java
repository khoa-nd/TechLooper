package com.techlooper.normalizer.impl;

import com.techlooper.normalizer.Normalizer;
import com.techlooper.service.SuggestionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobTitleNormalizerTest {

    private Normalizer<String, String> normalizer;

    private SuggestionService suggestionService;

    @Before
    public void setUp() throws Exception {
        suggestionService = mock(SuggestionService.class);
        normalizer = new JobTitleNormalizer(suggestionService);
    }

    private static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Java Developer", Arrays.asList("Java Developer", "Senior Java PLSQL Developer", "Senior Java Scala Developer"), "Java Developer"},
                {"04 Java Developer", Arrays.asList("Java Developer", "Senior Java PLSQL Developer", "Senior Java Scala Developer"), "Java Developer"},
                {"Java Developer with High Salary", Collections.emptyList(), "Java Developer with High Salary"},
                {"Java Developer with Salary 1500-2000", Arrays.asList("Java Developer", "Senior Java PLSQL Developer", "Senior Java Scala Developer"), "Java Developer"}
        });
    }

    @Test
    public void testNormalize() throws Exception {
        for (Object[] row : data()) {
            String inputJobTitle = String.valueOf(row[0]);
            List<String> suggestJobTitles = (List<String>) row[1];
            String expectJobTitle = String.valueOf(row[2]);
            when(suggestionService.searchJobTitles(inputJobTitle)).thenReturn(suggestJobTitles);

            String outputJobTitle = normalizer.normalize(inputJobTitle);

            Assert.assertEquals(expectJobTitle, outputJobTitle);
        }
    }
}