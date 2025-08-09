package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity;

import br.com.openfinance.core.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "account_identifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentificationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "identification_id")
    private UUID identificationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "ispb", length = 8)
    private String ispb;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "check_digit", length = 2)
    private String checkDigit;

    @Column(name = "account_type", length = 50)
    private String accountType;
}
