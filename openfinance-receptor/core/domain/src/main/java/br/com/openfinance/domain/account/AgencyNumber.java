package br.com.openfinance.domain.account;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a bank agency number with its check digit.
 */
public class AgencyNumber {
    private static final Pattern AGENCY_NUMBER_PATTERN = Pattern.compile("\\d{1,10}");
    private static final Pattern CHECK_DIGIT_PATTERN = Pattern.compile("\\d{0,2}");
    
    private final String number;
    private final String checkDigit;
    
    public AgencyNumber(String number, String checkDigit) {
        this.number = Objects.requireNonNull(number, "Agency number cannot be null");
        this.checkDigit = checkDigit != null ? checkDigit : "";
        
        validate();
    }
    
    private void validate() {
        if (!AGENCY_NUMBER_PATTERN.matcher(number).matches()) {
            throw new IllegalArgumentException("Invalid agency number format: " + number);
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
        return checkDigit.isEmpty() ? number : number + "-" + checkDigit;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgencyNumber that = (AgencyNumber) o;
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