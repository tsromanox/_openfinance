package br.com.openfinance.core.infrastructure.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CPFValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCPF {
    String message() default "Invalid CPF";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
