package br.com.openfinance.accounts.infrastructure.adapters.input.rest;

import br.com.openfinance.accounts.api.AccountsApi;
import br.com.openfinance.accounts.api.model.*;
import br.com.openfinance.accounts.application.dto.*;
import br.com.openfinance.accounts.application.mapper.AccountMapper;
import br.com.openfinance.accounts.application.services.AccountApplicationService;
import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.model.AccountBalance;
import br.com.openfinance.accounts.domain.model.AccountTransaction;
import br.com.openfinance.core.application.dto.PageResponse;
import br.com.openfinance.core.domain.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/open-banking/accounts/v3")
@Tag(name = "Accounts", description = "Account management endpoints")
@Slf4j
@RequiredArgsConstructor
public class AccountController implements AccountsApi {

    private final AccountApplicationService accountService;
    private final AccountMapper accountMapper;

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ResponseAccount> getAccount(
            @PathVariable UUID accountId,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String xFapiInteractionId) {

        log.info("Getting account: {}", accountId);

        Account account = accountService.getAccount(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));

        AccountDTO dto = accountMapper.toDto(account);
        ResponseAccount response = buildAccountResponse(dto);

        return ResponseEntity.ok()
                .header("x-fapi-interaction-id", xFapiInteractionId)
                .body(response);
    }

    @GetMapping("/accounts")
    @Operation(summary = "List accounts")
    public ResponseEntity<ResponseAccountList> listAccounts(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String participantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String xFapiInteractionId) {

        log.info("Listing accounts. Customer: {}, Participant: {}", customerId, participantId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accounts;

        if (customerId != null) {
            accounts = accountService.listAccountsByCustomer(customerId, pageable);
        } else if (participantId != null) {
            accounts = accountService.listAccountsByParticipant(participantId, pageable);
        } else {
            throw new ValidationException("Either customerId or participantId must be provided");
        }

        ResponseAccountList response = buildAccountListResponse(accounts);

        return ResponseEntity.ok()
                .header("x-fapi-interaction-id", xFapiInteractionId)
                .body(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    @Operation(summary = "Get account transactions")
    public ResponseEntity<ResponseAccountTransactions> getAccountTransactions(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String xFapiInteractionId) {

        log.info("Getting transactions for account {} from {} to {}", accountId, fromDate, toDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<AccountTransaction> transactions = accountService.getTransactions(
                accountId, fromDate, toDate, pageable);

        ResponseAccountTransactions response = buildTransactionResponse(transactions);

        return ResponseEntity.ok()
                .header("x-fapi-interaction-id", xFapiInteractionId)
                .body(response);
    }

    @GetMapping("/accounts/{accountId}/balances")
    @Operation(summary = "Get account balance")
    public ResponseEntity<ResponseAccountBalances> getAccountBalance(
            @PathVariable UUID accountId,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String xFapiInteractionId) {

        log.info("Getting balance for account: {}", accountId);

        AccountBalance balance = accountService.getCurrentBalance(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Balance not found for account", accountId.toString()));

        AccountBalanceDTO dto = accountMapper.toBalanceDto(balance);
        ResponseAccountBalances response = buildBalanceResponse(dto);

        return ResponseEntity.ok()
                .header("x-fapi-interaction-id", xFapiInteractionId)
                .body(response);
    }

    @PostMapping("/accounts/{accountId}/sync")
    @Operation(summary = "Sync account data")
    public ResponseEntity<Void> syncAccount(@PathVariable UUID accountId) {
        log.info("Syncing account: {}", accountId);

        accountService.syncAccount(accountId);

        return ResponseEntity.accepted().build();
    }

    private ResponseAccount buildAccountResponse(AccountDTO account) {
        // Build OpenAPI response object
        return new ResponseAccount();
    }

    private ResponseAccountList buildAccountListResponse(Page<Account> accounts) {
        // Build OpenAPI response object
        return new ResponseAccountList();
    }

    private ResponseAccountTransactions buildTransactionResponse(Page<AccountTransaction> transactions) {
        // Build OpenAPI response object
        return new ResponseAccountTransactions();
    }

    private ResponseAccountBalances buildBalanceResponse(AccountBalanceDTO balance) {
        // Build OpenAPI response object
        return new ResponseAccountBalances();
    }
}
