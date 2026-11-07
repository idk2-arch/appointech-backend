package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.FranjaTrabajoConflictoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.FranjaTrabajoRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reglas compartidas por crear, cambiar, borrar y listar las franjas de trabajo: duración de la cita, cruces con otras
 * franjas y con las no disponibilidades del técnico, citas ya agendadas y ocupación de los bloques.
 */
@Component
public class ReglasFranjasTrabajo {

    /** Estados en los que una cita ocupa la agenda del técnico. */
    static final List<EstadoCita> ESTADOS_QUE_OCUPAN =
            List.of(EstadoCita.CONFIRMADA, EstadoCita.EN_CAMINO, EstadoCita.EN_PROCESO);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConfiguracionFranjasRepositoryPort configuracionPort;
    private final FranjaTrabajoRepositoryPort franjaRepositoryPort;
    private final BloqueIndisponibilidadRepositoryPort bloqueRepositoryPort;
    private final ConsultaCitasDelTecnicoPort consultaCitasPort;
    private final Clock reloj;

    public ReglasFranjasTrabajo(ConfiguracionFranjasRepositoryPort configuracionPort,
                                FranjaTrabajoRepositoryPort franjaRepositoryPort,
                                BloqueIndisponibilidadRepositoryPort bloqueRepositoryPort,
                                ConsultaCitasDelTecnicoPort consultaCitasPort, Clock reloj) {
        this.configuracionPort = configuracionPort;
        this.franjaRepositoryPort = franjaRepositoryPort;
        this.bloqueRepositoryPort = bloqueRepositoryPort;
        this.consultaCitasPort = consultaCitasPort;
        this.reloj = reloj;
    }

    /** La duración de la cita: lo único de la configuración global que sigue vigente. */
    public int duracionBloqueMinutos() {
        return configuracionPort.obtenerActual().orElseGet(ConfiguracionFranjas::porDefecto).getDuracionBloqueMinutos();
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    /** Falla con 409 si la franja se cruza con otra del técnico o con una no disponibilidad suya; el mensaje dice con cuál. */
    public void exigirSinCruces(FranjaTrabajo franja) {
        franjaRepositoryPort.buscarDelTecnicoEnFecha(franja.tecnicoId(), franja.fecha()).stream()
                .filter(otra -> !otra.id().equals(franja.id()) && franja.seCruzaCon(otra))
                .findFirst()
                .ifPresent(otra -> {
                    throw new FranjaTrabajoConflictoException(
                            "La franja se cruza con otra franja del técnico: " + otra.descripcion());
                });
        List<BloqueIndisponibilidad> bloques =
                bloqueRepositoryPort.buscarDelTecnicoEntre(franja.tecnicoId(), franja.inicio(), franja.fin());
        if (!bloques.isEmpty()) {
            String detalle = bloques.stream()
                    .map(bloque -> "del " + bloque.getInicio().format(FECHA_HORA) + " al " + bloque.getFin().format(FECHA_HORA))
                    .collect(Collectors.joining("; "));
            throw new FranjaTrabajoConflictoException("La franja se cruza con una no disponibilidad del técnico ("
                    + detalle + "); no se creó la franja");
        }
    }

    /** Falla con 409 si hay citas agendadas dentro de la franja; no se cancela ni se mueve ninguna. */
    public void exigirSinCitas(FranjaTrabajo franja, String accion) {
        int citas = citasEnLaFranja(franja).size();
        if (citas > 0) {
            throw new FranjaTrabajoConflictoException("No se puede " + accion + " la franja " + franja.descripcion()
                    + ": ya tiene " + citas + (citas == 1 ? " cita agendada" : " citas agendadas")
                    + ". Cancélalas o reasígnalas antes");
        }
    }

    /** La franja con su número de bloques y cuántos están ocupados por citas agendadas. */
    public FranjaTrabajoConOcupacion conOcupacion(FranjaTrabajo franja) {
        return conOcupacion(franja, citasEnLaFranja(franja));
    }

    /** Igual que {@link #conOcupacion(FranjaTrabajo)}, con las citas del técnico ya consultadas (para listar sin una consulta por franja). */
    public FranjaTrabajoConOcupacion conOcupacion(FranjaTrabajo franja, List<Cita> citasDelTecnico) {
        List<FranjaTrabajo.Bloque> bloques = franja.bloques(duracionBloqueMinutos());
        long ocupados = bloques.stream().filter(bloque -> citasDelTecnico.stream().anyMatch(cita ->
                cita.getFechaHoraInicio().isBefore(bloque.fin()) && cita.getFechaHoraFin().isAfter(bloque.inicio()))).count();
        return new FranjaTrabajoConOcupacion(franja, bloques.size(), (int) ocupados);
    }

    /** Las citas agendadas del técnico entre dos momentos. */
    public List<Cita> citasAgendadas(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
        return consultaCitasPort.buscarPorEstadosEntre(tecnicoId, ESTADOS_QUE_OCUPAN, inicio, fin).stream()
                .filter(cita -> cita.getFechaHoraInicio() != null && cita.getFechaHoraFin() != null)
                .toList();
    }

    private List<Cita> citasEnLaFranja(FranjaTrabajo franja) {
        return citasAgendadas(franja.tecnicoId(), franja.inicio(), franja.fin());
    }
}
