package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;

import java.time.LocalDate;
import java.time.LocalTime;

/** El administrador crea, cambia y borra las franjas de trabajo de un técnico. */
public interface AdministrarFranjasTrabajoUseCase {

    /** Crea la franja; falla si es inválida o si se cruza con otra franja, con una no disponibilidad o con citas. */
    FranjaTrabajoConOcupacion crear(String correoAdministrador, Long tecnicoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin);

    /** Cambia fecha u horas; se bloquea si la franja ya tiene citas agendadas. */
    FranjaTrabajoConOcupacion actualizar(String correoAdministrador, Long tecnicoId, Long franjaId,
                                         LocalDate fecha, LocalTime horaInicio, LocalTime horaFin);

    /** Borra la franja; se bloquea si ya tiene citas agendadas. */
    void eliminar(String correoAdministrador, Long tecnicoId, Long franjaId);
}
