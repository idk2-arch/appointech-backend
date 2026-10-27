package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CitaConCliente;

import java.time.LocalDate;
import java.util.List;

public interface ConsultarCitasDelTecnicoUseCase {
    List<CitaConCliente> consultar(String correoTecnico, LocalDate fecha);
}
