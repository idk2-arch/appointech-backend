package com.appointech.appointech_backend.domain.models;

import java.time.LocalTime;

/** Reglas de franjas (RF17/RF19). Vigente: duración del bloque. EN DESUSO (no afectan el cálculo): radio global y jornada. */
public record ReglasFranjas(int radioMaximoKm, int duracionBloqueMinutos,
                            LocalTime horaInicioJornada, LocalTime horaFinJornada) {

    public static ReglasFranjas de(ConfiguracionFranjas c) {
        return new ReglasFranjas(c.getRadioMaximoKm(), c.getDuracionBloqueMinutos(),
                c.getHoraInicioJornada(), c.getHoraFinJornada());
    }
}
