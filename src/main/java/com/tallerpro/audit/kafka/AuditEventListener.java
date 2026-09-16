package com.tallerpro.audit.kafka;

import com.tallerpro.audit.dto.AuditEventMessage;
import com.tallerpro.audit.service.AuditIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditIngestionService auditIngestionService;

    /**
     * Escucha ambos topicos declarados en el DAS para este microservicio:
     * jobs.events (fuente de verdad) y audit.timeline (espejo compactado).
     * La idempotencia por eventId evita duplicar el registro si el mismo
     * evento llega por ambos canales o se reintenta.
     */
    @KafkaListener(
            topics = {"${app.kafka.topics.jobs-events}", "${app.kafka.topics.audit-timeline}"},
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAuditEvent(AuditEventMessage event, Acknowledgment acknowledgment) {
        log.debug("Evento recibido: eventId={} orderId={} status={} actor={}",
                event.eventId(), event.orderId(), event.status(), event.actorId());
        auditIngestionService.record(event);
        acknowledgment.acknowledge();
    }
}
