ALTER TABLE especialidad ADD COLUMN costo_referencia NUMERIC(12,2);
ALTER TABLE especialidad ADD CONSTRAINT ck_especialidad_costo CHECK (costo_referencia IS NULL OR costo_referencia > 0);

UPDATE especialidad SET costo_referencia = 80000 WHERE nombre = 'Lavadoras';
UPDATE especialidad SET costo_referencia = 100000 WHERE nombre = 'Neveras';
UPDATE especialidad SET costo_referencia = 70000 WHERE nombre = 'Estufas';
UPDATE especialidad SET costo_referencia = 80000 WHERE nombre = 'Secadoras';
UPDATE especialidad SET costo_referencia = 75000 WHERE nombre = 'Hornos';
UPDATE especialidad SET costo_referencia = 120000 WHERE nombre = 'Aires acondicionados';

ALTER TABLE pago ADD CONSTRAINT ck_pago_montos CHECK (
    (monto_cotizado IS NULL OR monto_cotizado > 0) AND (monto_final IS NULL OR monto_final > 0)
);

CREATE TABLE comprobante_pago (
    id BIGSERIAL PRIMARY KEY,
    pago_id BIGINT NOT NULL UNIQUE REFERENCES pago(id) ON DELETE CASCADE,
    emitido_en TIMESTAMP NOT NULL DEFAULT NOW()
);
