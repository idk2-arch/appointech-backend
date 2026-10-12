package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadorDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Caso de uso: calcular las franjas libres de un día combinando especialidad, citas, bloques de indisponibilidad y cercanía.
 * <p>Historias de usuario:
 * <ul>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-12">SCRUM-12</a> - RF19 - Calcular franjas horarias disponibles</li>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-13">SCRUM-13</a> - RF03 - Mostrar franjas sugeridas al cliente</li>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-11">SCRUM-11</a> - RF27 - Filtrar técnicos por especialidad</li>
 * </ul>
 */
@Service
public class CalcularFranjasDisponiblesUseCaseImpl implements CalcularFranjasDisponiblesUseCase {

    private final FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase;
    private final CitaRepositoryPort citaRepositoryPort;
    private final BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort;
    private final ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort;
    private final CalculadorDistanciaPort calculadorDistanciaPort;

    public CalcularFranjasDisponiblesUseCaseImpl(
            FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
            CitaRepositoryPort citaRepositoryPort,
            BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
            ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort,
            CalculadorDistanciaPort calculadorDistanciaPort) {
        this.filtrarTecnicosPorEspecialidadUseCase = filtrarTecnicosPorEspecialidadUseCase;
        this.citaRepositoryPort = citaRepositoryPort;
        this.bloqueIndisponibilidadRepositoryPort = bloqueIndisponibilidadRepositoryPort;
        this.configuracionFranjasRepositoryPort = configuracionFranjasRepositoryPort;
        this.calculadorDistanciaPort = calculadorDistanciaPort;
    }

    @Override
    public List<FranjaDisponible> calcular(Long especialidadId, LocalDate fecha, Double latitud, Double longitud) {
        LocalDateTime ahora = LocalDateTime.now();
        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new FranjaInvalidaException("La fecha no puede ser anterior a hoy");
        }

        ConfiguracionFranjas configuracion = configuracionFranjasRepositoryPort.obtenerActual()
                .orElseGet(() -> new ConfiguracionFranjas(null, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        Map<LocalDateTime, FranjaDisponible> mejorFranjaPorHorario = new TreeMap<>();

        for (Tecnico tecnico : filtrarTecnicosPorEspecialidadUseCase.filtrar(especialidadId)) {
            double distanciaKm = 0.0;
            if (tecnico.getLatitudBase() != null && tecnico.getLongitudBase() != null) {
                distanciaKm = calculadorDistanciaPort.calcularKm(
                        tecnico.getLatitudBase(), tecnico.getLongitudBase(), latitud, longitud);
                int radioDelTecnico = tecnico.getRadioCoberturaKm() != null
                        ? tecnico.getRadioCoberturaKm()
                        : configuracion.getRadioMaximoKm();
                int radioPermitido = Math.min(radioDelTecnico, configuracion.getRadioMaximoKm());
                if (distanciaKm > radioPermitido) {
                    continue;
                }
            }

            List<Cita> citasDelDia = citaRepositoryPort.buscarActivasDelTecnicoEntre(tecnico.getId(), inicioDia, finDia);
            List<BloqueIndisponibilidad> bloquesDelDia =
                    bloqueIndisponibilidadRepositoryPort.buscarDelTecnicoEntre(tecnico.getId(), inicioDia, finDia);

            LocalDateTime inicioBloque = fecha.atTime(configuracion.getHoraInicioJornada());
            LocalDateTime finJornada = fecha.atTime(configuracion.getHoraFinJornada());

            while (!inicioBloque.plusMinutes(configuracion.getDuracionBloqueMinutos()).isAfter(finJornada)) {
                LocalDateTime finBloque = inicioBloque.plusMinutes(configuracion.getDuracionBloqueMinutos());

                if (inicioBloque.isAfter(ahora) && estaLibre(inicioBloque, finBloque, citasDelDia, bloquesDelDia)) {
                    FranjaDisponible candidata = new FranjaDisponible(tecnico.getId(), inicioBloque, finBloque, distanciaKm);
                    FranjaDisponible actual = mejorFranjaPorHorario.get(inicioBloque);
                    if (actual == null || candidata.getDistanciaKm() < actual.getDistanciaKm()) {
                        mejorFranjaPorHorario.put(inicioBloque, candidata);
                    }
                }
                inicioBloque = finBloque;
            }
        }

        List<FranjaDisponible> resultado = new ArrayList<>(mejorFranjaPorHorario.values());
        resultado.sort(Comparator.comparing(FranjaDisponible::getInicio));
        return resultado;
    }

    private boolean estaLibre(LocalDateTime inicio, LocalDateTime fin,
                              List<Cita> citas, List<BloqueIndisponibilidad> bloques) {
        boolean cruzaCita = citas.stream().anyMatch(cita ->
                cita.getFechaHoraInicio() != null && cita.getFechaHoraFin() != null
                        && cita.getFechaHoraInicio().isBefore(fin) && cita.getFechaHoraFin().isAfter(inicio));
        boolean cruzaBloque = bloques.stream().anyMatch(bloque ->
                bloque.getInicio().isBefore(fin) && bloque.getFin().isAfter(inicio));
        return !cruzaCita && !cruzaBloque;
    }
}
