package com.example.fulfillment.controller;

import com.example.fulfillment.model.OrderDtos.OrderRequest;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook endpoint — Triggering BPMN Start Event.
 *
 * Test:
 *   curl -X POST http://localhost:8088/webhook/orders \
 *     -H "Content-Type: application/json" \
 *     -d '{
 *           "orderId":                "ORD-001",
 *           "customerId":             "CUST-42",
 *           "amount":                 249.99,
 *           "notificationPreference": "Both"
 *         }'
 */
@RestController
@RequestMapping("/webhook")
public class OrderWebhookController {

    private static final Logger log = LoggerFactory.getLogger(OrderWebhookController.class);

    /** BPMN <bpmn:process id="..."> must match */
    private static final String BPMN_PROCESS_ID = "order_fulfillment_process";

    private final CamundaClient camundaClient;

    public OrderWebhookController(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> receiveOrder(@RequestBody OrderRequest order) {

        log.info("[webhook] orderId={} customerId={} amount={} channel={}",
                order.orderId(), order.customerId(),
                order.amount(), order.notificationPreference());

        Map<String, Object> variables = Map.of(
            "orderId",                order.orderId(),
            "customerId",             order.customerId(),
            "amount",                 order.amount(),
            "notificationPreference", order.notificationPreference()
        );

        ProcessInstanceEvent event = camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId(BPMN_PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        log.info("[webhook] Process started: processInstanceKey={}", event.getProcessInstanceKey());

        return ResponseEntity.accepted().body(Map.of(
            "processInstanceKey", event.getProcessInstanceKey(),
            "orderId",            order.orderId(),
            "status",             "PROCESS_STARTED"
        ));
    }
}
