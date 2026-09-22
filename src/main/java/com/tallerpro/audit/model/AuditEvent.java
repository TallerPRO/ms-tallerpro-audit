package com.tallerpro.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RF-15: registro inmutable de un evento del ciclo de vida de una
 * orden de servicio. Campos exigidos por el caso: orderId, actor, rol,
 * tipoEvento, timestamp, payload.
 *
 * Inmutabilidad en dos capas:
 *  1) Aplicacion: no existen setters ni metodos de update/delete en
 *     esta clase ni en AuditEventRepository - solo se puede crear e
 *     insertar (save de una entidad nueva).
 *  2) Base de datos: V1__init_audit_schema.sql agrega un trigger que
 *     rechaza cualquier UPDATE/DELETE sobre la tabla audit_event,
 *     incluso ante un bug de la aplicacion o un acceso directo a la BD.
 */
@Entity
@Table(
        name = "audit_event",
        indexes = {
                @Index(name = "idx_audit_event_order", columnList = "orderId"),
                @Index(name = "idx_audit_event_actor", columnList = "actorId"),
                @Index(name = "idx_audit_event_ts", columnList = "eventTimestamp"),
                @Index(name = "idx_audit_event_type", columnList = "eventType")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_audit_event_event_id", columnNames = "eventId")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id del evento origen en Kafka; garantiza idempotencia ante redelivery. */
    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 60)
    private String orderId;

    @Column(nullable = false, length = 60)
    private String tallerId;

    /** "tipoEvento": aqui, el estado al que transiciono la orden. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private OrderStatus eventType;

    /** Actor: quien ejecuto la accion (id + nombre para trazabilidad legible). */
    @Column(nullable = false, length = 60)
    private String actorId;

    @Column(nullable = false, length = 120)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActorRole actorRole;

    /** Timestamp del evento original (emitido por ms-tallerpro-jobs). */
    @Column(nullable = false)
    private Instant eventTimestamp;

    /** Momento en que este microservicio persistio el registro. */
    @Column(nullable = false)
    private Instant recordedAt;

    /**
     * Payload original del evento (JSON crudo), para trazabilidad completa.
     *
     * Sin @Lob a proposito: la columna es TEXT y en PostgreSQL @Lob sobre un
     * String hace que el driver la trate como large object (OID); la lectura
     * falla con "Large Objects may not be used in auto-commit mode". H2 no lo
     * distingue, por eso los tests pasaban igual.
     */
    @Column(columnDefinition = "TEXT")
    private String payload;
}
