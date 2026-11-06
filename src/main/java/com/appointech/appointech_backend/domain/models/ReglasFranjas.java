package com.appointech.appointech_backend.domain.models;

import java.time.LocalTime;

/** Reglas con las que se calculan las franjas (RF19): radio, duración del bloque y jornada. */
public record ReglasFranjas(int radioMaximoKm, int duracionBloqueMinutos,
                            LocalTime horaInicioJornada, LocalTime horaFinJornada) {

    public static ReglasFranjas de(ConfiguracionFranjas c) {
        return new ReglasFranjas(c.getRadioMaximoKm(), c.getDuracionBloqueMinutos(),
                c.getHoraInicioJornada(), c.getHoraFinJornada());
    }
}
