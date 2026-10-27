package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;

import java.time.LocalDateTime;
import java.util.List;

public interface GestionBloquesPort {

    BloqueIndisponibilidad guardar(BloqueIndisponibilidad bloque);

    /** Bloques del técnico que terminan después de {@code desde} y, si {@code hasta} no es null, empiezan antes de él. */
    List<BloqueIndisponibilidad> listarDelTecnico(Long tecnicoId, LocalDateTime desde, LocalDateTime hasta);

    /** @return true si existía un bloque con ese id y pertenecía al técnico, y fue eliminado */
    boolean eliminarDelTecnico(Long bloqueId, Long tecnicoId);
}
