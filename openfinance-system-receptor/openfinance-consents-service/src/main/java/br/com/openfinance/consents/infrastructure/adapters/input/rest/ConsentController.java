package br.com.openfinance.consents.infrastructure.adapters.input.rest;

import br.com.openfinance.consents.api.*;
import br.com.openfinance.consents.application.services.ConsentApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/open-banking/consents/v3/consents")
@RequiredArgsConstructor
public class ConsentController implements ConsentsApi {

    private final ConsentApplicationService consentService;

    @Override
    @GetMapping("/{consentId}")
    public ResponseEntity<ResponseConsent> getConsent(
            @PathVariable String consentId,
            @RequestHeader(value = "x-fapi-interaction-id", required = false)
            String xFapiInteractionId) {

        ResponseConsent consent = consentService.getConsent(consentId);
        return ResponseEntity.ok()
                .header("x-fapi-interaction-id", xFapiInteractionId)
                .body(consent);
    }

    @Override
    @PostMapping("/queue")
    public ResponseEntity<Void> queueConsentForProcessing(
            @Valid @RequestBody QueueConsentRequest request) {

        consentService.queueConsent(request);
        return ResponseEntity.accepted().build();
    }
}
