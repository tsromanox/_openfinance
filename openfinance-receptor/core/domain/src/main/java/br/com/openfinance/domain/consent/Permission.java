package br.com.openfinance.domain.consent;

/**
 * Enum representing the different permissions that can be granted in a consent.
 * Based on Open Finance Brasil API specifications.
 */
public enum Permission {
    // Account permissions
    ACCOUNTS_READ("accounts", "Read account information"),
    ACCOUNTS_BALANCES_READ("accounts", "Read account balances"),
    ACCOUNTS_TRANSACTIONS_READ("accounts", "Read account transactions"),
    ACCOUNTS_OVERDRAFT_LIMITS_READ("accounts", "Read account overdraft limits"),
    
    // Credit card permissions  
    CREDIT_CARDS_ACCOUNTS_READ("credit-cards", "Read credit card account information"),
    CREDIT_CARDS_ACCOUNTS_LIMITS_READ("credit-cards", "Read credit card limits"),
    CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ("credit-cards", "Read credit card transactions"),
    CREDIT_CARDS_ACCOUNTS_BILLS_READ("credit-cards", "Read credit card bills"),
    
    // Resources permissions
    RESOURCES_READ("resources", "Read resources information"),
    
    // Loans permissions
    LOANS_READ("loans", "Read loans information"),
    LOANS_WARRANTIES_READ("loans", "Read loans warranties"),
    LOANS_SCHEDULED_INSTALLMENTS_READ("loans", "Read loans scheduled installments"),
    LOANS_PAYMENTS_READ("loans", "Read loans payments"),
    
    // Financings permissions
    FINANCINGS_READ("financings", "Read financings information"),
    FINANCINGS_WARRANTIES_READ("financings", "Read financings warranties"),
    FINANCINGS_SCHEDULED_INSTALLMENTS_READ("financings", "Read financings scheduled installments"),
    FINANCINGS_PAYMENTS_READ("financings", "Read financings payments");
    
    private final String category;
    private final String description;
    
    Permission(String category, String description) {
        this.category = category;
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAccountPermission() {
        return "accounts".equals(category);
    }
    
    public boolean isCreditCardPermission() {
        return "credit-cards".equals(category);
    }
    
    public boolean isLoanPermission() {
        return "loans".equals(category);
    }
    
    public boolean isFinancingPermission() {
        return "financings".equals(category);
    }
    
    public boolean isResourcePermission() {
        return "resources".equals(category);
    }
}
