package br.com.openfinance.core.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PerformanceMonitoringAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(Monitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(meterRegistry));

            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "error")
                    .tag("exception", e.getClass().getSimpleName())
                    .register(meterRegistry));

            throw e;
        }
    }
}
