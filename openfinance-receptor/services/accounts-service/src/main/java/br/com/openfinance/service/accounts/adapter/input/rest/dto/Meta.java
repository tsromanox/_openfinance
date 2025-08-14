package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record Meta(
        @JsonProperty("totalRecords") Integer totalRecords,
        @JsonProperty("totalPages") Integer totalPages,
        @JsonProperty("requestDateTime") LocalDateTime requestDateTime
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer totalRecords;
        private Integer totalPages;
        private LocalDateTime requestDateTime;

        public Builder totalRecords(Integer totalRecords) {
            this.totalRecords = totalRecords;
            return this;
        }

        public Builder totalPages(Integer totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder requestDateTime(LocalDateTime requestDateTime) {
            this.requestDateTime = requestDateTime;
            return this;
        }

        public Meta build() {
            return new Meta(totalRecords, totalPages, requestDateTime);
        }
    }
}
