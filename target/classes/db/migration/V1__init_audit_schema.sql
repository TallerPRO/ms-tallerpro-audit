CREATE TABLE audit_event (
    id               BIGSERIAL PRIMARY KEY,
    event_id         VARCHAR(100) NOT NULL UNIQUE,
    order_id         VARCHAR(60)  NOT NULL,
    taller_id        VARCHAR(60)  NOT NULL,
    event_type       VARCHAR(20)  NOT NULL,
    actor_id         VARCHAR(60)  NOT NULL,
    actor_name       VARCHAR(120) NOT NULL,
    actor_role       VARCHAR(20)  NOT NULL,
    event_timestamp  TIMESTAMPTZ  NOT NULL,
    recorded_at      TIMESTAMPTZ  NOT NULL,
    payload          TEXT
);

CREATE INDEX idx_audit_event_order ON audit_event (order_id);
CREATE INDEX idx_audit_event_actor ON audit_event (actor_id);
CREATE INDEX idx_audit_event_ts ON audit_event (event_timestamp);
CREATE INDEX idx_audit_event_type ON audit_event (event_type);

-- RF-15: inmutabilidad garantizada a nivel de base de datos. Ni un bug
-- de la aplicacion ni un acceso directo con un cliente SQL pueden
-- modificar o borrar un registro ya persistido.
CREATE OR REPLACE FUNCTION audit_event_block_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_event es de solo lectura: % no esta permitido (id=%)',
        TG_OP, COALESCE(OLD.id, NULL);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_event_no_update
    BEFORE UPDATE ON audit_event
    FOR EACH ROW
    EXECUTE FUNCTION audit_event_block_mutation();

CREATE TRIGGER trg_audit_event_no_delete
    BEFORE DELETE ON audit_event
    FOR EACH ROW
    EXECUTE FUNCTION audit_event_block_mutation();
