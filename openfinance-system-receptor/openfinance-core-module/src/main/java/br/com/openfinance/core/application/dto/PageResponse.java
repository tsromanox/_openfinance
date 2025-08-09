package br.com.openfinance.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> data;
    private Links links;
    private Meta meta;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Links {
        private String self;
        private String first;
        private String prev;
        private String next;
        private String last;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private int totalRecords;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private String requestDateTime;
    }
}
