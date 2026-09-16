package com.tallerpro.audit.service;

import com.tallerpro.audit.dto.AuditEventMessage;
import com.tallerpro.audit.model.AuditEvent;
import com.tallerpro.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * RF-15: transforma el evento de Kafka en un registro AuditEvent
 * inmutable. Este servicio SOLO inserta (repository.save de una
 * entidad nueva, sin id) - jamas actualiza ni elimina un registro
 * existente. Es, junto al trigger de base de datos, la segunda capa
 * de la garantia de inmutabilidad.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditIngestionService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void record(AuditEventMessage event) {
        if (auditEventRepository.existsByEventId(event.eventId())) {
            log.info("Evento duplicado ignorado (idempotencia): eventId={}", event.eventId());
            return;
        }

        Instant recordedAt = Instant.now();

        AuditEvent auditEvent = AuditEvent.builder()
                .eventId(event.eventId())
                .orderId(event.orderId())
                .tallerId(event.tallerId())
                .eventType(event.status())
                .actorId(event.actorId() != null ? event.actorId() : "desconocido")
                .actorName(event.actorName() != null ? event.actorName() : "Desconocido")
                .actorRole(event.actorRole() != null ? event.actorRole() : com.tallerpro.audit.model.ActorRole.SISTEMA)
                .eventTimestamp(event.timestamp() != null ? event.timestamp() : recordedAt)
                .recordedAt(recordedAt)
                .payload(event.payload())
                .build();

        auditEventRepository.save(auditEvent);
    }
}
