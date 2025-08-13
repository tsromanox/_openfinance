package br.com.openfinance;

/**
 * Legacy main class - replaced by OpenFinanceReceptorApplication.
 * This class is kept for backward compatibility.
 * 
 * @deprecated Use OpenFinanceReceptorApplication instead
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting OpenFinance Receptor via legacy Main class...");
        System.out.println("Delegating to OpenFinanceReceptorApplication...");
        
        // Delegate to the main application class
        OpenFinanceReceptorApplication.main(args);
    }
}