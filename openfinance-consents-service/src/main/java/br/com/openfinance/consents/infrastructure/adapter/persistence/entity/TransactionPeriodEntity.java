package br.com.openfinance.consents.infrastructure.adapter.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPeriodEntity {
    private LocalDate from;
    private LocalDate to;
}
