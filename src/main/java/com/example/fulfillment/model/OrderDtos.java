package com.example.fulfillment.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Process variable DTOs — Java Records.
 *
 * Variable names match the BPMN FEEL expressions exactly:
 *   creditDecision         → XOR "Credit Decision?" gateway
 *   manualReviewDecision   → XOR "Manual Review Decision?" gateway
 *   notificationPreference → OR  "Select Notification Channel?" gateway
 */
public final class OrderDtos {

    private OrderDtos() {}

    /**
     * Webhook payload — initial process variables.
     *
     * @param orderId                Unique order identifier.
     *                               Also used as the BPMN message correlation key:
     *                               {@code <zeebe:subscription correlationKey="= orderId"/>}
     * @param customerId             Customer identifier
     * @param amount                 Order total
     * @param notificationPreference "Email" | "SMS" | "Both"
     */
    public record OrderRequest(
            String     orderId,
            String     customerId,
            BigDecimal amount,
            String     notificationPreference
    ) {}

    /**
     * Output of the validate-credit worker.
     *
     * @param creditDecision "Approved" | "Rejected" | "Review"
     * @param creditScore    Mock credit score (300–850)
     * @param creditReason   Human-readable decision reason
     */
    public record CreditCheckResult(
            String creditDecision,
            int    creditScore,
            String creditReason
    ) {}

    /**
     * Variable set by the finance manager when completing the User Task.
     *
     * @param manualReviewDecision "Approved" | "Rejected"
     */
    public record ManualReviewResult(
            String manualReviewDecision
    ) {}

    /** Output of the reserve-inventory worker. */
    public record InventoryReservation(
            String  reservationId,
            Instant reservedAt
    ) {}

    /** Output of the generate-invoice-pdf worker. */
    public record InvoiceResult(
            String invoiceId,
            String invoicePdfUrl
    ) {}

    /** Output of the update-sla-log worker. */
    public record SlaLogEntry(
            String  priorityFlag,
            Instant loggedAt
    ) {}
}