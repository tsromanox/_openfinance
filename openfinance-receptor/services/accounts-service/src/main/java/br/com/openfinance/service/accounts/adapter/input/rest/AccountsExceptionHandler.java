package br.com.openfinance.service.accounts.adapter.input.rest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.LocalDateTime;

@RestControllerAdvice
public class AccountsExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountsExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(
            AccountNotFoundException ex, WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage()
        );
        problemDetail.setTitle("Account Not Found");
        problemDetail.setType(URI.create("https://api.openfinance.com/errors/account-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(InvalidAccountStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAccountState(
            InvalidAccountStateException ex, WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()
        );
        problemDetail.setTitle("Invalid Account State");
        problemDetail.setType(URI.create("https://api.openfinance.com/errors/invalid-state"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    @ExceptionHandler(AccountSyncException.class)
    public ResponseEntity<ProblemDetail> handleAccountSyncError(
            AccountSyncException ex, WebRequest request) {

        log.error("Account sync error", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Unable to sync account data"
        );
        problemDetail.setTitle("Sync Error");
        problemDetail.setType(URI.create("https://api.openfinance.com/errors/sync-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }

    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientPermissions(
            InsufficientPermissionsException ex, WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, ex.getMessage()
        );
        problemDetail.setTitle("Insufficient Permissions");
        problemDetail.setType(URI.create("https://api.openfinance.com/errors/insufficient-permissions"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericError(
            Exception ex, WebRequest request) {

        log.error("Unexpected error", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.openfinance.com/errors/internal-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
