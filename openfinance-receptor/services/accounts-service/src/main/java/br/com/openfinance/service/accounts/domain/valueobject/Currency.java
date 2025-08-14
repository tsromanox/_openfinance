package br.com.openfinance.service.accounts.domain.valueobject;

public enum Currency {
    BRL("Real Brasileiro"),
    USD("Dólar Americano"),
    EUR("Euro");

    private final String description;

    Currency(String description) {
        this.description = description;
    }
}
