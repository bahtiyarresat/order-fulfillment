package com.example.fulfillment.worker;

import com.example.fulfillment.model.OrderDtos.CreditCheckResult;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Worker: validate-credit
 *
 * Produces the variable that drives the XOR "Credit Decision?" gateway:
 *   creditDecision = "Approved" → proceeds to AND parallel split
 *   creditDecision = "Rejected" → routes to Order Rejected end event
 *   creditDecision = "Review"   → routes to Manager Manual Review user task
 *
 * Distribution: 40% Approved / 30% Rejected / 30% Review
 */
@Component
public class CreditValidationWorker {

    private static final Logger log = LoggerFactory.getLogger(CreditValidationWorker.class);
    private static final Random RNG = new Random();

    private static final String[] OUTCOMES = {
            "Approved", "Approved", "Approved", "Approved",
            "Rejected", "Rejected", "Rejected",
            "Review",   "Review",   "Review"
    };

    @JobWorker(type = "validate-credit")
    public Map<String, Object> validateCredit(
            @Variable String orderId,
            @Variable String customerId
    ) {
        log.debug("[validate-credit] orderId={} customerId={}", orderId, customerId);

        String decision = OUTCOMES[RNG.nextInt(OUTCOMES.length)];

        int score = switch (decision) {
            case "Approved" -> 650 + RNG.nextInt(201);   // 650–850
            case "Rejected" -> 300 + RNG.nextInt(200);   // 300–499
            default         -> 500 + RNG.nextInt(150);   // 500–649
        };

        String reason = switch (decision) {
            case "Approved" -> "Credit score above threshold.";
            case "Rejected" -> "Credit score below minimum threshold.";
            default         -> "Score in review band — manual approval required.";
        };

        CreditCheckResult result = new CreditCheckResult(decision, score, reason);

        log.info("[validate-credit] creditDecision={} score={} reason={}",
                result.creditDecision(), result.creditScore(), result.creditReason());

        // Key names must match the BPMN FEEL expression variable references exactly
        return Map.of(
                "creditDecision", result.creditDecision(),
                "creditScore",    result.creditScore(),
                "creditReason",   result.creditReason()
        );
    }
}