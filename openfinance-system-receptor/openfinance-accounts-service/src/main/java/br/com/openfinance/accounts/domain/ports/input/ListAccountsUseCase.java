package br.com.openfinance.accounts.domain.ports.input;

import br.com.openfinance.accounts.domain.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListAccountsUseCase {
    Page<Account> listAccountsByCustomer(String customerId, Pageable pageable);
    Page<Account> listAccountsByParticipant(String participantId, Pageable pageable);
}
