package com.tallerpro.audit.repository;

import com.tallerpro.audit.model.ActorRole;
import com.tallerpro.audit.model.AuditEvent;
import com.tallerpro.audit.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio de solo INSERT + SELECT. Deliberadamente NO extiende
 * JpaRepository directo en el servicio (ver AuditQueryService, que solo
 * invoca save() para altas y los metodos de consulta de aqui abajo) -
 * la aplicacion nunca llama a deleteById/delete/deleteAll sobre este
 * repositorio; la garantia dura de inmutabilidad la da el trigger de
 * base de datos (V1__init_audit_schema.sql).
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    boolean existsByEventId(String eventId);

    /** RF-16: timeline con filtros opcionales de actor, rango de fechas y tipo de evento. */
    @Query("""
            select ae from AuditEvent ae
            where (:orderId is null or ae.orderId = :orderId)
              and (:actorId is null or ae.actorId = :actorId)
              and (:eventType is null or ae.eventType = :eventType)
              and (:from is null or ae.eventTimestamp >= :from)
              and (:to is null or ae.eventTimestamp <= :to)
            order by ae.eventTimestamp asc
            """)
    Page<AuditEvent> search(@Param("orderId") String orderId,
                             @Param("actorId") String actorId,
                             @Param("eventType") OrderStatus eventType,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

    /** Timeline completo de una orden especifica, en orden cronologico. */
    List<AuditEvent> findByOrderIdOrderByEventTimestampAsc(String orderId);
}
