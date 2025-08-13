package br.com.openfinance.service.resources.domain;

/**
 * Enumeration of Open Finance Brasil resource types.
 */
public enum ResourceType {
    
    /**
     * Full-service bank with comprehensive Open Finance capabilities.
     */
    BANK("Bank", "Full-service banking institution"),
    
    /**
     * Credit union or cooperative financial institution.
     */
    CREDIT_UNION("Credit Union", "Credit union or cooperative"),
    
    /**
     * Fintech company providing financial services.
     */
    FINTECH("Fintech", "Financial technology company"),
    
    /**
     * Payment institution focused on payment services.
     */
    PAYMENT_INSTITUTION("Payment Institution", "Specialized payment services"),
    
    /**
     * Credit card company or credit provider.
     */
    CREDIT_PROVIDER("Credit Provider", "Credit and lending services"),
    
    /**
     * Investment firm or asset management company.
     */
    INVESTMENT_FIRM("Investment Firm", "Investment and asset management"),
    
    /**
     * Insurance company providing insurance products.
     */
    INSURANCE_COMPANY("Insurance Company", "Insurance products and services"),
    
    /**
     * Broker or intermediary financial services.
     */
    BROKER("Broker", "Financial intermediary services"),
    
    /**
     * Pension fund or retirement services provider.
     */
    PENSION_FUND("Pension Fund", "Retirement and pension services"),
    
    /**
     * Other type of financial institution not covered above.
     */
    OTHER("Other", "Other financial institution");
    
    private final String displayName;
    private final String description;
    
    ResourceType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if this resource type typically provides account services.
     */
    public boolean providesAccountServices() {
        return this == BANK || this == CREDIT_UNION || this == FINTECH;
    }
    
    /**
     * Determines if this resource type typically provides payment services.
     */
    public boolean providesPaymentServices() {
        return this == BANK || this == PAYMENT_INSTITUTION || this == FINTECH;
    }
    
    /**
     * Determines if this resource type typically provides credit services.
     */
    public boolean providesCreditServices() {
        return this == BANK || this == CREDIT_PROVIDER || this == FINTECH;
    }
    
    /**
     * Determines if this resource type typically provides investment services.
     */
    public boolean providesInvestmentServices() {
        return this == BANK || this == INVESTMENT_FIRM || this == BROKER;
    }
}