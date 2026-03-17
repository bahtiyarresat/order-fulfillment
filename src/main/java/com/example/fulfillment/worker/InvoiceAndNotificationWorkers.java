package com.example.fulfillment.worker;

import com.example.fulfillment.model.OrderDtos.InvoiceResult;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Workers: generate-invoice-pdf, send-email, send-sms
 *
 *   generate-invoice-pdf → AND parallel Branch 2 (run in parallel with reserve-inventory)
 *   send-email           → OR inclusive Path 1
 *                          BPMN condition: = notificationPreference = "Email" or notificationPreference = "Both"
 *   send-sms             → OR inclusive Path 2
 *                          BPMN condition: = notificationPreference = "SMS" or notificationPreference = "Both"
 */
@Component
public class InvoiceAndNotificationWorkers {

    private static final Logger log = LoggerFactory.getLogger(InvoiceAndNotificationWorkers.class);

    // ── generate-invoice-pdf ──────────────────────────────────────────────────

    @JobWorker(type = "generate-invoice-pdf")
    public Map<String, Object> generateInvoicePdf(@Variable String orderId) {

        String invoiceId     = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String invoicePdfUrl = "https://storage.example.com/invoices/" + invoiceId + ".pdf";

        log.info("[generate-invoice-pdf] orderId={} invoiceId={} url={}",
                orderId, invoiceId, invoicePdfUrl);

        InvoiceResult r = new InvoiceResult(invoiceId, invoicePdfUrl);

        return Map.of(
            "invoiceId",     r.invoiceId(),
            "invoicePdfUrl", r.invoicePdfUrl()
        );
    }

    // ── send-email ────────────────────────────────────────────────────────────

    @JobWorker(type = "send-email")
    public Map<String, Object> sendEmail(
            @Variable String orderId,
            @Variable String customerId
    ) {
        String  recipient = "customer-" + customerId + "@example.com";
        Instant sentAt    = Instant.now();

        log.info("[send-email] To={} Subject='Order {} confirmed'", recipient, orderId);

        return Map.of(
            "emailSentAt",    sentAt.toString(),
            "emailRecipient", recipient
        );
    }

    // ── send-sms ──────────────────────────────────────────────────────────────

    @JobWorker(type = "send-sms")
    public Map<String, Object> sendSms(
            @Variable String orderId,
            @Variable String customerId
    ) {
        String  recipient = "+44-7700-" + Math.abs(customerId.hashCode() % 1000000);
        Instant sentAt    = Instant.now();

        log.info("[send-sms] To={} Message='Order {} confirmed'", recipient, orderId);

        return Map.of(
            "smsSentAt",    sentAt.toString(),
            "smsRecipient", recipient
        );
    }
}
