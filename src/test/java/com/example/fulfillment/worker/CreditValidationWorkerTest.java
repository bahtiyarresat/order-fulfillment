package com.example.fulfillment.worker;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreditValidationWorker unit tests.
 * Camunda/Spring no need context — worker is a plain POJO.
 */
class CreditValidationWorkerTest {

    private final CreditValidationWorker worker = new CreditValidationWorker();

    @Test
    void shouldReturnAllBpmnVariableKeys() {
        Map<String, Object> result = worker.validateCredit("ORD-001", "CUST-1");
        assertThat(result).containsKeys("creditDecision", "creditScore", "creditReason");
    }

    @Test
    void creditDecisionMustBeValidGatewayValue() {
        Map<String, Object> result = worker.validateCredit("ORD-001", "CUST-1");
        assertThat((String) result.get("creditDecision"))
                .isIn("Approved", "Rejected", "Review");
    }

    @Test
    void scoreShouldMatchDecisionBand() {
        for (int i = 0; i < 200; i++) {
            Map<String, Object> result = worker.validateCredit("ORD-" + i, "CUST-1");
            String decision = (String) result.get("creditDecision");
            int    score    = (int)   result.get("creditScore");
            switch (decision) {
                case "Approved" -> assertThat(score).isBetween(650, 850);
                case "Rejected" -> assertThat(score).isBetween(300, 499);
                case "Review"   -> assertThat(score).isBetween(500, 649);
            }
        }
    }

    @Test
    void allThreeXorPathsMustBeReachable() {
        Set<String> seen = IntStream.range(0, 500)
                .mapToObj(i -> (String) worker
                        .validateCredit("ORD-" + i, "CUST-TEST")
                        .get("creditDecision"))
                .collect(Collectors.toSet());

        assertThat(seen)
                .as("All 3 XOR paths should be accessible")
                .containsExactlyInAnyOrder("Approved", "Rejected", "Review");
    }

    @RepeatedTest(20)
    void creditReasonShouldNeverBeNull() {
        Map<String, Object> result = worker.validateCredit("ORD-X", "CUST-Y");
        assertThat(result.get("creditReason")).isNotNull();
    }
}
