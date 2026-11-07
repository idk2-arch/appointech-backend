-- Una solicitud especial también puede originarse cuando hay técnicos que cubren la ubicación pero ninguno tiene
-- bloques libres (SIN_HORARIOS). V16 limitaba el motivo a dos valores; se amplía la restricción sin tocar los datos.
ALTER TABLE cita DROP CONSTRAINT ck_cita_motivo_solicitud_especial;
ALTER TABLE cita
    ADD CONSTRAINT ck_cita_motivo_solicitud_especial
        CHECK (motivo_solicitud_especial IS NULL
               OR motivo_solicitud_especial IN ('SIN_TECNICOS_ESPECIALIDAD', 'FUERA_DE_COBERTURA', 'SIN_HORARIOS'));
