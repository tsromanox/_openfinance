package br.com.openfinance.core.infrastructure.monitoring;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Monitored {
}
