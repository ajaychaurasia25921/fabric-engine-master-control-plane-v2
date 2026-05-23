# Fabric Engine Master Control Plane v2

Reactive control-plane baseline for network fabric operations, aligned to the provided `openapi.yml`, `index3-full-final.html`, and the SMS-only flow from `index-sms-final.html`.

- Spring WebFlux functional routes for fabric topology, event ingestion, SSE streaming, and SMS dispatch.
- Spring AI `FabricAgent` using local Ollama chat and embeddings. It ingests `openapi.yml` into a `VectorStore` and reacts to anomalies from a `Flux` event stream.
- R2DBC-backed SMS dispatch persistence to keep database operations off the event loop.
- Vue 3 Composition API frontend with VueFlow packet canvas, Pinia fabric state, SSE updates, and an SMS dispatch side panel.
- GraalVM polyglot function factory for generating AWS Lambda or Azure Functions Java wrappers that execute `function.js` through GraalJS.

## API

The OpenAPI contract lives in [openapi.yml](/Users/ajay/Documents/Codex/2026-05-22/role-you-are-a-principal-cloud/openapi.yml).

Implemented endpoints:

- `GET /api/v1/fabric/topology`
- `POST /api/v1/fabric/events`
- `GET /api/v1/fabric/stream`
- `POST /sms/dispatches`
- `POST /api/v1/platform/functions/blueprints`
- `POST /api/v1/platform/polyglot/scripts/evaluate`

## Docker Compose

```bash
docker compose up --build
```

Services:

- `ollama` on `11434`
- `postgres` on `5432`
- `backend` on `8080`
- `frontend` on `5173`

Spring AI auto-pulls `llama3.2:3b` and `nomic-embed-text` when missing. Override with `OLLAMA_CHAT_MODEL` and `OLLAMA_EMBEDDING_MODEL`.
Database schema is created at backend startup by Flyway migrations in `backend/src/main/resources/db/migration`. Docker Compose uses Postgres with R2DBC for app access and JDBC/Flyway for migrations.

## Public Hosting

This repository includes `render.yaml` for a GitHub-connected Render Blueprint:

- `reactor-backend`: Dockerized Spring WebFlux service with `/actuator/health`.
- `reactor-frontend`: static Vue build with SPA rewrites.

Before public launch, create managed PostgreSQL credentials and an externally reachable Ollama-compatible model endpoint, then set the variables from `.env.production.example` in Render. Free web services can sleep after inactivity, and they are not a good place to run Postgres plus Ollama inside the same container. Keep the Compose stack for local development; use managed services for public launch.

Recommended release checklist:

- Set `SPRING_PROFILES_ACTIVE=prod`.
- Set `FABRIC_CORS_ALLOWED_ORIGINS` to the deployed frontend URL.
- Set all R2DBC and Flyway database values to the same managed PostgreSQL database.
- Set `OLLAMA_BASE_URL` to your hosted Ollama-compatible endpoint.
- Set real SMPP credentials for SMS dispatch.
- Leave `FABRIC_DEMO_EVENTS_ENABLED=false` for production unless you intentionally want simulated telemetry.

## Backend

```bash
cd backend
gradle bootRun
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

Spring AI `1.1.6` is used through the official BOM with `spring-ai-starter-model-ollama`. No cloud AI provider or API key is required.

GraalVM polyglot support uses `org.graalvm.polyglot:polyglot` and `org.graalvm.polyglot:js`. The runtime evaluator disables host IO, process creation, thread creation, and environment access; generated AWS/Azure blueprints are source artifacts for your deployment pipeline rather than automatic cloud deploys.
