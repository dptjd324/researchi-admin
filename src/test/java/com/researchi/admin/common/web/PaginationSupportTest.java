package com.researchi.admin.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationSupportTest {

    @Test
    void showsFirstTenPageLinksInFirstBlock() {
        Model model = new ExtendedModelMap();

        PaginationSupport.applyMetadata(model, request(), 250, 1, 10);

        assertThat(pageNumbers(model)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void showsSecondTenPageLinksWhenCurrentPageExceedsTen() {
        Model model = new ExtendedModelMap();

        PaginationSupport.applyMetadata(model, request(), 250, 11, 10);

        assertThat(pageNumbers(model)).containsExactly(11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
    }

    @Test
    void trimsLastPageBlockToRemainingPages() {
        Model model = new ExtendedModelMap();

        PaginationSupport.applyMetadata(model, request(), 250, 21, 10);

        assertThat(pageNumbers(model)).containsExactly(21, 22, 23, 24, 25);
    }

    @Test
    void providesFirstAndLastPageUrls() {
        Model model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");
        request.addParameter("keyword", "coffee");
        request.addParameter("cursor", "123");

        PaginationSupport.applyMetadata(model, request, 250, 11, 10);

        assertThat(model.asMap().get("firstPageUrl")).isEqualTo("/jobs?keyword=coffee&page=1");
        assertThat(model.asMap().get("lastPageUrl")).isEqualTo("/jobs?keyword=coffee&page=25");
    }

    @Test
    void disablesFirstAndLastPageUrlsAtEdges() {
        Model firstPageModel = new ExtendedModelMap();
        Model lastPageModel = new ExtendedModelMap();

        PaginationSupport.applyMetadata(firstPageModel, request(), 250, 1, 10);
        PaginationSupport.applyMetadata(lastPageModel, request(), 250, 25, 10);

        assertThat(firstPageModel.asMap().get("firstPageUrl")).isNull();
        assertThat(lastPageModel.asMap().get("lastPageUrl")).isNull();
    }

    private HttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/jobs");
    }

    @SuppressWarnings("unchecked")
    private List<Integer> pageNumbers(Model model) {
        return ((List<PaginationSupport.PageLink>) model.asMap().get("pageLinks"))
                .stream()
                .map(PaginationSupport.PageLink::pageNumber)
                .toList();
    }
}
