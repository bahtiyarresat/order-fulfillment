# Order Fulfillment Engine

A BPMN-driven order management process engine built with Camunda 8.8.7 and Spring Boot 3.3.5.

---

## Architecture

```
Webhook (POST /webhook/orders)
    ↓
Spring Boot (port 8080)
    ↓ gRPC :26500
Camunda Orchestration Cluster (Zeebe + Operate + Tasklist)
    ↓
Elasticsearch (secondary storage)
```

---

## Tech Stack

| Component | Version |
|---|---|
| Java | 17+ |
| Spring Boot | 3.3.5 |
| camunda-spring-boot-starter | 8.8.7 |
| Camunda Orchestration Cluster | 8.8.7 |
| Elasticsearch | 8.17.0 |

---

## BPMN Process — Order Fulfillment

```
Start (Webhook)
  → Validate Customer Credit          [Service Task: validate-credit]
  → Credit Decision? (XOR)
      ├── Rejected  → Order Rejected End
      ├── Review    → Manager Manual Review (User Task)
      │     ├── Timer 10 min (interrupting)     → Auto-Cancel Order End
      │     ├── Message: CustomerUpdatedPriority (non-interrupting)
      │     │     → Update SLA Log              [Service Task: update-sla-log]
      │     └── Manual Review Decision? (XOR)
      │           ├── Rejected → Order Rejected End
      │           └── Approved ↓
      └── Approved ───────────────────────────↓
  → Credit Secured (XOR Merge)
  → Parallel Execution (AND Split)
      ├── Reserve Inventory            [Service Task: reserve-inventory]
      └── Generate Invoice PDF         [Service Task: generate-invoice-pdf]
  → Fulfillment Joined (AND Join)
  → Select Notification Channel? (OR Inclusive)
      ├── Email / Both → Send Email    [Service Task: send-email]
      └── SMS / Both   → Send SMS      [Service Task: send-sms]
  → Notifications Sent (OR Join)
  → Order Fulfilled End
```

---

## Project Structure

```
order-fulfillment/
├── docker-compose.yml                       # Camunda 8.8.7 stack
├── .env                                     # Docker version variables
├── connector-secrets.txt                    # Connector secrets (can be empty)
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/fulfillment/
    │   │   ├── OrderFulfillmentApplication.java
    │   │   ├── controller/
    │   │   │   └── OrderWebhookController.java      # POST /webhook/orders
    │   │   ├── model/
    │   │   │   └── OrderDtos.java                   # Java Records (DTOs)
    │   │   └── worker/
    │   │       ├── CreditValidationWorker.java      # validate-credit
    │   │       ├── InventoryWorker.java             # reserve-inventory
    │   │       ├── SlaLogWorker.java                # update-sla-log
    │   │       └── InvoiceAndNotificationWorkers.java
    │   └── resources/
    │       ├── application.yaml                     # Base config
    │       ├── application-local.yaml               # Camunda Run profile
    │       ├── application-docker.yaml              # Docker Compose profile
    │       └── order-fulfillment.bpmn               # BPMN process definition
    └── test/
        └── java/com/example/fulfillment/worker/
            └── CreditValidationWorkerTest.java
```

---

## Setup & Running

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker + Docker Compose

### Step 1 — Start the Camunda Stack

Ensure the following files are in the same directory as `docker-compose.yml`:

**`.env`**
```env
CAMUNDA_VERSION=8.8.7
CAMUNDA_CONNECTORS_VERSION=8.8.7
ELASTIC_VERSION=8.17.0
```

**`connector-secrets.txt`** (empty file is fine)
```
# Add secrets as KEY=VALUE pairs if needed
```

Start the stack:
```bash
docker compose up -d
```

Wait for the orchestration cluster to be ready (~2-3 minutes):
```bash
docker compose logs -f orchestration
# Ready when you see: "Started StandaloneCamunda"
```

### Step 2 — Start the Spring Boot Application

**IntelliJ:**
- Run Configuration → Active profiles: `docker`
- Run `OrderFulfillmentApplication`

