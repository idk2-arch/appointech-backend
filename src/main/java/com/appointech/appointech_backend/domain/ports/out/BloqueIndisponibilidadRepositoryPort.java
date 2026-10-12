package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueIndisponibilidadRepositoryPort {
    List<BloqueIndisponibilidad> buscarDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin);
}
