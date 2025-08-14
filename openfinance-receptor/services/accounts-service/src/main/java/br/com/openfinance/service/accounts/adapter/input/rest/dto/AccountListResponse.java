package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Response DTOs
public record AccountListResponse(
        @JsonProperty("data") List<AccountDto> data,
        @JsonProperty("links") Links links,
        @JsonProperty("meta") Meta meta
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<AccountDto> data;
        private Links links;
        private Meta meta;

        public Builder data(List<AccountDto> data) {
            this.data = data;
            return this;
        }

        public Builder links(Links links) {
            this.links = links;
            return this;
        }

        public Builder meta(Meta meta) {
            this.meta = meta;
            return this;
        }

        public AccountListResponse build() {
            return new AccountListResponse(data, links, meta);
        }
    }
}

