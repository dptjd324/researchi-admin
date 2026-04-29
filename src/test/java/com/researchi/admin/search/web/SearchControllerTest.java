package com.researchi.admin.search.web;

import com.researchi.admin.search.domain.PeriodSearchForm;
import com.researchi.admin.search.domain.PeriodSearchResult;
import com.researchi.admin.search.service.PeriodSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private PeriodSearchService periodSearchService;

    @InjectMocks
    private SearchController searchController;

    @Test
    void indexPopulatesDefaultModelWithoutRunningSearch() {
        when(periodSearchService.getScopeOptions()).thenReturn(List.of("APPLICATION", "MAIL"));
        when(periodSearchService.getStatusOptions("APPLICATION")).thenReturn(List.of("RECEIVED"));
        when(periodSearchService.getJobOptions(null)).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();
        PeriodSearchForm form = new PeriodSearchForm();

        String viewName = searchController.index(form, false, model);

        assertThat(viewName).isEqualTo("search/index");
        assertThat(model.get("submitted")).isEqualTo(false);
        assertThat(model.get("scopeOptions")).isEqualTo(List.of("APPLICATION", "MAIL"));
    }

    @Test
    void indexPopulatesSearchResultWhenSubmitted() {
        when(periodSearchService.getScopeOptions()).thenReturn(List.of("APPLICATION"));
        when(periodSearchService.getStatusOptions("APPLICATION")).thenReturn(List.of("RECEIVED"));
        when(periodSearchService.getJobOptions(null)).thenReturn(List.of());
        PeriodSearchResult result = new PeriodSearchResult("APPLICATION", "Applied At", 2, 9L, List.of(), List.of(), List.of(), List.of());
        when(periodSearchService.search(org.mockito.ArgumentMatchers.any(PeriodSearchForm.class))).thenReturn(result);

        ExtendedModelMap model = new ExtendedModelMap();
        PeriodSearchForm form = new PeriodSearchForm();

        String viewName = searchController.index(form, true, model);

        assertThat(viewName).isEqualTo("search/index");
        assertThat(model.get("submitted")).isEqualTo(true);
        assertThat(model.get("result")).isEqualTo(result);
        verify(periodSearchService).search(form);
    }
}
