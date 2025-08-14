package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Links(
        @JsonProperty("self") String self,
        @JsonProperty("first") String first,
        @JsonProperty("prev") String prev,
        @JsonProperty("next") String next,
        @JsonProperty("last") String last
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String self;
        private String first;
        private String prev;
        private String next;
        private String last;

        public Builder self(String self) {
            this.self = self;
            return this;
        }

        public Builder first(String first) {
            this.first = first;
            return this;
        }

        public Builder last(String last) {
            this.last = last;
            return this;
        }

        public Links build() {
            return new Links(self, first, prev, next, last);
        }
    }
}
