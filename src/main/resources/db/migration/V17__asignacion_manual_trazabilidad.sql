-- RF15: trazabilidad de la asignacion manual de tecnico por el administrador (quien y cuando).
ALTER TABLE cita
    ADD COLUMN asignacion_manual BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN asignada_por BIGINT,
    ADD COLUMN asignada_en TIMESTAMP,
    ADD CONSTRAINT fk_cita_asignada_por FOREIGN KEY (asignada_por) REFERENCES usuario(id);

-- Las solicitudes manuales ya asignadas antes de esta migracion se marcan, pero quien y cuando
-- no se conocen: quedan en NULL (no se inventan datos).
UPDATE cita SET asignacion_manual = TRUE
WHERE agendamiento_manual AND tecnico_id IS NOT NULL;
