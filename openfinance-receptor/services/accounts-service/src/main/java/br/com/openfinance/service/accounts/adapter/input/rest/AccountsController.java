package br.com.openfinance.service.accounts.adapter.input.rest;

import br.com.openfinance.accounts.application.port.input.AccountUseCase;
import br.com.openfinance.accounts.adapter.input.rest.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para operações de Accounts
 */
@RestController
@RequestMapping("/open-banking/accounts/v2")
@Tag(name = "Accounts", description = "Operações relacionadas a contas bancárias")
public class AccountsController {

    private final AccountUseCase accountUseCase;
    private final AccountMapper mapper;

    public AccountsController(AccountUseCase accountUseCase, AccountMapper mapper) {
        this.accountUseCase = accountUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/accounts")
    @Operation(summary = "Lista contas do consentimento")
    public ResponseEntity<AccountListResponse> getAccounts(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") UUID interactionId,
            @RequestParam UUID consentId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(1000) Integer pageSize) {

        var accounts = accountUseCase.getAccountsByConsent(consentId);

        var response = AccountListResponse.builder()
                .data(accounts.stream()
                        .map(mapper::toDto)
                        .collect(Collectors.toList()))
                .links(createLinks(page, pageSize, accounts.size()))
                .meta(createMeta(accounts.size(), page, pageSize))
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "Obtém detalhes de uma conta específica")
    public ResponseEntity<AccountDetailResponse> getAccount(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") UUID interactionId,
            @PathVariable String accountId) {

        var account = accountUseCase.getAccountById(accountId);

        var response = AccountDetailResponse.builder()
                .data(mapper.toDetailDto(account))
                .links(createSelfLink(accountId))
                .meta(createSimpleMeta())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/balances")
    @Operation(summary = "Obtém saldo da conta")
    public ResponseEntity<BalanceResponse> getAccountBalance(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") UUID interactionId,
            @PathVariable String accountId) {

        var balance = accountUseCase.getAccountBalance(accountId);

        var response = BalanceResponse.builder()
                .data(mapper.toBalanceDto(balance))
                .links(createSelfLink(accountId + "/balances"))
                .meta(createSimpleMeta())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    @Operation(summary = "Obtém transações da conta")
    public ResponseEntity<TransactionListResponse> getAccountTransactions(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") UUID interactionId,
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromBookingDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toBookingDate,
            @RequestParam(required = false) String creditDebitIndicator,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(1000) Integer pageSize) {

        var transactions = accountUseCase.getAccountTransactions(
                accountId, fromBookingDate, toBookingDate, page, pageSize
        );

        var response = TransactionListResponse.builder()
                .data(transactions.stream()
                        .map(mapper::toTransactionDto)
                        .collect(Collectors.toList()))
                .links(createLinks(page, pageSize, transactions.size()))
                .meta(createMeta(transactions.size(), page, pageSize))
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/{accountId}/sync")
    @Operation(summary = "Força sincronização da conta")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Void> syncAccount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String accountId) {

        accountUseCase.syncAccountBalance(accountId);
        accountUseCase.syncAccountTransactions(accountId, 7);

        return ResponseEntity.accepted().build();
    }

    // Helper methods for response building
    private Links createLinks(int page, int pageSize, int totalRecords) {
        return Links.builder()
                .self("/accounts?page=" + page + "&page-size=" + pageSize)
                .first("/accounts?page=1&page-size=" + pageSize)
                .last("/accounts?page=" + ((totalRecords / pageSize) + 1) + "&page-size=" + pageSize)
                .build();
    }

    private Links createSelfLink(String resource) {
        return Links.builder()
                .self("/accounts/" + resource)
                .build();
    }

    private Meta createMeta(int totalRecords, int page, int pageSize) {
        return Meta.builder()
                .totalRecords(totalRecords)
                .totalPages((totalRecords / pageSize) + 1)
                .requestDateTime(LocalDateTime.now())
                .build();
    }

    private Meta createSimpleMeta() {
        return Meta.builder()
                .requestDateTime(LocalDateTime.now())
                .build();
    }
}
