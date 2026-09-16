package com.tallerpro.audit.controller;

import com.tallerpro.audit.dto.AuditDtos.OrderTimelineSummary;
import com.tallerpro.audit.dto.AuditDtos.TimelinePage;
import com.tallerpro.audit.model.OrderStatus;
import com.tallerpro.audit.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * RF-16: endpoints de solo lectura para el timeline de auditoria.
 * Protegidos por SecurityConfig (solo ROLE_Auditor).
 */
@Tag(name = "Auditoria", description = "Timeline inmutable de eventos de TallerPro")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @Operation(summary = "Timeline con filtros de orden, usuario (actor), fecha y tipo de evento")
    @GetMapping("/timeline")
    public ResponseEntity<TimelinePage> timeline(
            @RequestParam(required = false) String orderId,
            @Parameter(description = "Id del usuario/actor que ejecuto la accion")
            @RequestParam(required = false) String actorId,
            @Parameter(description = "Tipo de evento (estado de la orden)")
            @RequestParam(required = false) OrderStatus eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditQueryService.search(orderId, actorId, eventType, from, to, pageable));
    }

    @Operation(summary = "Timeline completo de una orden, verificando los 4 hitos clave (recepcion, diagnostico, reparacion, entrega)")
    @GetMapping("/orders/{orderId}/timeline")
    public ResponseEntity<OrderTimelineSummary> orderTimeline(@PathVariable String orderId) {
        return ResponseEntity.ok(auditQueryService.orderTimeline(orderId));
    }
}
