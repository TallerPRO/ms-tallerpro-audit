package com.tallerpro.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-tallerpro-audit
 *
 * Consume eventos de Kafka (jobs.events / audit.timeline) y persiste
 * de forma INMUTABLE el timeline de cada orden de servicio: quien
 * recepciono, diagnostico, reparo o entrego cada vehiculo.
 *
 * Todos los endpoints expuestos son de solo lectura (RF-16), reservados
 * al rol Auditor. No existe ninguna operacion de escritura hacia el
 * resto de la plataforma ni forma de modificar/eliminar un registro ya
 * persistido (ver V1__init_audit_schema.sql: trigger que bloquea
 * UPDATE/DELETE a nivel de base de datos).
 */
@SpringBootApplication
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
