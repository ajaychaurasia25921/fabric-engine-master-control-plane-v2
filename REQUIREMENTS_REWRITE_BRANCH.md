# FABRIC ENGINE MASTER CONTROL PLANE v2
## Requirements Document - REWRITE-QUARKUS-REACT-RPC BRANCH
### Quarkus + React + RPC Architectural Transformation

**Document Version:** 1.0  
**Date:** May 23, 2026  
**Status:** Requirements Finalized  
**Branch:** rewrite-quarkus-react-rpc  
**Repository:** https://github.com/ajaychaurasia25921/fabric-engine-master-control-plane-v2

---

## TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [Strategic Rationale](#strategic-rationale)
3. [Architectural Transformation](#architectural-transformation)
4. [Functional Requirements (Quarkus Backend)](#functional-requirements-quarkus-backend)
5. [Frontend Requirements (React)](#frontend-requirements-react)
6. [RPC Communication Layer](#rpc-communication-layer)
7. [Technology Stack (Rewrite)](#technology-stack-rewrite)
8. [Configuration & Environment](#configuration--environment)
9. [Data Persistence & Schema](#data-persistence--schema)
10. [Performance & Scalability Goals](#performance--scalability-goals)
11. [Migration Strategy](#migration-strategy)
12. [Testing & Quality Assurance](#testing--quality-assurance)
13. [Known Constraints & Risks](#known-constraints--risks)
14. [Implementation Timeline](#implementation-timeline)
15. [Glossary](#glossary)

---

## EXECUTIVE SUMMARY

The **rewrite-quarkus-react-rpc branch** represents a strategic architectural modernization of the Fabric Engine from **Spring WebFlux + Vue 3** to a **cloud-native, faster-startup, lower-memory footprint** stack:

### Vision
- **Backend**: Spring WebFlux → **Quarkus** (native compilation, 50ms startup, 20MB memory)
- **Frontend**: Vue 3 → **React** (hooks-based, modern tooling, larger ecosystem)
- **Communication**: REST/SSE → **RPC** (gRPC or JSON-RPC for polyglot efficiency)

### Strategic Benefits
1. **Startup Time**: 30s → 50ms (60x faster) for serverless workloads
2. **Memory**: 512MB JVM → 50-100MB GraalVM native (80% reduction)
3. **Developer Experience**: Quarkus hot reload, React dev tools ecosystem
4. **Interoperability**: gRPC enables polyglot microservices (Python, Go, Node.js)
5. **API Backwards Compatibility**: OpenAPI 3.1.0 spec maintained (same contract)
6. **Production Readiness**: GraalVM native binary deployable on Alpine Linux

### Target Outcomes
- ✅ 100% functional parity with main branch
- ✅ Improved cold-start latency for Lambda/Azure Functions
- ✅ Reduced cloud infrastructure costs (smaller containers)
- ✅ Enhanced developer productivity (React ecosystem, Quarkus extensions)
- ✅ Multi-language service composition (gRPC interop)

---

## STRATEGIC RATIONALE

### Why Quarkus?

| Criterion | Spring Boot | Quarkus | Winner |
|-----------|------------|---------|--------|
| **Startup Time** | 3-5s | 50-100ms | Quarkus ✅ |
| **Memory (RSS)** | 400-600MB | 50-100MB | Quarkus ✅ |
| **Native Compilation** | GraalVM plugin | First-class support | Quarkus ✅ |
| **Cloud-Native Features** | Via Spring Cloud | Built-in | Quarkus ✅ |
| **Maturity** | Production-proven | Enterprise-ready | Spring ✅ |
| **Learning Curve** | Familiar for Spring devs | Similar concepts | Tie |
| **Extension Ecosystem** | Massive | Growing rapidly | Spring ✅ |
| **Container Size** | 500MB+| 50-100MB | Quarkus ✅ |

**Decision**: Quarkus for cloud-native workloads, API gateways, serverless.

### Why React?

| Criterion | Vue 3 | React | Winner |
|-----------|-------|-------|--------|
| **Community Size** | 200K+ devs | 5M+ devs | React ✅ |
| **Ecosystem** | Growing | Mature | React ✅ |
| **Hiring Pool** | Smaller | Larger | React ✅ |
| **Performance** | Excellent | Excellent | Tie |
| **State Management** | Pinia | Redux/Zustand/Jotai | React ✅ |
| **DevTools** | Good | Excellent | React ✅ |
| **Learning Curve** | Easier | Steeper | Vue ✅ |
| **Component Libraries** | MUI Vue | Material-UI, Chakra | React ✅ |

**Decision**: React for better hiring market fit, ecosystem maturity.

### Why gRPC/RPC?

| Criterion | REST | gRPC | JSON-RPC | Winner |
|-----------|------|------|----------|--------|
| **Serialization** | JSON (text) | Protobuf (binary) | JSON (text) | gRPC ✅ |
| **Performance** | ~100ms | ~5ms | ~50ms | gRPC ✅ |
| **Polyglot Support** | Universal | Excellent | Limited | gRPC ✅ |
| **Streaming** | SSE (text) | Native (binary) | Polling | gRPC ✅ |
| **Browser Support** | Direct | gRPC-Web | Direct | Tie |
| **Debugging** | Easy | Requires tools | Easy | REST/JSON-RPC ✅ |
| **Learning Curve** | Minimal | Moderate | Minimal | REST/JSON-RPC ✅ |

**Decision**: Hybrid approach:
- **REST/HTTP** for browser (Vue/React frontend) via gRPC-Web
- **gRPC** for service-to-service (Ollama, databases, polyglot functions)
- **JSON-RPC 2.0** as fallback for simple synchronous calls

---

## ARCHITECTURAL TRANSFORMATION

### 3.1 Migration Timeline & Phases

```
Phase 1: Foundation (Weeks 1-4)
├─ Set up Quarkus project structure
├─ Port Spring WebFlux routes to Quarkus REST (JAX-RS)
├─ Migrate R2DBC to Quarkus reactive R2DBC
├─ Configure Flyway migrations
└─ Deploy to Docker (standard JVM mode)

Phase 2: gRPC Integration (Weeks 5-8)
├─ Define .proto files for fabric services
├─ Generate gRPC stubs (Java server, TypeScript client)
├─ Implement gRPC service handlers
├─ Add gRPC-Web layer for browser clients
└─ Benchmarking: REST vs gRPC latency

Phase 3: React Frontend (Weeks 9-12)
├─ Initialize React app (Vite + TypeScript)
├─ Migrate Vue components → React components
├─ Set up state management (Zustand or Redux Toolkit)
├─ Implement VueFlow equivalent (React Flow)
├─ Integrate gRPC-Web clients
└─ QA & performance optimization

Phase 4: Native Compilation (Weeks 13-16)
├─ Configure GraalVM native-image
├─ Build native binary (quarkus build --native)
├─ Performance testing & profiling
├─ Docker image optimization (Alpine base)
├─ Cloud deployment (Render, AWS Lambda)
└─ Load testing (1000 concurrent users)

Phase 5: Verification & Release (Weeks 17-20)
├─ OpenAPI spec regeneration
├─ Integration test suite
├─ Documentation update
├─ Production cutover planning
└─ GA release (feature parity verified)
```

### 3.2 Backward Compatibility Strategy

**Maintaining OpenAPI 3.1.0 Contract:**

```
┌────────────────────────────────────────────┐
│        OpenAPI 3.1.0 Specification        │
│   (REST endpoints - externally facing)     │
└────────────────────────────────────────────┘
               ▲ (same interface)
               │
┌──────────────┴──────────────────────────────┐
│     Quarkus REST Layer (JAX-RS)            │
│  (adapter to gRPC-backed services)         │
└──────────────┬──────────────────────────────┘
               │
┌──────────────┴──────────────────────────────┐
│     gRPC Service Layer                      │
│  (internal communication, high performance) │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
  PostgreSQL        Ollama
  (R2DBC)         (local LLM)
```

**API Endpoint Mapping (Example):**

```
REST (OpenAPI)
  POST /api/v1/fabric/events
    │
    └─→ Quarkus REST handler (FastAPI)
         │
         └─→ gRPC FabricService.PublishEvent()
              │
              └─→ React buffer + PostgreSQL persist
```

### 3.3 Database & Schema Continuity

**PostgreSQL Schema**: Unchanged  
**Migration Tool**: Flyway (same as main branch)  
**R2DBC Driver**: Quarkus reactive R2DBC (drop-in replacement)

**Validation Query:**
```java
@DataSource("custom")
@ConfigureDataSource
r2dbc:postgresql://localhost:5432/fabric
```

---

## FUNCTIONAL REQUIREMENTS (QUARKUS BACKEND)

### 4.1 Core Service Handlers (Quarkus REST)

#### 4.1.1 Fabric Topology Service

```java
@Path("/api/v1/fabric")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FabricTopologyResource {

    @Inject
    FabricServiceGrpc.FabricServiceBlockingStub fabricService;

    @GET
    @Path("/topology")
    @Operation(summary = "Get fabric topology")
    public Uni<FabricTopologyResponse> getTopology() {
        // REST → gRPC bridge
        return Uni.createFrom().item(() -> {
            GetTopologyRequest request = GetTopologyRequest.newBuilder().build();
            return fabricService.getTopology(request);
        }).map(FabricTopologyMapper::toRest);
    }

    @POST
    @Path("/events")
    @Operation(summary = "Publish fabric event")
    public Uni<Response> publishEvent(FabricEvent event) {
        return Uni.createFrom().item(() -> {
            PublishEventRequest request = FabricEventMapper.toGrpc(event);
            fabricService.publishEvent(request);
            return Response.accepted().build();
        });
    }
}
```

#### 4.1.2 Event Streaming (Server-Sent Events)

```java
@GET
@Path("/stream")
@Produces("text/event-stream")
@Operation(summary = "Stream fabric events")
public Multi<ServerSentEvent<FabricEventDto>> streamEvents() {
    return fabricService.streamEvents(StreamRequest.newBuilder().build())
        .onItem().transform(event -> ServerSentEvent.of(
            FabricEventMapper.toDto(event)
        ));
}
```

#### 4.1.3 SMS Dispatch Service

```java
@Path("/sms")
public class SmsDispatchResource {

    @Inject
    SmsServiceGrpc.SmsServiceBlockingStub smsService;

    @POST
    @Path("/dispatches")
    public Uni<SmsDispatchResponse> dispatchSms(SmsDispatchPayload payload) {
        return Uni.createFrom().item(() -> {
            DispatchRequest request = SmsPayloadMapper.toGrpc(payload);
            return smsService.dispatch(request);
        }).map(SmsResponseMapper::toRest);
    }

    @GET
    @Path("/gateways/status")
    public Uni<SmsGatewayMetrics> getGatewayStatus() {
        return Uni.createFrom().item(() ->
            smsService.getGatewayStatus(Empty.getDefaultInstance())
        ).map(SmsStatusMapper::toRest);
    }
}
```

### 4.2 gRPC Service Definitions (Proto)

```protobuf
// fabric.proto
syntax = "proto3";

package com.acme.fabric.api;

service FabricService {
    rpc GetTopology(GetTopologyRequest) returns (FabricTopology);
    rpc PublishEvent(PublishEventRequest) returns (PublishEventResponse);
    rpc StreamEvents(StreamRequest) returns (stream FabricEvent);
}

message GetTopologyRequest {}

message FabricTopology {
    repeated Node nodes = 1;
    repeated Edge edges = 2;
    string timestamp = 3;
}

message Node {
    string id = 1;
    string label = 2;
    NodeData data = 3;
    Position position = 4;
}

message Edge {
    string id = 1;
    string source = 2;
    string target = 3;
    EdgeData data = 4;
}

message PublishEventRequest {
    FabricEvent event = 1;
}

message PublishEventResponse {
    bool success = 1;
    string event_id = 2;
}

message FabricEvent {
    string event_id = 1;
    string timestamp = 2;
    string source_node = 3;
    string event_type = 4;
    string severity = 5;
    string message = 6;
    map<string, string> metadata = 7;
}
```

### 4.3 Polyglot Function Execution (Quarkus)

```java
@Path("/api/v1/platform")
public class PolyglotFunctionResource {

    @Inject
    PolyglotServiceGrpc.PolyglotServiceBlockingStub polyglotService;

    @POST
    @Path("/polyglot/scripts/evaluate")
    public Uni<PolyglotScriptResponse> evaluateScript(PolyglotScriptRequest req) {
        return Uni.createFrom().item(() ->
            polyglotService.evaluate(PolyglotScriptMapper.toGrpc(req))
        ).map(PolyglotResponseMapper::toRest);
    }
}
```

---

## FRONTEND REQUIREMENTS (REACT)

### 5.1 React Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── FabricTopology.tsx       (VueFlow → React Flow)
│   │   ├── TopologyCanvas.tsx       (graph rendering)
│   │   ├── EventPanel.tsx           (event viewer)
│   │   ├── SmsDispatch.tsx          (SMS panel)
│   │   ├── AiGuide.tsx              (AI chat component)
│   │   └── Layout.tsx               (main layout)
│   ├── hooks/
│   │   ├── useFabricTopology.ts     (data fetching)
│   │   ├── useFabricEvents.ts       (SSE streaming)
│   │   ├── useGrpcClient.ts         (gRPC client)
│   │   └── useLocalStorage.ts       (persistence)
│   ├── store/
│   │   ├── fabricStore.ts           (Zustand state)
│   │   ├── uiStore.ts               (UI state)
│   │   └── actions.ts               (async thunks)
│   ├── services/
│   │   ├── fabricService.ts         (REST/gRPC facade)
│   │   ├── smsService.ts            (SMS operations)
│   │   └── grpcClient.ts            (gRPC-Web client)
│   ├── pages/
│   │   ├── Dashboard.tsx            (main page)
│   │   ├── Settings.tsx             (configuration)
│   │   └── NotFound.tsx             (404)
│   ├── utils/
│   │   ├── api.ts                   (API constants)
│   │   ├── formatters.ts            (data formatting)
│   │   └── validators.ts            (input validation)
│   ├── App.tsx                      (root component)
│   ├── index.css                    (tailwind/styling)
│   └── main.tsx                     (entry point)
├── public/
├── package.json
├── tsconfig.json
├── vite.config.ts                   (Vite config)
├── vitest.config.ts                 (unit test config)
└── Dockerfile
```

### 5.2 Core React Components

#### 5.2.1 FabricTopology Component (React Flow)

```typescript
import React, { useCallback } from 'react';
import { ReactFlow, Background, Controls, MiniMap } from 'reactflow';
import 'reactflow/dist/style.css';
import useFabricTopology from '../hooks/useFabricTopology';

interface FabricTopologyProps {
  nodeFilter?: string;
}

export const FabricTopology: React.FC<FabricTopologyProps> = ({ nodeFilter }) => {
  const { nodes, edges, isLoading, error } = useFabricTopology(nodeFilter);

  const onNodesChange = useCallback((changes: any) => {
    // handle node position changes
  }, []);

  const onEdgesChange = useCallback((changes: any) => {
    // handle edge changes
  }, []);

  if (isLoading) return <div>Loading topology...</div>;
  if (error) return <div className="error">{error.message}</div>;

  return (
    <div className="fabric-topology">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
      >
        <Background />
        <Controls />
        <MiniMap />
      </ReactFlow>
    </div>
  );
};
```

#### 5.2.2 Event Streaming Hook (gRPC-Web)

```typescript
import { useEffect, useState, useRef } from 'react';
import { fabricClient } from '../services/grpcClient';
import { StreamRequest } from '../generated/fabric_pb';

export function useFabricEvents() {
  const [events, setEvents] = useState([]);
  const [error, setError] = useState<Error | null>(null);
  const streamRef = useRef(null);

  useEffect(() => {
    const request = new StreamRequest();
    streamRef.current = fabricClient.streamEvents(request, {});

    fabricClient.streamEvents(request, {}).on('data', (event) => {
      setEvents(prev => [...prev.slice(-99), event]); // keep last 100
    });

    fabricClient.streamEvents(request, {}).on('error', (err) => {
      setError(err);
    });

    return () => {
      streamRef.current?.cancel();
    };
  }, []);

  return { events, error };
}
```

#### 5.2.3 State Management (Zustand)

```typescript
import create from 'zustand';

interface FabricStore {
  topology: any;
  events: any[];
  selectedNode: string | null;
  setTopology: (topology: any) => void;
  addEvent: (event: any) => void;
  selectNode: (nodeId: string) => void;
  clearEvents: () => void;
}

export const useFabricStore = create<FabricStore>((set) => ({
  topology: null,
  events: [],
  selectedNode: null,

  setTopology: (topology) => set({ topology }),

  addEvent: (event) => set(state => ({
    events: [...state.events.slice(-99), event]
  })),

  selectNode: (nodeId) => set({ selectedNode: nodeId }),

  clearEvents: () => set({ events: [] }),
}));
```

### 5.3 Type-Safe gRPC-Web Integration

```typescript
// services/grpcClient.ts
import { FabricServiceClient } from '../generated/FabricServiceClientPb';
import { SmsServiceClient } from '../generated/SmsServiceClientPb';

const grpcWebHost = process.env.REACT_APP_GRPC_WEB_URL || 'http://localhost:8080';

export const fabricClient = new FabricServiceClient(grpcWebHost);
export const smsClient = new SmsServiceClient(grpcWebHost);

export interface GrpcClientConfig {
  host: string;
  port: number;
  ssl: boolean;
}

export function initializeGrpcClient(config: GrpcClientConfig) {
  // dynamic client initialization
}
```

---

## RPC COMMUNICATION LAYER

### 6.1 Protocol Decision Matrix

| Protocol | Use Case | Priority |
|----------|----------|----------|
| **HTTP/REST** | Browser ↔ Backend (CORS, security) | High |
| **gRPC** | Backend ↔ Ollama, Database, Functions | High |
| **gRPC-Web** | React Browser ↔ gRPC Backend | Medium |
| **JSON-RPC 2.0** | Fallback for simple sync calls | Low |
| **SSE** | Real-time events (text/event-stream) | High |
| **WebSocket** | Future bidirectional (not MVP) | Low |

### 6.2 Hybrid Communication Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    BROWSER (React)                       │
│  - Uses HTTP for REST (backward compat)                 │
│  - Uses gRPC-Web for streaming (performance)            │
│  - Uses SSE for event pushes (real-time)                │
└──────────────┬──────────────────────────────────────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    │          │          │
    ▼          ▼          ▼
  HTTP      gRPC-Web     SSE
  (REST)    (streaming)  (events)
    │          │          │
    └──────────┼──────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│         Quarkus REST Layer (JAX-RS Adapters)           │
│  - OpenAPI endpoint metadata                            │
│  - REST ↔ gRPC translation                             │
│  - Authentication & CORS                                │
└──────────────┬──────────────────────────────────────────┘
               │
    ┌──────────┼──────────────────┐
    │          │                  │
    ▼          ▼                  ▼
 gRPC       gRPC              gRPC
Services   Streaming          Unary
    │          │                  │
    └──────────┼──────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│      gRPC Service Layer (Business Logic)               │
│  - FabricService                                        │
│  - SmsService                                           │
│  - PolyglotService                                      │
└──────────────┬──────────────────────────────────────────┘
               │
    ┌──────────┼────────────┐
    │          │            │
    ▼          ▼            ▼
PostgreSQL  Ollama      Functions
(R2DBC)     (gRPC)      (GraalVM)
```

### 6.3 Protocol Implementation Details

#### 6.3.1 gRPC Service Definition Example

```protobuf
syntax = "proto3";

package fabric.services.v1;

service FabricService {
    // Unary RPC
    rpc GetTopology(GetTopologyRequest) returns (FabricTopology);
    
    // Server-streaming RPC
    rpc StreamEvents(StreamRequest) returns (stream FabricEvent);
    
    // Client-streaming RPC (future)
    // rpc BulkPublishEvents(stream FabricEvent) returns (BulkPublishResponse);
}

message GetTopologyRequest {
    string filter = 1;
}

message FabricTopology {
    repeated Node nodes = 1;
    repeated Edge edges = 2;
}

message StreamRequest {
    int32 event_limit = 1;
}

message FabricEvent {
    string event_id = 1;
    string timestamp = 2;
    string source_node = 3;
    string event_type = 4;
}
```

#### 6.3.2 gRPC-Web Configuration (Quarkus)

```yaml
# application.yml (Quarkus)
quarkus:
  grpc:
    server:
      port: 9000
      enable-reflection: true
      plain-text: true
  grpc-web:
    enabled: true
    root-path: /api/grpc-web
```

#### 6.3.3 Envelope Pattern for gRPC ↔ REST

```java
// REST endpoint
@POST
@Path("/fabric/events")
public Response publishEvent(FabricEventDto dto) {
    // Wrap in gRPC message
    PublishEventRequest grpcReq = PublishEventRequest.newBuilder()
        .setEvent(FabricEventMapper.toProto(dto))
        .build();
    
    // Call gRPC service
    PublishEventResponse grpcResp = fabricService.publishEvent(grpcReq);
    
    // Unwrap response
    return Response.accepted(FabricEventResponseMapper.toDto(grpcResp)).build();
}
```

---

## TECHNOLOGY STACK (REWRITE)

### 7.1 Backend Dependencies (Quarkus)

```xml
<!-- pom.xml -->
<project>
    <properties>
        <quarkus.version>3.13.0</quarkus.version>
        <grpc.version>1.62.0</grpc.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Core Quarkus -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-jsonb</artifactId>
        </dependency>

        <!-- gRPC & Protobuf -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-grpc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty-shaded</artifactId>
        </dependency>

        <!-- Reactive Data -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-reactive-datasource</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-reactive-pg-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-flyway</artifactId>
        </dependency>

        <!-- Spring AI (if maintaining compatibility) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
        </dependency>

        <!-- Security & Observability -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-oidc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-micrometer</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 7.2 Frontend Dependencies (React + Vite + TypeScript)

```json
{
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-flow-renderer": "^11.11.0",
    "zustand": "^4.5.0",
    "@grpc/grpc-js": "^1.11.0",
    "@grpc/grpc-js-web": "^1.5.0",
    "axios": "^1.7.0",
    "date-fns": "^3.0.0"
  },
  "devDependencies": {
    "typescript": "^5.4.0",
    "@vitejs/plugin-react": "^4.3.0",
    "vite": "^5.2.0",
    "@types/react": "^18.3.0",
    "@types/react-dom": "^18.3.0",
    "vitest": "^1.3.0",
    "@testing-library/react": "^14.2.0",
    "tailwindcss": "^3.4.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0"
  }
}
```

### 7.3 Comparison: Main vs. Rewrite

| Aspect | Main Branch | Rewrite Branch |
|--------|------------|-----------------|
| **Backend Framework** | Spring Boot 3.5.14 | Quarkus 3.13.0 |
| **API Protocol** | REST + SSE | REST + gRPC-Web |
| **Frontend** | Vue 3 + Vite | React 18 + Vite + TypeScript |
| **State Management** | Pinia | Zustand / Redux Toolkit |
| **Graph Library** | VueFlow | React Flow |
| **Database Driver** | Spring R2DBC | Quarkus R2DBC |
| **Native Compilation** | GraalVM (optional) | GraalVM (native-image first) |
| **Startup Time** | ~3-5s (JVM) | ~50-100ms (native) |
| **Memory (RSS)** | 400-600MB | 50-100MB |
| **Container Size** | 500MB+ | 50-100MB |
| **Polyglot Support** | GraalVM | Quarkus extensions |

---

## CONFIGURATION & ENVIRONMENT

### 8.1 Quarkus Configuration (application.yml)

```yaml
quarkus:
  application:
    name: fabric-engine-rewrite
    version: 2.0.0-quarkus

  # HTTP
  http:
    port: 8080
    host: 0.0.0.0
    cors:
      origins: ${FABRIC_CORS_ALLOWED_ORIGINS:http://localhost:5173}
      methods: GET,POST,PUT,DELETE,OPTIONS
      headers: Content-Type,Authorization,X-Requested-With

  # gRPC
  grpc:
    server:
      port: 9000
      enable-reflection: true
      enable-grpc-web: true
    clients:
      ollama:
        host: ${OLLAMA_HOST:localhost}
        port: 9001
        plain-text: true

  # Database
  datasource:
    devservices:
      enabled: false
    db-kind: postgresql
    username: ${FABRIC_DB_USER:fabric}
    password: ${FABRIC_DB_PASSWORD:fabric}
    jdbc:
      url: ${FABRIC_JDBC_URL:jdbc:postgresql://localhost:5432/fabric}
    reactive:
      url: ${FABRIC_REACTIVE_URL:vertx-reactive:postgresql://localhost/fabric}

  # Flyway
  flyway:
    schemas: public
    locations: classpath:db/migration
    baseline-on-migrate: true

  # Native Compilation
  native:
    enable-reports: true
    enable-vm-inspect: false
    full-stack-traces: false

  # Logging
  log:
    level: INFO
    category:
      "com.acme.fabric": DEBUG
    console:
      format: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c{2.}] %s%e%n"

  # Observability
  micrometer:
    enabled: true
    export:
      prometheus:
        enabled: true

spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        model: ${OLLAMA_CHAT_MODEL:llama3.2:3b}
      embedding:
        model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

fabric:
  openapi-path: ${FABRIC_OPENAPI_PATH:/app/openapi.yml}
  demo-events-enabled: ${FABRIC_DEMO_EVENTS_ENABLED:true}
  event-generation-interval: 8000
```

### 8.2 React Vite Configuration (vite.config.ts)

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE || 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/grpc-web': {
        target: process.env.VITE_GRPC_WEB_URL || 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: process.env.NODE_ENV === 'production' ? false : true,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendors': ['react', 'react-dom'],
          'grpc-vendors': ['@grpc/grpc-js-web'],
          'flow': ['react-flow-renderer'],
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    }
  }
})
```

---

## DATA PERSISTENCE & SCHEMA

### 9.1 Quarkus + R2DBC (Reactive PostgreSQL)

**No schema changes required**. Same PostgreSQL database as main branch.

```java
// Entity definition (Quarkus)
@Entity
@Table(name = "fabric_nodes")
public class FabricNode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "node_type")
    private String type;

    @Column(name = "status")
    private String status;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// Reactive repository (Quarkus Panache)
@ApplicationScoped
public class FabricNodeRepository implements PanacheRepository<FabricNode> {
    public Uni<List<FabricNode>> findActive() {
        return find("status", "RUNNING").list();
    }
}
```

### 9.2 Migration Path (Flyway)

**No changes to migration scripts**. Existing Flyway migrations work unchanged.

```sql
-- V1__initial_schema.sql (unchanged)
CREATE TABLE fabric_nodes (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  node_type VARCHAR(50) NOT NULL,
  status VARCHAR(50) DEFAULT 'INITIALIZING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  metadata JSONB
);
```

---

## PERFORMANCE & SCALABILITY GOALS

### 10.1 Performance Targets (Rewrite vs. Main)

| Metric | Main (Spring) | Rewrite (Quarkus) | Improvement |
|--------|---------------|-------------------|------------|
| **Startup Time (JVM)** | 3-5s | 2-3s (50% faster) | 40% |
| **Startup Time (Native)** | N/A | 50-100ms | - |
| **Memory (JVM RSS)** | 400-600MB | 250-350MB | 35% |
| **Memory (Native RSS)** | N/A | 50-100MB | - |
| **API Latency (REST)** | <200ms (P95) | <150ms (P95) | 25% |
| **gRPC Latency** | N/A | <5ms | - |
| **Throughput (REST)** | 1000 req/s | 1500 req/s | 50% |
| **Throughput (gRPC)** | N/A | 5000 req/s | - |

### 10.2 Scalability Metrics

| Scenario | Target | Notes |
|----------|--------|-------|
| **Concurrent Users** | 5000+ | Quarkus handles with lower memory |
| **Concurrent gRPC streams** | 10000+ | Per instance |
| **Events/sec throughput** | 5000+ events/sec | gRPC streaming |
| **Database connections** | 50 (tunable) | R2DBC pool |
| **Horizontal scaling** | Linear | Stateless design |

---

## MIGRATION STRATEGY

### 11.1 Parallel Deployment (Blue-Green)

```
Week 1-4:  Deploy Quarkus on separate infrastructure
           Run both main and rewrite in parallel
           Sync data between instances

Week 5-8:  Quarkus: 10% of traffic
           Monitor: latency, errors, CPU, memory

Week 9-12: Quarkus: 50% of traffic
           Gradual traffic shift based on metrics

Week 13-16: Quarkus: 100% of traffic
            Deprecate Spring Boot instance

Week 17+:  Maintain Spring Boot for rollback capability
           Gradual decommission after stabilization
```

### 11.2 Data Consistency

**Single PostgreSQL database** for both versions.

- Spring WebFlux reads/writes to `fabric_db`
- Quarkus reads/writes to same `fabric_db`
- Flyway migrations run on both deployments
- No data transformation needed

### 11.3 API Contract Compatibility

**OpenAPI 3.1.0 maintained across both versions**.

```yaml
# Same OpenAPI spec, different backing services
/api/v1/fabric/topology:
  get:
    # Main branch: Spring WebFlux
    # Rewrite branch: Quarkus REST → gRPC bridge
    # Both return identical JSON
```

### 11.4 Rollback Plan

```
If Quarkus deployment fails:
1. Revert load balancer routing to Spring Boot
2. No data loss (shared database)
3. Restore service within 2 minutes
4. Post-mortem investigation

If data corruption detected:
1. Restore from PostgreSQL backup
2. Re-sync event journal
3. Manual reconciliation if needed
```

---

## TESTING & QUALITY ASSURANCE

### 12.1 Test Matrix

| Test Type | Framework | Coverage | Priority |
|-----------|-----------|----------|----------|
| **Unit** | JUnit 5 + Mockito | >80% services | High |
| **Integration** | Quarkus @QuarkusTest | REST endpoints | High |
| **gRPC Integration** | gRPC testing utilities | gRPC stubs | Medium |
| **React Unit** | Vitest + RTL | >70% components | High |
| **React E2E** | Cypress | User workflows | Medium |
| **Load Testing** | Gatling / JMeter | 5000 concurrent | High |
| **Performance** | JProfiler | Memory leaks | High |
| **Security** | OWASP ZAP | Vulnerabilities | High |

### 12.2 Sample Quarkus Integration Test

```java
@QuarkusTest
public class FabricTopologyResourceTest {
    
    @Test
    public void testGetTopology() {
        given()
            .get("/api/v1/fabric/topology")
            .then()
            .statusCode(200)
            .body("nodes", hasSize(greaterThan(0)));
    }

    @Test
    public void testPublishEvent() {
        FabricEvent event = new FabricEvent(/* ... */);
        
        given()
            .contentType(ContentType.JSON)
            .body(event)
            .post("/api/v1/fabric/events")
            .then()
            .statusCode(202);
    }
}
```

### 12.3 React Component Test Example

```typescript
import { render, screen } from '@testing-library/react';
import { FabricTopology } from './FabricTopology';

describe('FabricTopology Component', () => {
  it('renders topology canvas', () => {
    render(<FabricTopology />);
    expect(screen.getByRole('heading', { name: /fabric topology/i })).toBeInTheDocument();
  });

  it('handles node selection', async () => {
    const { user } = render(<FabricTopology />);
    const nodeElement = screen.getByTestId('node-001');
    await user.click(nodeElement);
    expect(nodeElement).toHaveClass('selected');
  });
});
```

---

## KNOWN CONSTRAINTS & RISKS

### 13.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-----------|--------|-----------|
| **gRPC-Web browser compatibility** | Low | High | Use feature detection, fallback to REST |
| **Quarkus extension immaturity** | Medium | Medium | Vet extensions, contribute back to community |
| **React ecosystem fragmentation** | Low | Medium | Use well-established libraries (React Flow, Zustand) |
| **Native image compilation failure** | Medium | High | Extensive testing, GraalVM configuration |
| **Database migration issues** | Low | High | Dry-run migrations, backup strategy |
| **gRPC client generation errors** | Low | Medium | Protoc validation, code review |

### 13.2 Operational Constraints

- **GraalVM native build time**: ~5-10 minutes (Docker layer caching helps)
- **gRPC debugging**: Requires tools (grpcurl, Postman gRPC plugin)
- **Quarkus extension updates**: May require configuration changes
- **React dev tool ecosystem**: Slightly different from Vue tooling

### 13.3 Compatibility Notes

- **Drop Spring WebFlux dependencies** from Quarkus (no dual dependency)
- **gRPC-Web CORS headers** must match REST endpoints
- **Protocol buffer version**: Lock to protoc 3.x
- **Node.js version**: Require Node 18+ for React build

---

## IMPLEMENTATION TIMELINE

### 14.1 Detailed Roadmap

```
PHASE 1: Foundation (4 weeks)
├─ Week 1: Quarkus project setup, REST migration, Flyway config
├─ Week 2: R2DBC integration, Spring AI compatibility
├─ Week 3: Docker build, compose-up testing
└─ Week 4: Deployment to staging, basic smoke tests

PHASE 2: gRPC Layer (4 weeks)
├─ Week 5: Proto file design, service definitions
├─ Week 6: gRPC server implementation, code generation
├─ Week 7: gRPC-Web bridge layer, CORS config
└─ Week 8: Performance benchmarking vs REST

PHASE 3: React Frontend (4 weeks)
├─ Week 9: React project init, component hierarchy
├─ Week 10: VueFlow → React Flow migration
├─ Week 11: State management (Zustand), gRPC client setup
└─ Week 12: UI polish, accessibility fixes

PHASE 4: Native Compilation (4 weeks)
├─ Week 13: GraalVM native-image configuration
├─ Week 14: Reflection config, native build optimization
├─ Week 15: Alpine container optimization
└─ Week 16: Load testing, metrics collection

PHASE 5: Production Release (4 weeks)
├─ Week 17: Blue-green deployment setup
├─ Week 18: Gradual traffic migration (10% → 50%)
├─ Week 19: Production monitoring, performance tuning
└─ Week 20: GA release, Spring Boot deprecation notice
```

### 14.2 Key Milestones

| Milestone | Date | Criteria |
|-----------|------|----------|
| **Dev Environment Working** | Week 2 | Docker Compose up, health checks pass |
| **API Feature Parity** | Week 4 | All endpoints functional, same responses |
| **gRPC Operational** | Week 8 | gRPC-Web clients work, latency <5ms |
| **React UI Ready** | Week 12 | All components migrated, functionality parity |
| **Native Binary Works** | Week 16 | Startup <100ms, memory <100MB |
| **Production Candidate** | Week 18 | All tests pass, load testing successful |
| **GA Release** | Week 20 | Spring Boot deprecated, Quarkus production |

---

## GLOSSARY

| Term | Definition |
|------|-----------|
| **Quarkus** | Cloud-native Java framework (50ms startup, native compilation) |
| **gRPC** | High-performance RPC framework (binary serialization, HTTP/2) |
| **gRPC-Web** | gRPC adaptation for browsers (HTTP/1.1 compatibility) |
| **Protocol Buffers** | Language-neutral serialization format (.proto files) |
| **Protoc** | Protocol Buffer compiler (generates code from .proto) |
| **GraalVM** | Polyglot JVM with native compilation support |
| **Native Image** | GraalVM compiled binary (executable, not JVM) |
| **React Flow** | Graph visualization library (replacement for VueFlow) |
| **Zustand** | Lightweight state management library (alternative to Redux) |
| **Vite** | Fast frontend build tool (same as main branch) |
| **TypeScript** | Typed JavaScript (added for better IDE support) |
| **R2DBC** | Reactive Database Connectivity (async JDBC) |
| **Flyway** | Database migration tool (unchanged from main branch) |
| **Blue-Green Deployment** | Run 2 production environments, switch traffic |
| **Envelope Pattern** | Wrap service responses in metadata containers |
| **OpenAPI** | API specification format (REST contract) |

---

## DOCUMENT INFORMATION

**Author:** 25-Year Senior Prompt Engineer  
**Repository:** https://github.com/ajaychaurasia25921/fabric-engine-master-control-plane-v2  
**Branch:** rewrite-quarkus-react-rpc  
**Technology Stack:**  
- Backend: Quarkus 3.13.0 + gRPC 1.62.0  
- Frontend: React 18 + Vite 5 + TypeScript 5  
- Protocol: REST + gRPC-Web + SSE  
- Database: PostgreSQL 16 (unchanged)  

**Created:** May 23, 2026  
**Last Updated:** May 23, 2026  
**Status:** Finalized  

**Related Documents:**
- REQUIREMENTS_MAIN_BRANCH.md (Spring WebFlux + Vue 3)
- README.md (Quick start guide)
- openapi.yml (API specification - shared)

**Implementation Owner:** Architecture Team  
**Estimated Effort:** 20 weeks (5 months)  
**Expected ROI:** 60x faster startup, 80% less memory, 5x better throughput

---

**END OF REQUIREMENTS DOCUMENT - REWRITE BRANCH**

