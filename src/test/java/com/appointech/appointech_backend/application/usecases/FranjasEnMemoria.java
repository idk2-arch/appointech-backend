package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.ports.out.FranjaTrabajoRepositoryPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Franjas de trabajo en memoria para las pruebas de casos de uso (sin base de datos). */
class FranjasEnMemoria implements FranjaTrabajoRepositoryPort {

    final List<FranjaTrabajo> franjas = new ArrayList<>();
    private long siguienteId = 1;

    /** Abre una franja de trabajo y la devuelve. */
    FranjaTrabajo abrir(Long tecnicoId, LocalDate fecha, int horaInicio, int horaFin) {
        return guardar(new FranjaTrabajo(null, tecnicoId, fecha, LocalTime.of(horaInicio, 0), LocalTime.of(horaFin, 0),
                1L, LocalDateTime.of(2030, 1, 1, 0, 0)));
    }

    @Override
    public FranjaTrabajo guardar(FranjaTrabajo franja) {
        FranjaTrabajo conId = franja.id() != null ? franja
                : new FranjaTrabajo(siguienteId++, franja.tecnicoId(), franja.fecha(), franja.horaInicio(), franja.horaFin(),
                franja.creadaPorId(), franja.creadaEn());
        franjas.removeIf(f -> f.id().equals(conId.id()));
        franjas.add(conId);
        return conId;
    }

    @Override
    public Optional<FranjaTrabajo> buscarDelTecnico(Long franjaId, Long tecnicoId) {
        return franjas.stream().filter(f -> f.id().equals(franjaId) && f.tecnicoId().equals(tecnicoId)).findFirst();
    }

    @Override
    public List<FranjaTrabajo> buscarDelTecnicoEnFecha(Long tecnicoId, LocalDate fecha) {
        return franjas.stream().filter(f -> f.tecnicoId().equals(tecnicoId) && f.fecha().equals(fecha))
                .sorted(Comparator.comparing(FranjaTrabajo::horaInicio)).toList();
    }

    @Override
    public List<FranjaTrabajo> buscarDelTecnicoEntre(Long tecnicoId, LocalDate desde, LocalDate hasta) {
        return franjas.stream().filter(f -> f.tecnicoId().equals(tecnicoId) && !f.fecha().isBefore(desde) && !f.fecha().isAfter(hasta))
                .sorted(Comparator.comparing(FranjaTrabajo::fecha).thenComparing(FranjaTrabajo::horaInicio)).toList();
    }

    @Override
    public List<FranjaTrabajo> buscarDeTecnicosEnFecha(Collection<Long> tecnicoIds, LocalDate fecha) {
        return franjas.stream().filter(f -> tecnicoIds.contains(f.tecnicoId()) && f.fecha().equals(fecha)).toList();
    }

    @Override
    public void eliminar(Long franjaId) {
        franjas.removeIf(f -> f.id().equals(franjaId));
    }
}
