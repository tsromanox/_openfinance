package br.com.openfinance.service.accounts.domain.valueobject;

/**
 * Value Object - AccountIdentification
 */
public class AccountIdentification {
    private final String compeCode;
    private final String branchCode;
    private final String number;
    private final String checkDigit;

    public AccountIdentification(String compeCode, String branchCode,
                                 String number, String checkDigit) {
        this.compeCode = validateCompeCode(compeCode);
        this.branchCode = validateBranchCode(branchCode);
        this.number = validateNumber(number);
        this.checkDigit = validateCheckDigit(checkDigit);
    }

    private String validateCompeCode(String code) {
        if (code == null || !code.matches("^\\d{3}$")) {
            throw new IllegalArgumentException("Invalid COMPE code");
        }
        return code;
    }

    private String validateBranchCode(String code) {
        if (code == null || !code.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Invalid branch code");
        }
        return code;
    }

    private String validateNumber(String number) {
        if (number == null || !number.matches("^\\d{8,20}$")) {
            throw new IllegalArgumentException("Invalid account number");
        }
        return number;
    }

    private String validateCheckDigit(String digit) {
        if (digit == null || digit.length() != 1) {
            throw new IllegalArgumentException("Invalid check digit");
        }
        return digit;
    }

    // Getters
    public String getCompeCode() { return compeCode; }
    public String getBranchCode() { return branchCode; }
    public String getNumber() { return number; }
    public String getCheckDigit() { return checkDigit; }

    public String getFormattedNumber() {
        return String.format("%s.%s.%s-%s", compeCode, branchCode, number, checkDigit);
    }
}
