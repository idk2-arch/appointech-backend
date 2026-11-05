package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cita;

import java.util.List;

public interface ConsultaCitasFinalizadasDelTecnicoPort {

    /** Las últimas citas FINALIZADAS del técnico, las más recientes primero, hasta el límite indicado. */
    List<Cita> buscarFinalizadas(Long tecnicoId, int limite);
}
