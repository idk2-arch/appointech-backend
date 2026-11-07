package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.FranjaTrabajo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FranjaTrabajoRepositoryPort {

    FranjaTrabajo guardar(FranjaTrabajo franja);

    /** Solo si la franja existe y es de ese técnico. */
    Optional<FranjaTrabajo> buscarDelTecnico(Long franjaId, Long tecnicoId);

    /** Las franjas del técnico en esa fecha, por hora de inicio. */
    List<FranjaTrabajo> buscarDelTecnicoEnFecha(Long tecnicoId, LocalDate fecha);

    /** Las franjas del técnico entre las dos fechas (ambas incluidas), por fecha y hora de inicio. */
    List<FranjaTrabajo> buscarDelTecnicoEntre(Long tecnicoId, LocalDate desde, LocalDate hasta);

    /** Las franjas de varios técnicos en una fecha, para calcular la disponibilidad sin una consulta por técnico. */
    List<FranjaTrabajo> buscarDeTecnicosEnFecha(Collection<Long> tecnicoIds, LocalDate fecha);

    void eliminar(Long franjaId);
}
