-- Sprint 5: cotización, costo final, pago y comprobante.

-- Costo de referencia (COP) por especialidad: base de la cotización. NULL = aún sin valor de referencia.
ALTER TABLE especialidad ADD COLUMN costo_referencia NUMERIC(12,2);
ALTER TABLE especialidad
    ADD CONSTRAINT ck_especialidad_costo_referencia CHECK (costo_referencia IS NULL OR costo_referencia > 0);

-- Valores iniciales solo para las especialidades que existen (V2 y V3); el administrador puede ajustarlos después.
UPDATE especialidad SET costo_referencia = 80000  WHERE nombre = 'Lavadoras';
UPDATE especialidad SET costo_referencia = 100000 WHERE nombre = 'Neveras';
UPDATE especialidad SET costo_referencia = 70000  WHERE nombre = 'Estufas';
UPDATE especialidad SET costo_referencia = 80000  WHERE nombre = 'Secadoras';
UPDATE especialidad SET costo_referencia = 75000  WHERE nombre = 'Hornos';
UPDATE especialidad SET costo_referencia = 120000 WHERE nombre = 'Aires acondicionados';

-- Pago: id de la transacción en la pasarela y contador de intentos en línea (la referencia de cada intento es nueva).
ALTER TABLE pago ADD COLUMN id_transaccion_externa VARCHAR(100);
ALTER TABLE pago ADD COLUMN intentos_pago INTEGER NOT NULL DEFAULT 0;

ALTER TABLE pago ADD CONSTRAINT ck_pago_monto_cotizado CHECK (monto_cotizado IS NULL OR monto_cotizado > 0);
ALTER TABLE pago ADD CONSTRAINT ck_pago_monto_final CHECK (monto_final IS NULL OR monto_final > 0);
ALTER TABLE pago ADD CONSTRAINT ck_pago_intentos CHECK (intentos_pago >= 0);
-- Un pago PAGADO siempre tiene monto, método y fecha.
ALTER TABLE pago ADD CONSTRAINT ck_pago_pagado_completo
    CHECK (estado <> 'PAGADO' OR (monto_final IS NOT NULL AND metodo IS NOT NULL AND pagado_en IS NOT NULL));

CREATE UNIQUE INDEX ux_pago_referencia_externa ON pago (referencia_externa) WHERE referencia_externa IS NOT NULL;

-- Comprobante: uno solo por pago (pago_id único); su número COMP-000001 se deriva del id.
CREATE TABLE comprobante_pago (
    id BIGSERIAL PRIMARY KEY,
    pago_id BIGINT NOT NULL UNIQUE REFERENCES pago(id) ON DELETE CASCADE,
    emitido_en TIMESTAMP NOT NULL DEFAULT NOW()
);
