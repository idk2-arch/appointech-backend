-- Notificaciones internas (la campana de la app). La tabla notificacion ya existe desde V1 (id, usuario_id, tipo,
-- mensaje, leida, creada_en) pero ningún código la usaba; aquí se completa con el título y la cita relacionada.

-- Título corto de cada aviso. Si hubiera filas antiguas reciben un título genérico antes de volverlo obligatorio.
ALTER TABLE notificacion ADD COLUMN titulo VARCHAR(120);
UPDATE notificacion SET titulo = 'Notificación' WHERE titulo IS NULL;
ALTER TABLE notificacion ALTER COLUMN titulo SET NOT NULL;

-- Cita a la que se refiere el aviso (opcional); si la cita se borra, el aviso se conserva sin ella.
ALTER TABLE notificacion ADD COLUMN cita_id BIGINT REFERENCES cita(id) ON DELETE SET NULL;

-- Sirve al listado (más recientes primero, con o sin filtro de no leídas) y al conteo de no leídas.
-- Reemplaza al índice (usuario_id, leida) de V1, que este nuevo cubre por completo.
CREATE INDEX idx_notificacion_usuario_leida_creada ON notificacion (usuario_id, leida, creada_en DESC);
DROP INDEX idx_notificacion_usuario;
