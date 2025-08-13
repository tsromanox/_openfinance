package br.com.openfinance.application.service;

import br.com.openfinance.application.exception.AccountNotFoundException;
import br.com.openfinance.application.exception.ConsentNotAuthorizedException;
import br.com.openfinance.application.exception.ConsentNotFoundException;
import br.com.openfinance.application.port.input.AccountUseCase;
import br.com.openfinance.application.port.output.ConsentRepository;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import br.com.openfinance.domain.account.Account;
import br.com.openfinance.domain.account.Balance;
import br.com.openfinance.domain.consent.ConsentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService implements AccountUseCase {

    private final ConsentRepository consentRepository;
    private final OpenFinanceClient openFinanceClient;

    public AccountService(
            ConsentRepository consentRepository,
            OpenFinanceClient openFinanceClient) {
        this.consentRepository = consentRepository;
        this.openFinanceClient = openFinanceClient;
    }

    @Override
    public List<Account> syncAccountsForConsent(UUID consentId) {
        var consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));

        if (consent.getStatus() != ConsentStatus.AUTHORISED) {
            throw new ConsentNotAuthorizedException(consentId);
        }

        try {
            // Buscar contas na instituição financeira
            var accountsResponse = openFinanceClient.getAccounts(
                    consent.getOrganizationId(),
                    consent.getConsentId()
            );

            return accountsResponse.data().stream()
                    .map(accountData -> Account.builder()
                            .accountId(accountData.accountId())
                            .brandName(accountData.brandName())
                            .companyCnpj(accountData.companyCnpj())
                            .type(accountData.type())
                            .subtype(accountData.subtype())
                            .number(accountData.number())
                            .checkDigit(accountData.checkDigit())
                            .agencyNumber(accountData.agencyNumber())
                            .agencyCheckDigit(accountData.agencyCheckDigit())
                            .balance(Balance.builder()
                                    .availableAmount(accountData.availableAmount())
                                    .availableAmountCurrency(accountData.availableAmountCurrency())
                                    .blockedAmount(accountData.blockedAmount())
                                    .blockedAmountCurrency(accountData.blockedAmountCurrency())
                                    .automaticallyInvestedAmount(accountData.automaticallyInvestedAmount())
                                    .automaticallyInvestedAmountCurrency(accountData.automaticallyInvestedAmountCurrency())
                                    .updatedAt(LocalDateTime.now())
                                    .build())
                            .consentId(consent.getId())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error syncing accounts for consent " + consentId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Account getAccountDetails(String accountId) {
        // Implementação simplificada - normalmente viria de um repositório
        throw new AccountNotFoundException(accountId);
    }

    @Override
    public void updateAccountBalance(String accountId) {
        try {
            // Buscar informações da conta (normalmente de um repositório)
            // Para este exemplo, vamos assumir que temos as informações necessárias
            
            // Buscar saldo atualizado na instituição financeira
            var balanceResponse = openFinanceClient.getBalance(
                    "organizationId", // Normalmente vem do contexto da conta
                    accountId,
                    "token" // Normalmente vem do contexto da conta/consent
            );

            // Atualizar saldo no banco de dados (implementação dependeria do repositório)
            var updatedBalance = Balance.builder()
                    .availableAmount(balanceResponse.data().availableAmount())
                    .availableAmountCurrency(balanceResponse.data().availableAmountCurrency())
                    .blockedAmount(balanceResponse.data().blockedAmount())
                    .blockedAmountCurrency(balanceResponse.data().blockedAmountCurrency())
                    .automaticallyInvestedAmount(balanceResponse.data().automaticallyInvestedAmount())
                    .automaticallyInvestedAmountCurrency(balanceResponse.data().automaticallyInvestedAmountCurrency())
                    .updatedAt(LocalDateTime.now())
                    .build();

            System.out.println("Balance updated for account: " + accountId);

        } catch (Exception e) {
            System.err.println("Error updating balance for account " + accountId + ": " + e.getMessage());
        }
    }
}