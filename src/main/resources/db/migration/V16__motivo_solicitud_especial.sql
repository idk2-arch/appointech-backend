-- RF28: motivo por el que una solicitud queda para atencion manual del administrador (campo aparte; el estado
-- de la cita no cambia, sigue PENDIENTE). NULL = solicitud normal.
ALTER TABLE cita
    ADD COLUMN motivo_solicitud_especial VARCHAR(40),
    ADD CONSTRAINT ck_cita_motivo_solicitud_especial
        CHECK (motivo_solicitud_especial IS NULL
               OR motivo_solicitud_especial IN ('SIN_TECNICOS_ESPECIALIDAD', 'FUERA_DE_COBERTURA'));

-- Las solicitudes manuales existentes se crearon solo cuando la direccion estaba fuera de cobertura.
UPDATE cita SET motivo_solicitud_especial = 'FUERA_DE_COBERTURA'
WHERE agendamiento_manual AND motivo_solicitud_especial IS NULL;

CREATE INDEX idx_cita_motivo_solicitud_especial ON cita(motivo_solicitud_especial)
    WHERE motivo_solicitud_especial IS NOT NULL;
