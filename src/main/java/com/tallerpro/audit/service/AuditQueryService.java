package com.tallerpro.audit.service;

import com.tallerpro.audit.dto.AuditDtos.OrderTimelineSummary;
import com.tallerpro.audit.dto.AuditDtos.TimelineEntry;
import com.tallerpro.audit.dto.AuditDtos.TimelinePage;
import com.tallerpro.audit.model.AuditEvent;
import com.tallerpro.audit.model.OrderStatus;
import com.tallerpro.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-16: consulta de solo lectura del timeline de auditoria, con
 * filtros de usuario (actor), fecha y tipo de evento. Reservado al
 * rol Auditor (ver SecurityConfig).
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    /** Mapea cada estado de la orden al "hito" de negocio que representa. */
    private static final Map<OrderStatus, String> HITOS = Map.of(
            OrderStatus.RECEPCIONADA, "recepciono",
            OrderStatus.DIAGNOSTICADA, "diagnostico",
            OrderStatus.EN_REPARACION, "reparo",
            OrderStatus.ENTREGADA, "entrego"
    );

    public TimelinePage search(String orderId, String actorId, OrderStatus eventType,
                                Instant from, Instant to, Pageable pageable) {
        Page<AuditEvent> page = auditEventRepository.search(orderId, actorId, eventType, from, to, pageable);

        List<TimelineEntry> content = page.getContent().stream()
                .map(this::toEntry)
                .toList();

        return new TimelinePage(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Timeline completo de una orden, verificando que existan los
     * cuatro hitos clave (recepcion, diagnostico, reparacion, entrega)
     * exigidos por el negocio para respaldar garantias y reclamos.
     */
    public OrderTimelineSummary orderTimeline(String orderId) {
        List<AuditEvent> events = auditEventRepository.findByOrderIdOrderByEventTimestampAsc(orderId);

        List<TimelineEntry> entries = events.stream().map(this::toEntry).toList();

        Map<String, TimelineEntry> hitos = new LinkedHashMap<>();
        for (AuditEvent event : events) {
            String hito = HITOS.get(event.getEventType());
            if (hito != null) {
                hitos.putIfAbsent(hito, toEntry(event));
            }
        }

        boolean completo = hitos.keySet().containsAll(HITOS.values());

        return new OrderTimelineSummary(orderId, entries, hitos, completo);
    }

    private TimelineEntry toEntry(AuditEvent event) {
        return new TimelineEntry(
                event.getEventId(),
                event.getOrderId(),
                event.getTallerId(),
                event.getEventType().name(),
                event.getActorId(),
                event.getActorName(),
                event.getActorRole().name(),
                event.getEventTimestamp(),
                event.getRecordedAt()
        );
    }
}
