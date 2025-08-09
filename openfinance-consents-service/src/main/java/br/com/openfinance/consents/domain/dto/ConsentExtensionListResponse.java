package br.com.openfinance.consents.domain.dto;

import br.com.openfinance.consents.generated.model.ResponseConsentReadExtensionsDataInner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentExtensionListResponse {
    private List<ResponseConsentReadExtensionsDataInner> data;
    private Long totalRecords;
    private Integer totalPages;
}
