package com.example.fulfillment.worker;

import com.example.fulfillment.model.OrderDtos.InventoryReservation;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Worker: reserve-inventory
 *
 * Branch 1 of the AND parallel split.
 * Runs concurrently with generate-invoice-pdf.
 * Simulates acquiring a database row-level lock for the ordered items.
 */
@Component
public class InventoryWorker {

    private static final Logger log = LoggerFactory.getLogger(InventoryWorker.class);

    @JobWorker(type = "reserve-inventory")
    public Map<String, Object> reserveInventory(@Variable String orderId) {

        String  reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant reservedAt    = Instant.now();

        // Simulated DB lock log (required by assignment spec)
        log.info("[reserve-inventory] *** DB LOCK ACQUIRED ***");
        log.info("[reserve-inventory] SQL → UPDATE inventory " +
                        "SET locked = TRUE, reservation_id = '{}' " +
                        "WHERE order_id = '{}' AND available = TRUE",
                reservationId, orderId);
        log.info("[reserve-inventory] reservationId={} lockedAt={}", reservationId, reservedAt);

        InventoryReservation r = new InventoryReservation(reservationId, reservedAt);

        return Map.of(
                "reservationId",       r.reservationId(),
                "inventoryReservedAt", r.reservedAt().toString()
        );
    }
}