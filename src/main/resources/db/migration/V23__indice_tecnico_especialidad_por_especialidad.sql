-- La clave primaria de tecnico_especialidad es (tecnico_id, especialidad_id): sirve para buscar las especialidades de un
-- técnico, pero no para "técnicos de una especialidad" (consulta de cada cálculo de disponibilidad). Este índice cubre ese sentido.
CREATE INDEX IF NOT EXISTS idx_tecnico_especialidad_especialidad ON tecnico_especialidad (especialidad_id, tecnico_id);
