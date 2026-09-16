package com.tallerpro.audit.model;

/**
 * Estados del ciclo de vida de una orden de servicio, tal como los
 * publica ms-tallerpro-jobs en jobs.events.
 *
 * RECEPCIONADA -> DIAGNOSTICADA -> EN_REPARACION -> LISTA_RETIRO -> ENTREGADA
 *                                                                 -> ANULADA
 */
public enum OrderStatus {
    RECEPCIONADA,
    DIAGNOSTICADA,
    EN_REPARACION,
    LISTA_RETIRO,
    ENTREGADA,
    ANULADA
}
