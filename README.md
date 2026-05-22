# Fabric Engine Master Control Plane v2

Reactive control-plane baseline for network fabric operations:

- Spring WebFlux functional routes for fabric topology, event ingestion, SSE streaming, and SMS dispatch.
- Spring AI `FabricAgent` that ingests `openapi.yml` into a `VectorStore` when configured and reacts to anomalies from a `Flux` event stream.
- R2DBC-backed SMS dispatch persistence to keep database operations off the event loop.
- Vue 3 Composition API frontend with VueFlow packet canvas, Pinia fabric state, SSE updates, and an SMS dispatch side panel.

## API

The OpenAPI contract lives in [openapi.yml](/Users/ajay/Documents/Codex/2026-05-22/role-you-are-a-principal-cloud/openapi.yml).

Implemented endpoints:

- `GET /api/v1/fabric/topology`
- `POST /api/v1/fabric/events`
- `GET /api/v1/fabric/stream`
- `POST /sms/dispatches`

## Backend

```bash
cd backend
mvn spring-boot:run
```

The demo telemetry simulator publishes node events every 8 seconds. Disable it with:

```yaml
fabric:
  telemetry:
    demo-events-enabled: false
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Use `VITE_API_BASE=http://localhost:8080` if the backend runs somewhere else.

## Notes

Spring AI `1.1.6` is used through the official BOM. If a `VectorStore` or `ChatClient.Builder` bean is configured by adding provider-specific starters, `FabricAgent` will use it. Without model credentials, the agent falls back to deterministic rule-based remediation plans while preserving the same reactive flow.
