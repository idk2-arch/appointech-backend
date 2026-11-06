-- RF17: la base tambien protege las reglas de franjas editables por el administrador.
-- El radio (1 a 100 km) ya tiene su CHECK desde V8. Los valores actuales (120 min, 08:00-18:00) cumplen todo.
ALTER TABLE configuracion_franjas
    ADD CONSTRAINT chk_configuracion_franjas_duracion CHECK (duracion_bloque_minutos BETWEEN 15 AND 480),
    ADD CONSTRAINT chk_configuracion_franjas_jornada CHECK (hora_inicio_jornada < hora_fin_jornada),
    ADD CONSTRAINT chk_configuracion_franjas_bloque_en_jornada
        CHECK (duracion_bloque_minutos <= EXTRACT(EPOCH FROM (hora_fin_jornada - hora_inicio_jornada)) / 60);