**Terminal:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

---

## Access URLs

| Service | URL                            | Credentials |
|---|--------------------------------|---|
| Spring Boot (Webhook) | http://localhost:8080          | — |
| Operate (process monitoring) | http://localhost:8088/operate  | demo / demo |
| Tasklist (user tasks) | http://localhost:8088/tasklist | demo / demo |
| Identity | http://localhost:8088/identity | demo / demo |
| Elasticsearch | http://localhost:9200          | — |
| Zeebe gRPC | localhost:26500                | — |

**Camunda Modeler connection:**
- Target: `Camunda 8 Self-Managed`
- Cluster URL: `http://localhost:8088`
- Authentication: `None`

---

## Triggering a Process

```bash
curl -X POST http://localhost:8080/webhook/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-001",
    "customerId": "CUST-42",
    "amount": 249.99,
    "notificationPreference": "Both"
  }'
```

**`notificationPreference` values:**
- `"Email"` — sends email only
- `"SMS"` — sends SMS only
- `"Both"` — sends both email and SMS

**Success response:**
```json
{
  "processInstanceKey": 2251799813685249,
  "orderId": "ORD-001",
  "status": "PROCESS_STARTED"
}
```

---

## BPMN Process Variables

| Variable | Type | Values | Used by |
|---|---|---|---|
| `creditDecision` | String | `"Approved"`, `"Rejected"`, `"Review"` | XOR Credit Decision gateway |
| `manualReviewDecision` | String | `"Approved"`, `"Rejected"` | XOR Manual Review gateway |
| `notificationPreference` | String | `"Email"`, `"SMS"`, `"Both"` | OR Inclusive gateway |
| `orderId` | String | — | Message correlation key |

---

## Job Workers

### validate-credit
- **Type:** Service Task
- **Behaviour:** Returns Approved (40%) / Rejected (30%) / Review (30%) randomly
- **Output:** `creditDecision`, `creditScore`, `creditReason`

### reserve-inventory
- **Type:** Service Task — AND parallel Branch 1
- **Behaviour:** Simulates a database row-level lock
- **Output:** `reservationId`, `inventoryReservedAt`

### generate-invoice-pdf
- **Type:** Service Task — AND parallel Branch 2
- **Behaviour:** Generates a mock invoice PDF URL
- **Output:** `invoiceId`, `invoicePdfUrl`

### update-sla-log
- **Type:** Service Task — triggered by non-interrupting message boundary
- **Behaviour:** Prints `"Priority Updated!"` to console; user task remains active
- **Output:** `slaPriorityFlag`, `slaLoggedAt`

### send-email
- **Type:** Service Task — OR inclusive Path 1
- **Condition:** `notificationPreference = "Email" or "Both"`
- **Output:** `emailSentAt`, `emailRecipient`

### send-sms
- **Type:** Service Task — OR inclusive Path 2
- **Condition:** `notificationPreference = "SMS" or "Both"`
- **Output:** `smsSentAt`, `smsRecipient`

---

## Spring Profiles

| Profile | Environment | Activation |
|---|---|---|
| `local` (default) | Camunda Run | No profile needed |
| `docker` | Docker Compose | `--spring.profiles.active=docker` |

---

## Testing the Non-Interrupting Message Boundary

Send the `CustomerUpdatedPriority` message while a process instance is waiting at the Manager Manual Review user task:

```bash
curl -X POST http://localhost:8088/v2/messages/publication \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'demo:demo' | base64)" \
  -d '{
    "messageName": "CustomerUpdatedPriority",
    "correlationKey": "ORD-001",
    "variables": {
      "priorityFlag": "URGENT"
    }
  }'
```

The Manager Manual Review task is **not cancelled**. The `Update SLA Log` service task runs in a parallel branch and prints `Priority Updated!` to the console.

---

## Cleanup

```bash
# Stop stack, keep data
docker compose down

# Stop stack, remove ALL data (clean slate)
docker compose down -v
```
