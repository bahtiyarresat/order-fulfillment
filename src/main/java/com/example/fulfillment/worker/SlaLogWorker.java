package com.example.fulfillment.worker;

import com.example.fulfillment.model.OrderDtos.SlaLogEntry;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Worker: update-sla-log
 *
 * NON-INTERRUPTING triggered by Message Boundary Event.
 * BPMN'de: cancelActivity="false"
 *   → Manager Manual Review user task is still active
 *   → This worker runs in parallel
 *
 * Message correlation on BPMN:
 *   <zeebe:subscription correlationKey="= orderId"/>
 *
 * Requirement of Assignment: print into console "Priority Updated!".
 */
@Component
public class SlaLogWorker {

    private static final Logger log = LoggerFactory.getLogger(SlaLogWorker.class);

    @JobWorker(type = "update-sla-log", fetchVariables = {"orderId", "priorityFlag"})
    public Map<String, Object> updateSlaLog(
            @Variable String orderId,
            @Variable String priorityFlag
    ) {
        // Assignment spec's console output
        System.out.println("Priority Updated!");

        String  flag     = (priorityFlag != null) ? priorityFlag : "HIGH";
        Instant loggedAt = Instant.now();

        log.info("[update-sla-log] *** NON-INTERRUPTING MESSAGE BOUNDARY FIRED ***");
        log.info("[update-sla-log] orderId={} priorityFlag={} loggedAt={}", orderId, flag, loggedAt);
        log.info("[update-sla-log] 'Manager Manual Review' user task is still ACTIVE.");

        SlaLogEntry entry = new SlaLogEntry(flag, loggedAt);

        return Map.of(
            "slaPriorityFlag", entry.priorityFlag(),
            "slaLoggedAt",     entry.loggedAt().toString()
        );
    }
}
