-- Limite unico de distancia para todo el sistema: configuracion_franjas.radio_maximo_km
-- (editable por el administrador). El radio por tecnico deja de usarse pero la columna se conserva.
INSERT INTO configuracion_franjas (duracion_bloque_minutos, radio_maximo_km, hora_inicio_jornada, hora_fin_jornada)
SELECT 120, 15, '08:00', '18:00'
WHERE NOT EXISTS (SELECT 1 FROM configuracion_franjas);

ALTER TABLE configuracion_franjas
    ADD CONSTRAINT chk_configuracion_franjas_radio_maximo CHECK (radio_maximo_km BETWEEN 1 AND 100);
