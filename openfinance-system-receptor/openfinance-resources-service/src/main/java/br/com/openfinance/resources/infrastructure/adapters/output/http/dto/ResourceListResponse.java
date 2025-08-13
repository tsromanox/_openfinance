package br.com.openfinance.resources.infrastructure.adapters.output.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceListResponse {
    @JsonProperty("data")
    private List<ResourceResponse> data;
    
    @JsonProperty("links")
    private LinksResponse links;
    
    @JsonProperty("meta")
    private MetaResponse meta;
}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LinksResponse {
    @JsonProperty("self")
    private String self;
    
    @JsonProperty("first")
    private String first;
    
    @JsonProperty("prev")
    private String prev;
    
    @JsonProperty("next")
    private String next;
    
    @JsonProperty("last")
    private String last;
}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MetaResponse {
    @JsonProperty("totalRecords")
    private Integer totalRecords;
    
    @JsonProperty("totalPages")
    private Integer totalPages;
    
    @JsonProperty("requestDateTime")
    private String requestDateTime;
}