package com.tallerpro.audit.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AuditDtos {

    public record TimelineEntry(
            String eventId,
            String orderId,
            String tallerId,
            String eventType,
            String actorId,
            String actorName,
            String actorRole,
            Instant eventTimestamp,
            Instant recordedAt
    ) {}

    public record TimelinePage(
            List<TimelineEntry> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    /**
     * RF-15 (verificacion): confirma que el timeline de una orden cubre
     * las cuatro etapas clave del negocio - quien recepciono,
     * diagnostico, reparo y entrego el vehiculo.
     */
    public record OrderTimelineSummary(
            String orderId,
            List<TimelineEntry> events,
            Map<String, TimelineEntry> hitos,
            boolean completo
    ) {}
}
