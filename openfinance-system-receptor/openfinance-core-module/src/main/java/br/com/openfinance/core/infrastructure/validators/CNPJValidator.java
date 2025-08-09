package br.com.openfinance.core.infrastructure.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CNPJValidator implements ConstraintValidator<ValidCNPJ, String> {

    @Override
    public boolean isValid(String cnpj, ConstraintValidatorContext context) {
        if (cnpj == null || cnpj.isEmpty()) {
            return true;
        }

        cnpj = cnpj.replaceAll("[^0-9]", "");

        if (cnpj.length() != 14) {
            return false;
        }

        // Check for known invalid CNPJs
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        // CNPJ validation algorithm
        int[] multiplicador1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] multiplicador2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (cnpj.charAt(i) - '0') * multiplicador1[i];
        }

        int resto = soma % 11;
        int digito1 = resto < 2 ? 0 : 11 - resto;

        if (digito1 != (cnpj.charAt(12) - '0')) {
            return false;
        }

        soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += (cnpj.charAt(i) - '0') * multiplicador2[i];
        }

        resto = soma % 11;
        int digito2 = resto < 2 ? 0 : 11 - resto;

        return digito2 == (cnpj.charAt(13) - '0');
    }
}
