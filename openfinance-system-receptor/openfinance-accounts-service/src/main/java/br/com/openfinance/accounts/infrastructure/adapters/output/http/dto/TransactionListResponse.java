package br.com.openfinance.accounts.infrastructure.adapters.output.http.dto;

import lombok.Data;
import java.util.List;

@Data
public class TransactionListResponse {
    private List<TransactionResponse> data;
    private Links links;
    private Meta meta;

    @Data
    public static class Links {
        private String self;
        private String first;
        private String prev;
        private String next;
        private String last;
    }

    @Data
    public static class Meta {
        private int totalRecords;
        private int totalPages;
        private String requestDateTime;
    }
}
