# ms-tallerpro-audit

> **Estado: provisorio / en construcción.** Faltan tests, CI/CD y detalles de despliegue final.

Microservicio de **auditoría y trazabilidad inmutable** para TallerPro. Consume eventos de Kafka (`jobs.events` / `audit.timeline`) y persiste, sin posibilidad de modificación o borrado, el timeline completo de cada orden de servicio: quién la recepcionó, diagnosticó, reparó y entregó.

## Qué hace

- Consume `jobs.events` y `audit.timeline` de forma asíncrona (nunca llama de vuelta a otro microservicio).
- Persiste cada evento como un registro **inmutable** (RF-15): `orderId`, `actor`, `rol`, `tipoEvento`, `timestamp`, `payload`.
- Expone el timeline con filtros de actor, fecha y tipo de evento (RF-16), en modo **solo lectura**, reservado a los roles **Auditor** y **Admin** (caso, sección 6).
- Verifica que el timeline de una orden cubra los 4 hitos clave: recepción, diagnóstico, reparación y entrega.

## Cómo garantiza la inmutabilidad

Dos capas independientes:

1. **Aplicación**: `AuditEventRepository` no expone (ni se usan) métodos de update/delete; el único flujo de escritura es `AuditIngestionService.record()`, que solo hace `save()` de una entidad nueva.
2. **Base de datos**: la migración `V1__init_audit_schema.sql` agrega triggers (`trg_audit_event_no_update`, `trg_audit_event_no_delete`) que rechazan cualquier `UPDATE`/`DELETE` sobre `audit_event`, incluso ante un acceso directo con un cliente SQL.

## Stack

Spring Boot 3.3 · Java 17 · Spring Kafka · Spring Data JPA · PostgreSQL · Flyway · Spring Security (OAuth2 Resource Server / JWT) · springdoc-openapi · Lombok

## Estructura del proyecto

```
src/main/java/com/tallerpro/audit/
├── config/       # Kafka consumer, seguridad JWT (roles Auditor/Admin), OpenAPI
├── controller/   # Endpoints REST de solo lectura
├── dto/          # AuditEventMessage (payload Kafka) y DTOs de respuesta
├── kafka/        # Listener de jobs.events / audit.timeline
├── model/        # AuditEvent (inmutable), OrderStatus, ActorRole
├── repository/   # Repositorio de solo INSERT + SELECT
└── service/      # Ingesta (insert-only) + consulta del timeline
src/main/resources/
├── application.yml
└── db/migration/ # Migraciones Flyway (incluye triggers de inmutabilidad)
```

## Cómo correrlo localmente

### Requisitos

- Java 17+, Maven 3.9+ (o usa `./mvnw` si lo agregaste, ver `ms-tallerpro-report`)
- PostgreSQL corriendo (o el `docker-compose` de más abajo)
- Un broker Kafka con los tópicos `jobs.events` / `audit.timeline`

### Variables de entorno principales

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `tallerpro_audit` / `tallerpro` / `tallerpro` | Conexión a PostgreSQL |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker(s) Kafka |
| `AZURE_TENANT_ID` | `tenant-id` | Tenant de Azure AD (issuer del JWT) |
| `AZURE_API_CLIENT_ID` | `api-client-id` | Client ID de la App Registration (audience del JWT), el mismo para todos los MS |
| `TALLERPRO_JWT_ENABLED` | `true` | `false` solo en desarrollo local sin tenant (usuario ficticio con `TALLERPRO_DEV_ROLES`, default `Auditor`) |
| `SERVER_PORT` | `8086` | |

### Levantar

```bash
mvn spring-boot:run
```

- Swagger UI: `http://localhost:8086/swagger-ui.html`
- Health: `http://localhost:8086/actuator/health`

### Con Docker

```bash
docker build -t ms-tallerpro-audit .
```

Ver `compose-audit-fragment.yml` para integrarlo en `compose-apps.yml`, con su propia base PostgreSQL.

## Endpoints principales

Todos requieren `Authorization: Bearer <JWT>` con rol **Auditor** o **Admin**. Sin token → 401; otro rol → 403.

Ejecución local sin Postgres/Kafka: `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=test "-Dspring-boot.run.arguments=--tallerpro.security.jwt-enabled=false"`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/audit/timeline` | Timeline paginado con filtros `orderId`, `actorId`, `eventType`, `from`, `to` |
| GET | `/api/audit/orders/{orderId}/timeline` | Timeline completo de una orden + verificación de los 4 hitos clave |

## Pendiente

- [ ] Tests (inmutabilidad, consulta con filtros, verificación de timeline completo) — próximo paso
- [ ] Confirmar si `ms-tallerpro-jobs` publica `actorId`/`actorName`/`actorRole` directamente en el evento o si hay que resolverlos vía otro medio
- [ ] Revisar `issuer-uri` / `audiences` contra la App Registration definitiva
- [ ] CI/CD (GitHub Actions) hacia EC2
