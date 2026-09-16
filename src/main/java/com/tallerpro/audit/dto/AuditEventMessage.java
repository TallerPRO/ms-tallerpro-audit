package com.tallerpro.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tallerpro.audit.model.ActorRole;
import com.tallerpro.audit.model.OrderStatus;

import java.time.Instant;

/**
 * Envelope comun publicado por ms-tallerpro-jobs en jobs.events /
 * audit.timeline (type, eventId, timestamp, traceId, correlationId),
 * mas los datos de negocio necesarios para reconstruir el timeline de
 * auditoria: quien (actor/rol) hizo que (status) sobre que orden.
 *
 * Ejemplo:
 * {
 *   "eventId": "b3f1...",
 *   "type": "ORDER_STATUS_CHANGED",
 *   "timestamp": "2026-09-15T14:32:01Z",
 *   "traceId": "...",
 *   "correlationId": "...",
 *   "orderId": "ORD-1024",
 *   "tallerId": "TALLER-07",
 *   "status": "DIAGNOSTICADA",
 *   "actorId": "mec-045",
 *   "actorName": "Juan Perez",
 *   "actorRole": "MECANICO"
 * }
 */
public record AuditEventMessage(
        String eventId,
        String type,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        String traceId,
        String correlationId,
        String orderId,
        String tallerId,
        OrderStatus status,
        String actorId,
        String actorName,
        ActorRole actorRole,
        String payload
) {
}
