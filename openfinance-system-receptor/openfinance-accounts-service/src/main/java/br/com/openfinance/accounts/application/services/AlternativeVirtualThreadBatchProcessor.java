package br.com.openfinance.accounts.application.services;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.ports.output.AccountRepository;
import br.com.openfinance.accounts.domain.ports.output.TransmitterAccountClient;
import br.com.openfinance.accounts.domain.ports.output.AccountEventPublisher;
import br.com.openfinance.accounts.domain.services.AccountDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlternativeVirtualThreadBatchProcessor {

    private final AccountRepository accountRepository;
    private final TransmitterAccountClient transmitterClient;
    private final AccountEventPublisher eventPublisher;
    private final AccountDomainService domainService;

    @Value("${openfinance.accounts.batch.size:1000}")
    private int batchSize;

    public void processAccountsWithPagination() {
        log.info("Starting paginated batch processing with Virtual Threads");

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        int pageNumber = 0;
        boolean hasMore = true;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            while (hasMore) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<Account> accountPage = accountRepository.findAllActive(pageable);

                if (accountPage.isEmpty()) {
                    hasMore = false;
                    break;
                }

                List<CompletableFuture<Boolean>> futures = accountPage.getContent().stream()
                        .map(account -> CompletableFuture.supplyAsync(
                                () -> processAccount(account), executor))
                        .toList();

                // Wait for batch completion
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Count successes
                long successes = futures.stream()
                        .map(CompletableFuture::join)
                        .filter(success -> success)
                        .count();

                totalProcessed.addAndGet((int) successes);
                totalErrors.addAndGet(accountPage.getNumberOfElements() - (int) successes);

                log.info("Processed page {}: {} accounts, {} errors",
                        pageNumber, successes, accountPage.getNumberOfElements() - successes);

                pageNumber++;
                hasMore = accountPage.hasNext();
            }

        }

        log.info("Batch processing completed. Total processed: {}, Total errors: {}",
                totalProcessed.get(), totalErrors.get());

        eventPublisher.publishBatchSyncCompleted(totalProcessed.get());
    }

    private boolean processAccount(Account account) {
        try {
            if (!domainService.shouldSyncAccount(account)) {
                return true;
            }

            Account externalData = transmitterClient.fetchAccount(
                    account.getParticipantId(),
                    account.getExternalAccountId()
            );

            domainService.updateAccountFromExternal(account, externalData);
            accountRepository.save(account);

            return true;
        } catch (Exception e) {
            log.error("Error processing account {}: {}", account.getAccountId(), e.getMessage());
            return false;
        }
    }
}
