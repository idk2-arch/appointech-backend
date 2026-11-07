-- Franjas de trabajo creadas a mano por el administrador, por técnico y por fecha.
-- La jornada y el radio globales de configuracion_franjas dejan de usarse para calcular la disponibilidad
-- (se conservan en la tabla y en sus endpoints); sigue vigente la duración del bloque.
CREATE TABLE franja_tecnico (
    id BIGSERIAL PRIMARY KEY,
    tecnico_id BIGINT NOT NULL REFERENCES tecnico(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    creada_por BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_franja_tecnico_horas CHECK (hora_inicio < hora_fin)
);

CREATE INDEX idx_franja_tecnico_tecnico_fecha ON franja_tecnico (tecnico_id, fecha);

-- Radio de atención por técnico: se reutiliza tecnico.radio_cobertura_km (existe desde V1 y no se usaba desde V8).
-- Para no cambiar la cobertura actual, cada técnico recibe el radio global vigente (15 km si no hubiera configuración).
UPDATE tecnico
SET radio_cobertura_km = COALESCE((SELECT radio_maximo_km FROM configuracion_franjas ORDER BY id DESC LIMIT 1), 15);

ALTER TABLE tecnico
    ADD CONSTRAINT ck_tecnico_radio_cobertura CHECK (radio_cobertura_km BETWEEN 1 AND 100);

-- NO se crean franjas automáticamente: hasta que el administrador cree franjas, los técnicos existentes no ofrecen citas.
