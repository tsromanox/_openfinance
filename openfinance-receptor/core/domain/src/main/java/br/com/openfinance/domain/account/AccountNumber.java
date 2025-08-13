package br.com.openfinance.domain.account;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a bank account number with its check digit.
 */
public class AccountNumber {
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("\\d{1,20}");
    private static final Pattern CHECK_DIGIT_PATTERN = Pattern.compile("\\d{1,2}");
    
    private final String number;
    private final String checkDigit;
    
    public AccountNumber(String number, String checkDigit) {
        this.number = Objects.requireNonNull(number, "Account number cannot be null");
        this.checkDigit = Objects.requireNonNull(checkDigit, "Check digit cannot be null");
        
        validate();
    }
    
    private void validate() {
        if (!ACCOUNT_NUMBER_PATTERN.matcher(number).matches()) {
            throw new IllegalArgumentException("Invalid account number format: " + number);
        }
        if (!CHECK_DIGIT_PATTERN.matcher(checkDigit).matches()) {
            throw new IllegalArgumentException("Invalid check digit format: " + checkDigit);
        }
    }
    
    public String getNumber() {
        return number;
    }
    
    public String getCheckDigit() {
        return checkDigit;
    }
    
    public String getFullNumber() {
        return number + "-" + checkDigit;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountNumber that = (AccountNumber) o;
        return Objects.equals(number, that.number) && Objects.equals(checkDigit, that.checkDigit);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(number, checkDigit);
    }
    
    @Override
    public String toString() {
        return getFullNumber();
    }
}