CREATE TABLE log_sistema (
    id BIGSERIAL PRIMARY KEY,
    nivel VARCHAR(10) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    origen VARCHAR(100) NOT NULL,
    usuario VARCHAR(150),
    mensaje TEXT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_log_nivel CHECK (nivel IN ('DEBUG', 'INFO', 'WARN', 'ERROR'))
);

CREATE INDEX idx_log_creado_en ON log_sistema(creado_en);
CREATE INDEX idx_log_nivel_tipo ON log_sistema(nivel, tipo);
