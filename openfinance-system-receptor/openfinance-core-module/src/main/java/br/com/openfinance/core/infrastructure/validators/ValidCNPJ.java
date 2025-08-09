package br.com.openfinance.core.infrastructure.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CNPJValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCNPJ {
    String message() default "Invalid CNPJ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
