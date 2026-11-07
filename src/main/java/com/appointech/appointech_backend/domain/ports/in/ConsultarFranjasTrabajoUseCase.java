package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;

import java.time.LocalDate;
import java.util.List;

public interface ConsultarFranjasTrabajoUseCase {

    /**
     * Las franjas del técnico entre las fechas dadas, por fecha y hora. Sin fechas: desde hoy y los 30 días siguientes.
     * El rango no puede superar 90 días ni tener el inicio posterior al fin.
     */
    List<FranjaTrabajoConOcupacion> listar(Long tecnicoId, LocalDate desde, LocalDate hasta);
}
