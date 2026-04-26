package com.researchi.admin.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class PaginationSupport {

    public record PageLink(int pageNumber, String url, boolean current) {
    }

    public static final int DEFAULT_PAGE_SIZE = 12;

    private PaginationSupport() {
    }

    public static <T> List<T> apply(
            Model model,
            HttpServletRequest request,
            List<T> items,
            Integer requestedPage,
            int pageSize
    ) {
        List<T> safeItems = items == null ? List.of() : items;
        int totalItems = safeItems.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = requestedPage == null ? 1 : Math.max(1, Math.min(requestedPage, totalPages));
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<T> pagedItems = safeItems.subList(fromIndex, toIndex);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("paginationEnabled", totalItems > pageSize);
        model.addAttribute("pageLinks", buildPageLinks(request, currentPage, totalPages));
        model.addAttribute("previousPageUrl", currentPage > 1 ? buildPageUrl(request, currentPage - 1) : null);
        model.addAttribute("nextPageUrl", currentPage < totalPages ? buildPageUrl(request, currentPage + 1) : null);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalItemCount", totalItems);

        return pagedItems;
    }

    private static String buildPageUrl(HttpServletRequest request, int page) {
        Map<String, String[]> parameterMap = new LinkedHashMap<>(request.getParameterMap());
        parameterMap.put("page", new String[]{String.valueOf(page)});

        StringBuilder builder = new StringBuilder(request.getRequestURI());
        boolean first = true;
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                builder.append(first ? '?' : '&');
                first = false;
                builder.append(encode(entry.getKey())).append('=').append(encode(value));
            }
        }
        return builder.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static List<PageLink> buildPageLinks(HttpServletRequest request, int currentPage, int totalPages) {
        int startPage = Math.max(1, currentPage - 1);
        int endPage = Math.min(totalPages, startPage + 2);
        startPage = Math.max(1, endPage - 3);
        if (totalPages <= 4) {
            startPage = 1;
            endPage = totalPages;
        }

        return IntStream.rangeClosed(startPage, endPage)
                .mapToObj(pageNumber -> new PageLink(pageNumber, buildPageUrl(request, pageNumber), pageNumber == currentPage))
                .toList();
    }
}
