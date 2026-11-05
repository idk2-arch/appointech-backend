package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CitaFinalizadaConPago;

import java.util.List;

public interface ConsultarCitasFinalizadasDelTecnicoUseCase {
    /** Últimas citas finalizadas del técnico, las más recientes primero, con la situación de su cobro. */
    List<CitaFinalizadaConPago> consultar(String correoTecnico);
}
