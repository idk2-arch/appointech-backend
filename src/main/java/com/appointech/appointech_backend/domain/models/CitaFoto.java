package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/** Foto del electrodoméstico adjunta a una cita; clave es la clave del objeto en el almacenamiento. */
public record CitaFoto(Long id, Long citaId, String clave, LocalDateTime creadoEn) {

    public static final int MAXIMO_POR_CITA = 3;
}
