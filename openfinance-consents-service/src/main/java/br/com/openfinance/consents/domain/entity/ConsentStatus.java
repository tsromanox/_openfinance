package br.com.openfinance.consents.domain.entity;

public enum ConsentStatus {
    AWAITING_AUTHORISATION,
    AUTHORISED,
    REJECTED,
    CONSUMED,
    REVOKED,
    EXPIRED
}
