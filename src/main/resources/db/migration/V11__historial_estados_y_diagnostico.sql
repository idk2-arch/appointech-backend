-- Sprint 4: historial de estados de la cita y recomendaciones del diagnostico.
-- cita.diagnostico y cita.actualizada_en ya existen (V1) y se reutilizan.
ALTER TABLE cita
    ADD COLUMN recomendaciones TEXT,
    ADD COLUMN fecha_diagnostico TIMESTAMP;

CREATE TABLE cita_historial_estado (
                                       id BIGSERIAL PRIMARY KEY,
                                       cita_id BIGINT NOT NULL REFERENCES cita(id) ON DELETE CASCADE,
                                       estado_anterior estado_cita,
                                       estado_nuevo estado_cita NOT NULL,
                                       fecha TIMESTAMP NOT NULL,
                                       actor VARCHAR(255) NOT NULL,
                                       motivo VARCHAR(500)
);

CREATE INDEX idx_cita_historial_cita ON cita_historial_estado(cita_id, fecha);
