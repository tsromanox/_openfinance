package br.com.openfinance.service.resources.domain;

/**
 * Enumeration of Open Finance Brasil API capabilities that resources can provide.
 */
public enum ResourceCapability {
    
    // Phase 1 Capabilities
    /**
     * Provides institutional and organizational information.
     */
    CHANNELS("Channels", "Branch and electronic channel information"),
    
    /**
     * Provides product and service information.
     */
    PRODUCTS_SERVICES("Products and Services", "Financial products and services catalog"),
    
    // Phase 2 Capabilities
    /**
     * Provides customer account information.
     */
    ACCOUNTS("Accounts", "Customer account data access"),
    
    /**
     * Provides credit card account information.
     */
    CREDIT_CARDS("Credit Cards", "Credit card account data access"),
    
    /**
     * Provides loan and financing information.
     */
    LOANS("Loans", "Loan and financing data access"),
    
    /**
     * Provides financing information.
     */
    FINANCINGS("Financings", "Financing data access"),
    
    /**
     * Provides invoice financing information.
     */
    INVOICE_FINANCINGS("Invoice Financings", "Invoice financing data access"),
    
    /**
     * Provides investment account information.
     */
    INVESTMENTS("Investments", "Investment account data access"),
    
    // Phase 3 Capabilities
    /**
     * Supports payment initiation via PIX.
     */
    PAYMENTS_PIX("PIX Payments", "PIX payment initiation"),
    
    /**
     * Supports payment initiation via TED.
     */
    PAYMENTS_TED("TED Payments", "TED payment initiation"),
    
    /**
     * Supports payment initiation via bank transfer.
     */
    PAYMENTS_TRANSFER("Bank Transfers", "Bank transfer payment initiation"),
    
    /**
     * Supports automatic payment setup.
     */
    AUTOMATIC_PAYMENTS("Automatic Payments", "Automatic payment setup and management"),
    
    // Phase 4 Capabilities (Future)
    /**
     * Provides insurance product information.
     */
    INSURANCE("Insurance", "Insurance product data access"),
    
    /**
     * Provides pension plan information.
     */
    PENSION("Pension", "Pension and retirement plan data access"),
    
    /**
     * Provides investment fund information.
     */
    INVESTMENT_FUNDS("Investment Funds", "Investment fund data access"),
    
    /**
     * Provides exchange and currency services.
     */
    EXCHANGE("Exchange", "Currency exchange services"),
    
    // Technical Capabilities
    /**
     * Supports real-time notifications.
     */
    WEBHOOKS("Webhooks", "Real-time event notifications"),
    
    /**
     * Supports bulk data operations.
     */
    BULK_OPERATIONS("Bulk Operations", "Bulk data processing capabilities"),
    
    /**
     * Supports advanced authentication methods.
     */
    ADVANCED_AUTH("Advanced Authentication", "Enhanced authentication mechanisms"),
    
    /**
     * Provides comprehensive API documentation.
     */
    API_DOCUMENTATION("API Documentation", "Comprehensive API documentation"),
    
    /**
     * Provides sandbox/testing environment.
     */
    SANDBOX("Sandbox", "Testing and development environment");
    
    private final String displayName;
    private final String description;
    
    ResourceCapability(String displayName, String description) {
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
     * Determines if this capability requires customer consent.
     */
    public boolean requiresConsent() {
        return switch (this) {
            case CHANNELS, PRODUCTS_SERVICES, API_DOCUMENTATION, SANDBOX -> false;
            default -> true;
        };
    }
    
    /**
     * Determines the Open Finance phase this capability belongs to.
     */
    public int getPhase() {
        return switch (this) {
            case CHANNELS, PRODUCTS_SERVICES -> 1;
            case ACCOUNTS, CREDIT_CARDS, LOANS, FINANCINGS, INVOICE_FINANCINGS, INVESTMENTS -> 2;
            case PAYMENTS_PIX, PAYMENTS_TED, PAYMENTS_TRANSFER, AUTOMATIC_PAYMENTS -> 3;
            case INSURANCE, PENSION, INVESTMENT_FUNDS, EXCHANGE -> 4;
            default -> 0; // Technical capabilities
        };
    }
    
    /**
     * Determines if this capability involves payment operations.
     */
    public boolean isPaymentCapability() {
        return switch (this) {
            case PAYMENTS_PIX, PAYMENTS_TED, PAYMENTS_TRANSFER, AUTOMATIC_PAYMENTS -> true;
            default -> false;
        };
    }
    
    /**
     * Determines if this capability involves data access.
     */
    public boolean isDataCapability() {
        return switch (this) {
            case ACCOUNTS, CREDIT_CARDS, LOANS, FINANCINGS, INVOICE_FINANCINGS, 
                 INVESTMENTS, INSURANCE, PENSION, INVESTMENT_FUNDS -> true;
            default -> false;
        };
    }
}