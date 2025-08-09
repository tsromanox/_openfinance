package br.com.openfinance.core.infrastructure.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeUtils {

    private static final ZoneId BRAZIL_TIMEZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter BRAZIL_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static LocalDateTime nowInBrazil() {
        return LocalDateTime.now(BRAZIL_TIMEZONE);
    }

    public static String formatToBrazilian(LocalDateTime dateTime) {
        return dateTime.format(BRAZIL_FORMATTER);
    }

    public static String formatToISO(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }

    public static LocalDateTime parseFromISO(String isoDateTime) {
        return LocalDateTime.parse(isoDateTime, ISO_FORMATTER);
    }

    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public static boolean isExpired(LocalDateTime expirationTime) {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    public static LocalDateTime addBusinessDays(LocalDateTime date, int days) {
        LocalDateTime result = date;
        int addedDays = 0;

        while (addedDays < days) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                addedDays++;
            }
        }

        return result;
    }
}
