package br.com.openfinance.accounts.domain.ports.input;

import br.com.openfinance.accounts.domain.model.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.UUID;

public interface GetAccountTransactionsUseCase {
    Page<AccountTransaction> getTransactions(
            UUID accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );
}
