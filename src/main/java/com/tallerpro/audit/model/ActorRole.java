package com.tallerpro.audit.model;

/**
 * Rol del actor humano que origino el evento (RBAC de TallerPro).
 * Se persiste junto a cada AuditEvent para poder responder de forma
 * verificable "quien recepciono, diagnostico, reparo o entrego" cada
 * vehiculo (seccion 7.3 del DAS).
 */
public enum ActorRole {
    ADMIN,
    JEFE_TALLER,
    MECANICO,
    CLIENTE,
    SISTEMA
}
