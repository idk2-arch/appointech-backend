-- Agendamiento manual: una cita PENDIENTE sin tecnico ni horario (ya permitido por el esquema) marcada como manual,
-- para distinguirla de una solicitud normal que espera que el cliente elija franja.
ALTER TABLE cita
    ADD COLUMN agendamiento_manual BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN telefono_contacto VARCHAR(20);

CREATE INDEX idx_cita_manual_pendiente ON cita(estado) WHERE agendamiento_manual;
