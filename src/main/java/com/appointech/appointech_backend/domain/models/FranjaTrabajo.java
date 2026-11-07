package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.FranjaTrabajoInvalidaException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana de trabajo de un técnico en una fecha concreta, creada por el administrador (por ejemplo 08:00-12:00).
 * El sistema la divide en citas reservables de la duración global del bloque, empezando en la hora de inicio.
 */
public record FranjaTrabajo(Long id, Long tecnicoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                            Long creadaPorId, LocalDateTime creadaEn) {

    /** Un bloque reservable dentro de la franja. */
    public record Bloque(LocalDateTime inicio, LocalDateTime fin) {
    }

    /**
     * Valida los datos de una franja nueva o editada y devuelve la franja (sin id si es nueva). Reglas: todo obligatorio,
     * inicio anterior al fin, cabe al menos una cita de {@code duracionBloqueMinutos}, la fecha no es pasada y, si es hoy,
     * la hora de inicio no es anterior a la actual.
     */
    public static FranjaTrabajo validada(Long id, Long tecnicoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                         int duracionBloqueMinutos, LocalDateTime ahora, Long creadaPorId, LocalDateTime creadaEn) {
        if (fecha == null || horaInicio == null || horaFin == null) {
            throw new FranjaTrabajoInvalidaException("La fecha, la hora de inicio y la hora de fin son obligatorias");
        }
        if (!horaInicio.isBefore(horaFin)) {
            throw new FranjaTrabajoInvalidaException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (Duration.between(horaInicio, horaFin).toMinutes() < duracionBloqueMinutos) {
            throw new FranjaTrabajoInvalidaException("La franja debe durar al menos " + duracionBloqueMinutos
                    + " minutos, que es lo que dura una cita");
        }
        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new FranjaTrabajoInvalidaException("La fecha de la franja no puede ser anterior a hoy");
        }
        if (fecha.equals(ahora.toLocalDate()) && horaInicio.isBefore(ahora.toLocalTime())) {
            throw new FranjaTrabajoInvalidaException("La hora de inicio no puede ser anterior a la hora actual");
        }
        return new FranjaTrabajo(id, tecnicoId, fecha, horaInicio, horaFin, creadaPorId, creadaEn);
    }

    public LocalDateTime inicio() {
        return fecha.atTime(horaInicio);
    }

    public LocalDateTime fin() {
        return fecha.atTime(horaFin);
    }

    /** True si las dos franjas son de la misma fecha y comparten algún minuto (que una termine cuando la otra empieza no es cruce). */
    public boolean seCruzaCon(FranjaTrabajo otra) {
        return fecha.equals(otra.fecha) && horaInicio.isBefore(otra.horaFin) && otra.horaInicio.isBefore(horaFin);
    }

    /**
     * Las citas que caben, una tras otra desde la hora de inicio; si al final sobra menos tiempo que una cita, ese sobrante
     * se descarta.
     */
    public List<Bloque> bloques(int duracionBloqueMinutos) {
        List<Bloque> bloques = new ArrayList<>();
        LocalDateTime inicioBloque = inicio();
        LocalDateTime finDeLaFranja = fin();
        while (!inicioBloque.plusMinutes(duracionBloqueMinutos).isAfter(finDeLaFranja)) {
            LocalDateTime finBloque = inicioBloque.plusMinutes(duracionBloqueMinutos);
            bloques.add(new Bloque(inicioBloque, finBloque));
            inicioBloque = finBloque;
        }
        return bloques;
    }

    /** True si [inicio, fin) es exactamente uno de los bloques que esta franja ofrece. */
    public boolean ofreceBloque(LocalDateTime inicioBuscado, LocalDateTime finBuscado, int duracionBloqueMinutos) {
        return bloques(duracionBloqueMinutos).stream()
                .anyMatch(bloque -> bloque.inicio().equals(inicioBuscado) && bloque.fin().equals(finBuscado));
    }

    /** Texto corto para los mensajes de error, por ejemplo "2030-05-06 de 08:00 a 12:00". */
    public String descripcion() {
        return fecha + " de " + horaInicio + " a " + horaFin;
    }
}
