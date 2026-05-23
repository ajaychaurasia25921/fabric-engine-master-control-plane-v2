# FABRIC ENGINE MASTER CONTROL PLANE v2
## Requirements Document - MAIN BRANCH
### Spring WebFlux + Vue 3 Production Implementation

**Document Version:** 1.0  
**Date:** May 23, 2026  
**Status:** Requirements Finalized  
**Branch:** main  
**Repository:** https://github.com/ajaychaurasia25921/fabric-engine-master-control-plane-v2

---

## TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [System Overview & Architecture](#system-overview--architecture)
3. [Functional Requirements](#functional-requirements)
4. [Non-Functional Requirements](#non-functional-requirements)
5. [API Contract (OpenAPI 3.1.0)](#api-contract-openapi-310)
6. [Technology Stack](#technology-stack)
7. [Configuration & Environment](#configuration--environment)
8. [Data Persistence & Schema](#data-persistence--schema)
9. [Deployment Architecture](#deployment-architecture)
10. [Testing & Quality Assurance](#testing--quality-assurance)
11. [Known Constraints](#known-constraints)
12. [Acceptance Criteria](#acceptance-criteria)
13. [Glossary](#glossary)

---

## EXECUTIVE SUMMARY

The **Fabric Engine Master Control Plane v2** is a **next-generation reactive control-plane platform** for network fabric operations. It combines:

- ✅ **Reactive Event Processing**: Spring WebFlux functional routes for non-blocking I/O
- ✅ **AI-Driven Anomaly Detection**: Spring AI `FabricAgent` with local Ollama LLM
- ✅ **Real-Time Visualization**: Vue 3 topology canvas with VueFlow
- ✅ **SMS Notification**: SMPP-backed telemetry dispatch
- ✅ **Polyglot Functions**: GraalVM JVM executing JavaScript, Python, etc.
- ✅ **Cloud-Native**: Docker Compose local dev, Render production deployment

### Current State
- **Backend**: Spring Boot 3.5.14 + WebFlux
- **Frontend**: Vue 3 Composition API + Vite
- **Database**: PostgreSQL 16 + R2DBC async driver
- **Deployment**: Docker Compose (local), Render (production)
- **Version**: 2.0.0

### Key Features
1. **Fabric Topology Management**: CRUD operations on network nodes/edges
2. **Event Ingestion & Streaming**: Pub/Sub via SSE (Server-Sent Events)
3. **AI-Powered Decisions**: Ollama LLM integrated for anomaly analysis
4. **SMS Gateway Integration**: Real-time operational alerts via SMPP
5. **Serverless Code Generation**: GraalVM polyglot for AWS Lambda/Azure Functions
6. **Enterprise Provisioning**: Multi-role server deployment (DB/App/Honeypot)

### Target Users
- Network operations teams (NOC)
- Cloud infrastructure engineers
- DevOps automation specialists
- AI/ML engineers (for anomaly detection tuning)

---

## SYSTEM OVERVIEW & ARCHITECTURE

### 2.1 Core Components

```
┌──────────────────────────────────────────────────────────┐
│                  Frontend (Vue 3 + Vite)                 │
│  - VueFlow topology canvas (nodes/edges visualization)   │
│  - Pinia state management (fabric, events, SMS)          │
│  - SSE EventSource for real-time updates                 │
│  - SMS dispatch side panel                               │
└────────────────────┬─────────────────────────────────────┘
                     │ HTTP/REST + SSE
                     │ Port: 5173
                     ▼
┌──────────────────────────────────────────────────────────┐
│         Backend (Spring WebFlux on Netty)                │
│  - Functional routing (lambda-based)                     │
│  - R2DBC driver (non-blocking Postgres)                  │
│  - Spring AI FabricAgent (Ollama integration)            │
│  - GraalVM polyglot runtime                              │
│  - SMPP SMS client library                               │
└────────────────────┬─────────────────────────────────────┘
                     │ Port: 8080
           ┌─────────┼─────────┐
           │         │         │
           ▼         ▼         ▼
      PostgreSQL   Ollama    GraalVM
      (R2DBC)    (port 11434) VM
      Port:5432
```

### 2.2 Service Architecture

```
Spring WebFlux Router
├─ GET  /api/v1/fabric/topology          → TopologyHandler.get()
├─ POST /api/v1/fabric/events            → EventHandler.publish()
├─ GET  /api/v1/fabric/stream            → EventHandler.stream() [SSE]
├─ POST /sms/dispatches                  → SmsHandler.dispatch()
├─ GET  /sms/gateways/status             → SmsHandler.status()
├─ POST /api/v1/platform/functions/blueprints  → PolyglotHandler.generate()
├─ POST /api/v1/platform/polyglot/scripts/evaluate → PolyglotHandler.evaluate()
├─ POST /systems/devices                 → HardwareHandler.register()
├─ GET  /systems/devices/{nodeId}        → HardwareHandler.get()
├─ PUT  /systems/devices/{nodeId}        → HardwareHandler.update()
├─ DELETE /systems/devices/{nodeId}      → HardwareHandler.delete()
├─ GET  /systems/devices/{nodeId}/health → HardwareHandler.getHealth()
├─ POST /systems/servers                 → ServerHandler.provision()
├─ GET  /systems/servers/{serverId}      → ServerHandler.get()
├─ PUT  /systems/servers/{serverId}      → ServerHandler.mutate()
└─ DELETE /systems/servers/{serverId}    → ServerHandler.delete()

Spring AI Layer
├─ OllamaChatClient (local LLM inference)
├─ OllamaEmbeddingClient (vector embeddings)
└─ VectorStore (semantic search)

Database Layer (R2DBC)
├─ FabricNodeRepository
├─ FabricEdgeRepository
├─ FabricEventRepository
├─ SmsDispatchRepository
└─ HardwareNodeRepository

Polyglot Runtime (GraalVM)
├─ GraalJS engine (JavaScript execution)
├─ Python engine (if available)
└─ REPL for ad-hoc code
```

### 2.3 Data Flow Diagram

```
User Actions (Vue UI)
│
├─→ Topology Canvas Interaction
│   │
│   └─→ GET /api/v1/fabric/topology
│       └─→ TopologyHandler
│           └─→ FabricNodeRepository (R2DBC)
│               └─→ PostgreSQL SELECT
│                   └─→ REST Response (JSON)
│
├─→ Event Publication
│   │
│   └─→ POST /api/v1/fabric/events
│       └─→ EventHandler
│           ├─→ Persist to DB (SmsDispatchRepository)
│           └─→ Emit to Flux stream
│               └─→ All SSE clients notified
│
├─→ AI Analysis (Background)
│   │
│   └─→ FabricAgent (Spring AI)
│       ├─→ Read event from Flux
│       ├─→ Query Ollama LLM (via ChatClient)
│       ├─→ Generate remediation suggestion
│       └─→ Broadcast to SSE stream
│
└─→ SMS Notification
    │
    └─→ POST /sms/dispatches
        └─→ SmsHandler
            ├─→ Queue in DB (R2DBC)
            ├─→ Async dispatch thread
            └─→ SMPP protocol → SMS Gateway
```

---

## FUNCTIONAL REQUIREMENTS

### 3.1 Fabric Topology Management

#### 3.1.1 Topology Retrieval

**Requirement**: GET /api/v1/fabric/topology

**Purpose**: Return current fabric topology in VueFlow-compatible format

**Request**: None (query parameters optional for filtering)

**Response** (200 OK):
```json
{
  "nodes": [
    {
      "id": "node-1",
      "label": "Router-A",
      "data": {
        "type": "NETWORK_DEVICE",
        "status": "ONLINE",
        "cpu_usage": 45.2,
        "memory_gb": 32
      },
      "position": { "x": 100, "y": 100 },
      "style": { "background": "#4ade80" }
    }
  ],
  "edges": [
    {
      "id": "edge-1-2",
      "source": "node-1",
      "target": "node-2",
      "animated": true,
      "data": { "bandwidth_mbps": 1000, "latency_ms": 2.5 }
    }
  ],
  "timestamp": "2026-05-23T12:34:56Z"
}
```

**Acceptance Criteria**:
- ✅ Response time: <200ms (P95)
- ✅ Topology updates every 10 seconds
- ✅ Supports 500+ nodes/1000+ edges
- ✅ Node positions persist in database
- ✅ CORS headers set correctly

#### 3.1.2 Node Registration

**Requirement**: POST /systems/devices

**Purpose**: Onboard physical bare-metal hardware nodes

**Request**:
```json
{
  "rackLocation": "Rack-04-Bay-B",
  "bladeArchitecture": "x86_64"
}
```

**Response** (201 Created):
```json
{
  "nodeId": "f83a21bc-12aa-419b-bc22-81bb41092da3",
  "rackLocation": "Rack-04-Bay-B",
  "bladeArchitecture": "x86_64",
  "registrationStatus": "ONLINE"
}
```

### 3.2 Event Ingestion & Streaming

#### 3.2.1 Event Publishing

**Requirement**: POST /api/v1/fabric/events

**Purpose**: Ingest control-plane events into reactive Flux stream

**Request**:
```json
{
  "eventType": "NODE_DEGRADED",
  "sourceNode": "node-1",
  "severity": "CRITICAL",
  "message": "CPU usage exceeded 80%",
  "metadata": {
    "cpu_usage_percent": 85.5,
    "threshold": 80
  }
}
```

**Response** (202 Accepted):
```json
{
  "eventId": "evt-789abc",
  "timestamp": "2026-05-23T12:34:56Z",
  "status": "QUEUED"
}
```

#### 3.2.2 Event Streaming (SSE)

**Requirement**: GET /api/v1/fabric/stream

**Purpose**: Real-time Server-Sent Events stream of fabric anomalies

**Connection**: Keep-alive (Content-Type: text/event-stream)

**Sample Events**:
```
event: fabric_event
data: {"eventId": "evt-1", "type": "NODE_UP", "sourceNode": "node-1", "timestamp": "..."}

event: fabric_event
data: {"eventId": "evt-2", "type": "NODE_DOWN", "sourceNode": "node-3", "timestamp": "..."}

event: ai_remediation
data: {"eventId": "evt-1", "suggestion": "Restart service on node-1", "confidence": 0.92}
```

**Acceptance Criteria**:
- ✅ Latency: <100ms from event publication to browser
- ✅ Connection stable for 24+ hours
- ✅ Automatic reconnection on network hiccup
- ✅ Memory usage stable (no memory leak)

### 3.3 SMS Dispatch & Notifications

#### 3.3.1 SMS Dispatch

**Requirement**: POST /sms/dispatches

**Purpose**: Send telemetry alerts/MFA tokens via SMPP gateway

**Request**:
```json
{
  "phoneNumber": "+1-555-012-3456",
  "message": "CRITICAL: Node-1 CPU at 95%. Action required.",
  "priority": "HIGH",
  "expirySeconds": 300
}
```

**Response** (202 Accepted):
```json
{
  "smsTrackingId": "7c11ba03-2cc5-4b10-88ef-bc2104992de3",
  "gatewayStatus": "QUEUED"
}
```

**Database Persistence**:
- SMS payload stored in `sms_dispatch` table
- Status tracking: QUEUED → TRANSMITTING → CARRIER_ACK
- Retry logic: exponential backoff (max 3 attempts)

#### 3.3.2 Gateway Status

**Requirement**: GET /sms/gateways/status

**Purpose**: Check SMSC transceiver health & connectivity

**Response** (200 OK):
```json
{
  "gateways": [
    {
      "gatewayId": "smpp-primary",
      "connectionStatus": "ACTIVE",
      "successRate": 0.9987,
      "avgLatencyMs": 250,
      "messagesPending": 42,
      "lastUpdate": "2026-05-23T12:34:56Z"
    }
  ]
}
```

### 3.4 Platform Function Factory

#### 3.4.1 Blueprint Generation

**Requirement**: POST /api/v1/platform/functions/blueprints

**Purpose**: Generate AWS Lambda or Azure Functions code wrappers

**Request**:
```json
{
  "functionName": "process-fabric-event",
  "sourceCode": "function(event) { return event.data.toUpperCase(); }",
  "language": "javascript",
  "targetPlatform": "aws_lambda"
}
```

**Response** (200 OK):
```json
{
  "blueprintId": "bp-xyz789",
  "lambdaHandler": "index.handler",
  "sourceFiles": [
    {
      "filename": "index.js",
      "content": "exports.handler = async (event) => { ... };"
    }
  ],
  "deploymentHints": {
    "runtime": "nodejs18.x",
    "memory_mb": 512,
    "timeout_seconds": 60
  }
}
```

#### 3.4.2 Polyglot Script Execution

**Requirement**: POST /api/v1/platform/polyglot/scripts/evaluate

**Purpose**: Execute constrained GraalVM polyglot scripts

**Request**:
```json
{
  "language": "javascript",
  "code": "function sum(a, b) { return a + b; }; sum(5, 3)",
  "bindings": {
    "nodeId": "node-1",
    "threshold": 100
  }
}
```

**Response** (200 OK):
```json
{
  "result": 8,
  "executionTimeMs": 15,
  "language": "javascript",
  "errors": []
}
```

**Security Constraints** (GraalVM sandbox):
- ❌ No filesystem access
- ❌ No network I/O
- ❌ No process creation
- ❌ No environment variable access
- ✅ Computation only (CPU-bound)

### 3.5 Hardware Management

#### 3.5.1 Hardware Node Registration

**Requirement**: POST /systems/devices

**Already covered** in section 3.1.2

#### 3.5.2 Node Health Metrics

**Requirement**: GET /systems/devices/{nodeId}/health

**Purpose**: Fetch CPU, memory, temperature telemetry

**Response** (200 OK):
```json
{
  "nodeId": "f83a21bc-12aa-419b-bc22-81bb41092da3",
  "cpuUtilizationPercent": 42.5,
  "ramFreeBytes": 549755813888,
  "dieTemperatureCelsius": 51,
  "timestamp": "2026-05-23T12:34:56Z"
}
```

### 3.6 Enterprise Server Provisioning

#### 3.6.1 Server Provisioning

**Requirement**: POST /systems/servers

**Purpose**: Deploy multi-role infrastructure servers

**Request**:
```json
{
  "serverName": "prod-zone-edge-hypervisor",
  "parentNodeId": "4fa218ce-bc31-4122-aa11-82ff3b1029da",
  "deploymentFramework": "VIRTUAL_MACHINE",
  "executionScope": "REAL",
  "targetRoleClass": "DATABASE_SERVER",
  "dbRoleConfig": {
    "databaseEngine": "POSTGRESQL",
    "localListeningPort": 5432
  }
}
```

**Response** (201 Created):
```json
{
  "serverId": "0c9d74b2-29ff-411a-b331-cb98a21104e1",
  "allocationMetadata": {
    "assignedClusterIp": "10.194.45.108",
    "bootstrapTimestamp": "2026-05-22T20:15:00Z"
  },
  "runtimeState": "INITIALIZING"
}
```

---

## NON-FUNCTIONAL REQUIREMENTS

### 4.1 Performance & Scalability

| Requirement | Target | Measurement |
|-------------|--------|------------|
| **Startup Time** | <5s | Cold start (JVM mode) |
| **API Latency (P95)** | <200ms | REST endpoint response |
| **Event Streaming Latency** | <100ms | Publication → SSE delivery |
| **Database Throughput** | 1000 req/s | R2DBC connection pool |
| **Concurrent Connections** | 10000+ | WebSocket/SSE clients |
| **Memory (JVM)** | 512MB | Container resource limit |
| **CPU** | 1-2 cores | During normal operation |

### 4.2 Reliability & Availability

| Requirement | Target | Notes |
|-------------|--------|-------|
| **Uptime** | 99.9% | SLA for production |
| **MTTR** | <2 minutes | Mean time to recover |
| **Data Durability** | 100% | PostgreSQL replication |
| **Event Ordering** | Causal | Within same source |
| **Fault Tolerance** | Graceful degradation | Ollama unavailable = basic ops |

### 4.3 Data Integrity & Persistence

**Requirement**: All data persisted to PostgreSQL

- **Database Schema**: Managed by Flyway migrations
- **R2DBC Driver**: For non-blocking database access
- **Connection Pool**: 20 connections (tunable)
- **Soft Deletes**: No hard deletes; mark as `deleted_at`
- **Audit Trail**: All mutations logged with timestamp/user

**Schema Versioning**:
```
backend/src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_sms_dispatch_table.sql
├── V3__add_fabric_events_audit.sql
└── V4__create_indexes.sql
```

### 4.4 Security

| Aspect | Requirement |
|--------|------------|
| **CORS** | Configurable via `FABRIC_CORS_ALLOWED_ORIGINS` |
| **Polyglot Sandbox** | GraalVM policies block dangerous operations |
| **SMPP Credentials** | Environment variables only (no hardcoding) |
| **Secrets Management** | Use Render managed environment variables |
| **HTTPS** | Enforced in production |
| **Input Validation** | All inputs validated (OpenAPI spec) |

### 4.5 Observability

- **Logging**: Structured JSON logs (SLF4J)
- **Metrics**: Spring Boot Actuator (/actuator/metrics)
- **Health Checks**: /actuator/health (liveness probe)
- **Distributed Tracing**: Ready for OpenTelemetry integration
- **Error Tracking**: Sentry or similar (optional)

---

## API CONTRACT (OPENAPI 3.1.0)

### 5.1 Full Endpoint Reference

```yaml
openapi: 3.1.0
info:
  title: Fabric Engine Control Plane
  version: 2.1.0
  description: Network fabric orchestration API

servers:
  - url: http://localhost:8080/api/v1
    description: Local Development
  - url: https://api.fabric.production.com/api/v1
    description: Production

paths:
  /fabric/topology:
    get:
      tags:
        - Reactive Fabric
      summary: Get fabric topology
      operationId: getFabricTopology
      responses:
        '200':
          description: Fabric nodes and edges
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FabricTopology'

  /fabric/events:
    post:
      tags:
        - Reactive Fabric
      summary: Publish fabric event
      operationId: publishFabricEvent
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FabricEvent'
      responses:
        '202':
          description: Event accepted
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EventPublishResponse'

  /fabric/stream:
    get:
      tags:
        - Reactive Fabric
      summary: Stream real-time events (SSE)
      operationId: streamFabricEvents
      responses:
        '200':
          description: Server-sent event stream
          content:
            text/event-stream:
              schema:
                $ref: '#/components/schemas/FabricEvent'

components:
  schemas:
    FabricTopology:
      type: object
      required:
        - nodes
        - edges
        - timestamp
      properties:
        nodes:
          type: array
          items:
            $ref: '#/components/schemas/Node'
        edges:
          type: array
          items:
            $ref: '#/components/schemas/Edge'
        timestamp:
          type: string
          format: date-time

    Node:
      type: object
      required:
        - id
        - label
      properties:
        id:
          type: string
        label:
          type: string
        data:
          type: object
        position:
          $ref: '#/components/schemas/Position'

    Edge:
      type: object
      required:
        - id
        - source
        - target
      properties:
        id:
          type: string
        source:
          type: string
        target:
          type: string
        data:
          type: object

    FabricEvent:
      type: object
      required:
        - eventType
        - sourceNode
        - severity
      properties:
        eventId:
          type: string
          format: uuid
        eventType:
          type: string
          enum: [NODE_UP, NODE_DOWN, NODE_DEGRADED, LINK_DOWN, ANOMALY]
        sourceNode:
          type: string
        severity:
          type: string
          enum: [INFO, WARNING, CRITICAL]
        message:
          type: string
        metadata:
          type: object
```

### 5.2 Complete Endpoint Mapping

| HTTP Method | Path | Handler | Response |
|------------|------|---------|----------|
| GET | /api/v1/fabric/topology | getFabricTopology() | FabricTopology |
| POST | /api/v1/fabric/events | publishFabricEvent() | EventPublishResponse |
| GET | /api/v1/fabric/stream | streamFabricEvents() | SSE Stream |
| POST | /sms/dispatches | dispatchSmsNotification() | SmsDispatchResponse |
| GET | /sms/gateways/status | getSmsGatewayStatus() | SmsGatewayMetrics |
| POST | /api/v1/platform/functions/blueprints | createPolyglotFunctionBlueprint() | PolyglotFunctionBlueprintResponse |
| POST | /api/v1/platform/polyglot/scripts/evaluate | evaluatePolyglotScript() | PolyglotScriptResponse |
| POST | /systems/devices | registerHardwareNode() | HardwareNodeProfile |
| GET | /systems/devices/{nodeId} | getHardwareNode() | HardwareNodeProfile |
| PUT | /systems/devices/{nodeId} | updateHardwareNode() | HardwareNodeProfile |
| DELETE | /systems/devices/{nodeId} | deleteHardwareNode() | 204 No Content |
| GET | /systems/devices/{nodeId}/health | getHardwareHealth() | SystemHealthDiagnostics |
| POST | /systems/servers | provisionEnterpriseServer() | ServerProvisionStatus |
| GET | /systems/servers/{serverId} | getServerDetails() | ServerProvisionStatus |
| PUT | /systems/servers/{serverId} | mutateServerConfig() | ServerProvisionStatus |
| DELETE | /systems/servers/{serverId} | deleteServer() | 204 No Content |

---

## TECHNOLOGY STACK

### 6.1 Backend Dependencies

```gradle
// build.gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-webflux:3.5.14'
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc:3.5.14'
    implementation 'org.springframework.boot:spring-boot-starter-validation:3.5.14'
    implementation 'org.springframework.boot:spring-boot-starter-actuator:3.5.14'

    // Spring AI
    implementation platform('org.springframework.ai:spring-ai-bom:1.1.6')
    implementation 'org.springframework.ai:spring-ai-starter-model-ollama:1.1.6'
    implementation 'org.springframework.ai:spring-ai-vector-store:1.1.6'

    // GraalVM Polyglot
    implementation 'org.graalvm.polyglot:polyglot:25.0.3'
    implementation 'org.graalvm.polyglot:js:25.0.3'

    // Database
    implementation 'org.flywaydb:flyway-core:9.22.3'
    runtimeOnly 'org.postgresql:r2dbc-postgresql:1.0.5'
    runtimeOnly 'org.postgresql:postgresql:42.7.3'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test:3.5.14'
    testImplementation 'io.projectreactor:reactor-test:2024.04.3'
}
```

### 6.2 Frontend Dependencies

```json
{
  "dependencies": {
    "vue": "^3.5.12",
    "@vue-flow/core": "^1.41.0",
    "@vue-flow/background": "^1.3.2",
    "@vue-flow/controls": "^1.1.2",
    "pinia": "^2.2.6"
  },
  "devDependencies": {
    "vite": "^5.4.10",
    "@vitejs/plugin-vue": "^5.1.4",
    "typescript": "^5.4.0"
  }
}
```

### 6.3 Database Versions

- **PostgreSQL**: 16.x (Alpine image for Docker)
- **Ollama**: Latest (auto-pulled llama3.2:3b, nomic-embed-text)

---

## CONFIGURATION & ENVIRONMENT

### 7.1 Backend Configuration (application.yml)

```yaml
spring:
  application:
    name: fabric-control-plane-v2
    version: 2.0.0

  webflux:
    base-path: /api/v1
    cors:
      allowed-origins: ${FABRIC_CORS_ALLOWED_ORIGINS:http://localhost:5173}
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: '*'

  r2dbc:
    url: ${SPRING_R2DBC_URL:r2dbc:postgresql://localhost:5432/fabric}
    username: ${SPRING_R2DBC_USERNAME:fabric}
    password: ${SPRING_R2DBC_PASSWORD:fabric}
    pool:
      max-size: 20
      initial-size: 5
      min-idle: 2

  flyway:
    url: ${SPRING_FLYWAY_URL:jdbc:postgresql://localhost:5432/fabric}
    user: ${SPRING_FLYWAY_USER:fabric}
    password: ${SPRING_FLYWAY_PASSWORD:fabric}
    locations: classpath:db/migration

fabric:
  openapi-path: ${FABRIC_OPENAPI_PATH:/app/openapi.yml}
  cors:
    allowed-origins: ${FABRIC_CORS_ALLOWED_ORIGINS:http://localhost:5173}
  telemetry:
    demo-events-enabled: ${FABRIC_DEMO_EVENTS_ENABLED:true}
    event-generation-interval: 8000

ollama:
  base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
  chat-model: ${OLLAMA_CHAT_MODEL:llama3.2:3b}
  embedding-model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

logging:
  level:
    root: INFO
    com.acme.fabric: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### 7.2 Frontend Environment (.env, .env.production)

```bash
# .env.development
VITE_API_BASE=http://localhost:8080
VITE_ENVIRONMENT=development

# .env.production
VITE_API_BASE=https://api.fabric.production.com
VITE_ENVIRONMENT=production
VITE_ENABLE_ANALYTICS=true
```

### 7.3 Docker Compose Environment

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: fabric
      POSTGRES_USER: fabric
      POSTGRES_PASSWORD: fabric
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U fabric -d fabric"]
      interval: 10s
      timeout: 5s
      retries: 12

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    environment:
      OLLAMA_MODELS_DIR: /root/.ollama/models
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 10s
      timeout: 5s
      retries: 12

  backend:
    build:
      context: .
      dockerfile: backend/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_R2DBC_URL: r2dbc:postgresql://postgres:5432/fabric
      SPRING_R2DBC_USERNAME: fabric
      SPRING_R2DBC_PASSWORD: fabric
      SPRING_FLYWAY_URL: jdbc:postgresql://postgres:5432/fabric
      SPRING_FLYWAY_USER: fabric
      SPRING_FLYWAY_PASSWORD: fabric
      OLLAMA_BASE_URL: http://ollama:11434
      FABRIC_CORS_ALLOWED_ORIGINS: http://localhost:5173
    depends_on:
      postgres:
        condition: service_healthy
      ollama:
        condition: service_healthy

  frontend:
    build:
      context: frontend
      dockerfile: Dockerfile
      args:
        VITE_API_BASE: http://localhost:8080
    ports:
      - "5173:80"
    depends_on:
      - backend

volumes:
  postgres-data:
  ollama-data:
```

---

## DATA PERSISTENCE & SCHEMA

### 8.1 PostgreSQL Schema

```sql
-- Core fabric topology
CREATE TABLE fabric_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    node_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'INITIALIZING',
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE fabric_edges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL REFERENCES fabric_nodes(id),
    target_id UUID NOT NULL REFERENCES fabric_nodes(id),
    bandwidth_mbps NUMERIC,
    latency_ms NUMERIC,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Events
CREATE TABLE fabric_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    source_node_id UUID REFERENCES fabric_nodes(id),
    severity VARCHAR(20) NOT NULL,
    message TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SMS Dispatch
CREATE TABLE sms_dispatch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(50) DEFAULT 'QUEUED',
    tracking_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    error_message TEXT
);
```

### 8.2 Flyway Migration Management

```
Location: backend/src/main/resources/db/migration/

Files:
├── V1__initial_schema.sql                    (schema creation)
├── V2__add_sms_dispatch_table.sql            (SMS support)
├── V3__add_fabric_events_audit.sql           (event audit trail)
├── V4__create_indexes.sql                    (performance optimization)
└── V5__add_monitoring_tables.sql             (metrics/health)
```

**Flyway Behavior**:
- **Validation**: Checksums verified on each startup
- **Baseline**: `baseline-on-migrate: true` (if needed)
- **Migration Order**: Strictly versioned (V1, V2, V3, ...)
- **Rollback**: No rollback support (use database restore)

---

## DEPLOYMENT ARCHITECTURE

### 9.1 Local Development (Docker Compose)

**Command**: `docker compose up --build`

**Services**:
- **PostgreSQL**: 5432 (Fabric database)
- **Ollama**: 11434 (LLM inference)
- **Backend**: 8080 (Spring WebFlux)
- **Frontend**: 5173 (Vue Vite dev server)

**Health Checks**:
- PostgreSQL: `pg_isready` every 10s (retry 12x)
- Ollama: `ollama list` every 10s (retry 12x)
- Backend: Depends on both services healthy

### 9.2 Production Deployment (Render)

**Configuration**: render.yaml (in repo root)

```yaml
services:
  - type: web
    name: fabric-backend
    env: docker
    dockerfile: backend/Dockerfile
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SPRING_R2DBC_URL
        fromDatabase:
          name: fabric-db
          property: r2dbc_url
      - key: OLLAMA_BASE_URL
        value: ${OLLAMA_ENDPOINT}
    healthCheckPath: /actuator/health
    healthCheckInterval: 10

  - type: web
    name: fabric-frontend
    staticPublishPath: dist
    buildCommand: npm run build
    envVars:
      - key: VITE_API_BASE
        value: https://fabric-backend.onrender.com

  - type: pserv
    name: fabric-db
    plan: standard
    ipAllowList: []
    region: oregon
```

### 9.3 Production Checklist

- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure managed PostgreSQL (Render, RDS, or similar)
- [ ] Set `OLLAMA_BASE_URL` to external Ollama endpoint
- [ ] Provide SMPP credentials for SMS gateway
- [ ] Configure `FABRIC_CORS_ALLOWED_ORIGINS` to frontend URL
- [ ] Disable demo telemetry: `FABRIC_DEMO_EVENTS_ENABLED=false`
- [ ] Enable health actuator endpoint
- [ ] SSL/HTTPS enforced
- [ ] Database backups configured (daily)
- [ ] Monitoring/alerting in place

---

## TESTING & QUALITY ASSURANCE

### 10.1 Test Strategy

| Test Type | Framework | Coverage | Frequency |
|-----------|-----------|----------|-----------|
| **Unit** | JUnit 5 + Mockito | >80% services | On commit |
| **Integration** | Spring WebFlux Test | REST endpoints | On PR |
| **R2DBC** | Testcontainers | Database layer | On PR |
| **E2E** | Cypress | User workflows | Nightly |
| **Load** | JMeter/Gatling | 1000 concurrent | Weekly |
| **Security** | OWASP ZAP | Vulnerabilities | Monthly |

### 10.2 Sample Test Code

```java
@SpringBootTest
@AutoConfigureWebTestClient
public class FabricTopologyTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testGetTopology() {
        webTestClient.get()
            .uri("/api/v1/fabric/topology")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.nodes").isArray()
            .jsonPath("$.nodes.length()").isGreaterThan(0);
    }

    @Test
    public void testPublishEvent() {
        FabricEvent event = new FabricEvent(
            "evt-1",
            "NODE_DEGRADED",
            "node-1",
            "CRITICAL",
            "CPU exceeds threshold"
        );

        webTestClient.post()
            .uri("/api/v1/fabric/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(event)
            .exchange()
            .expectStatus().isAccepted();
    }
}
```

---

## KNOWN CONSTRAINTS

### 11.1 Limitations

| Constraint | Impact | Workaround |
|-----------|--------|-----------|
| **Local Ollama model (~4GB)** | First startup slow | Pre-seed container image |
| **SSE no request body** | Complex filters via headers | Use POST + polling |
| **GraalVM sandbox** | Cannot write files from functions | Use database for state |
| **Single PostgreSQL instance** | No HA in dev env | Use managed DB for prod |
| **Docker container 512MB limit** | JVM memory pressure | Use native image for containers |

### 11.2 Assumptions

- Network connectivity between components (Docker bridge or host network)
- PostgreSQL accepting connections on port 5432
- Ollama server accepting requests on port 11434
- Frontend browser supports ES2020+
- No strict SLA <1s response time requirement

---

## ACCEPTANCE CRITERIA

**The system is production-ready when:**

- ✅ All 20+ API endpoints operational & tested
- ✅ SSE event streaming handles 1000+ events/sec
- ✅ SMS dispatch processes messages within 2s (P95)
- ✅ Topology visualization renders 500+ nodes smoothly
- ✅ GraalVM functions execute within 100ms
- ✅ Database migrations execute idempotently
- ✅ Docker Compose environment starts within 60s
- ✅ Render deployment health checks passing
- ✅ Security audit completed (OWASP Top 10 checked)
- ✅ Documentation complete & current
- ✅ Load testing validates 5000 concurrent connections
- ✅ All tests passing (>80% coverage)
- ✅ Zero data loss in failover scenario
- ✅ MTTR <2 minutes for common failure modes

---

## GLOSSARY

| Term | Definition |
|------|-----------|
| **Fabric** | Network topology mesh (SDN abstraction) |
| **Control Plane** | Management & orchestration layer |
| **WebFlux** | Spring's reactive web framework (Project Reactor) |
| **R2DBC** | Reactive Database Connectivity (async JDBC) |
| **VueFlow** | Graph visualization library for Vue 3 |
| **Pinia** | State management for Vue 3 |
| **SSE** | Server-Sent Events (unidirectional streaming) |
| **Flux** | Project Reactor's streaming API |
| **Ollama** | Local LLM inference engine |
| **Spring AI** | Spring Framework AI/ML integration |
| **GraalVM** | Polyglot JVM with native compilation |
| **SMPP** | Short Message Peer-to-Peer protocol |
| **OpenAPI** | API specification format (formerly Swagger) |
| **Flyway** | Database schema migration tool |
| **MTTR** | Mean Time To Recovery |

---

## DOCUMENT INFORMATION

**Author:** 25-Year Senior Prompt Engineer  
**Repository:** https://github.com/ajaychaurasia25921/fabric-engine-master-control-plane-v2  
**Branch:** main  
**Technology Stack:**  
- Backend: Spring Boot 3.5.14 + WebFlux  
- Frontend: Vue 3 + Vite  
- Database: PostgreSQL 16 + R2DBC  
- AI: Ollama + Spring AI  
- Polyglot: GraalVM 25.0.3  

**Created:** May 23, 2026  
**Last Updated:** May 23, 2026  
**Status:** Finalized  

**Related Documents:**
- REQUIREMENTS_REWRITE_BRANCH.md (Quarkus + React migration plan)
- README.md (Quick start guide)
- openapi.yml (Full API specification)

**Implementation Owner:** Development Team  
**Estimated Effort:** 8 weeks (for main branch maintenance)  
**SLA Target:** 99.9% availability  

---

**END OF REQUIREMENTS DOCUMENT - MAIN BRANCH**

