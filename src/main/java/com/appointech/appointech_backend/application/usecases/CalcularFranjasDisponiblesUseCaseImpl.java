package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.ContactoAdminConfig;
import com.appointech.appointech_backend.domain.models.MotivoSinFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(CalcularFranjasDisponiblesUseCaseImpl.class);

    private final FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase;
    private final CitaRepositoryPort citaRepositoryPort;
    private final BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort;
    private final ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort;
    private final CalculadoraDistanciaPort calculadoraDistanciaPort;
    private final ContactoAdminConfig contactoAdminConfig;

    public CalcularFranjasDisponiblesUseCaseImpl(
            FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
            CitaRepositoryPort citaRepositoryPort,
            BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
            ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort,
            CalculadoraDistanciaPort calculadoraDistanciaPort) {
        this(filtrarTecnicosPorEspecialidadUseCase, citaRepositoryPort, bloqueIndisponibilidadRepositoryPort,
                configuracionFranjasRepositoryPort, calculadoraDistanciaPort, new ContactoAdminConfig(null));
    }

    @Autowired
    public CalcularFranjasDisponiblesUseCaseImpl(
            FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
            CitaRepositoryPort citaRepositoryPort,
            BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
            ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort,
            CalculadoraDistanciaPort calculadoraDistanciaPort,
            ContactoAdminConfig contactoAdminConfig) {
        this.contactoAdminConfig = contactoAdminConfig;
        this.filtrarTecnicosPorEspecialidadUseCase = filtrarTecnicosPorEspecialidadUseCase;
        this.citaRepositoryPort = citaRepositoryPort;
        this.bloqueIndisponibilidadRepositoryPort = bloqueIndisponibilidadRepositoryPort;
        this.configuracionFranjasRepositoryPort = configuracionFranjasRepositoryPort;
        this.calculadoraDistanciaPort = calculadoraDistanciaPort;
    }

    @Override
    public List<FranjaDisponible> calcular(Long especialidadId, LocalDate fecha, Double latitud, Double longitud) {
        return calcularConMotivo(especialidadId, fecha, latitud, longitud).franjas();
    }

    @Override
    public ResultadoFranjas calcularConMotivo(Long especialidadId, LocalDate fecha, Double latitud, Double longitud) {
        LocalDateTime ahora = LocalDateTime.now();
        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new FranjaInvalidaException("La fecha no puede ser anterior a hoy");
        }

        ConfiguracionFranjas configuracion = configuracionFranjasRepositoryPort.obtenerActual()
                .orElseGet(() -> new ConfiguracionFranjas(null, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        Coordenadas origen = Coordenadas.de(latitud, longitud);

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        Map<LocalDateTime, FranjaDisponible> mejorFranjaPorHorario = new TreeMap<>();

        List<Tecnico> conEspecialidad = filtrarTecnicosPorEspecialidadUseCase.filtrar(especialidadId);
        if (conEspecialidad.isEmpty()) {
            return new ResultadoFranjas(List.of(), MotivoSinFranjas.SIN_TECNICOS_ESPECIALIDAD);
        }

        List<Tecnico> candidatos = new ArrayList<>();
        List<Coordenadas> ubicaciones = new ArrayList<>();
        for (Tecnico tecnico : conEspecialidad) {
            Coordenadas base = coordenadasBase(tecnico);
            if (base != null) {
                candidatos.add(tecnico);
                ubicaciones.add(base);
            }
        }
        if (candidatos.isEmpty()) {
            // Ningún técnico con la especialidad tiene una base utilizable: no se puede llegar a nadie.
            return fueraDeCobertura(configuracion);
        }

        List<Distancia> distancias = calculadoraDistanciaPort.calcular(origen, ubicaciones);
        if (distancias.size() != candidatos.size()) {
            throw new IllegalStateException("El calculador de distancias devolvió una cantidad inesperada de resultados");
        }

        boolean algunoDentroDeCobertura = false;
        for (int i = 0; i < candidatos.size(); i++) {
            Tecnico tecnico = candidatos.get(i);
            double distanciaKm = distancias.get(i).kilometros();
            // Límite único del sistema: el radio por técnico ya no se usa.
            if (distanciaKm > configuracion.getRadioMaximoKm()) {
                continue;
            }
            algunoDentroDeCobertura = true;

            List<Cita> citasDelDia = citaRepositoryPort.buscarActivasDelTecnicoEntre(tecnico.getId(), inicioDia, finDia);
            List<BloqueIndisponibilidad> bloquesDelDia =
                    bloqueIndisponibilidadRepositoryPort.buscarDelTecnicoEntre(tecnico.getId(), inicioDia, finDia);

            LocalDateTime inicioBloque = fecha.atTime(configuracion.getHoraInicioJornada());
            LocalDateTime finJornada = fecha.atTime(configuracion.getHoraFinJornada());

            while (!inicioBloque.plusMinutes(configuracion.getDuracionBloqueMinutos()).isAfter(finJornada)) {
                LocalDateTime finBloque = inicioBloque.plusMinutes(configuracion.getDuracionBloqueMinutos());

                if (inicioBloque.isAfter(ahora) && estaLibre(inicioBloque, finBloque, citasDelDia, bloquesDelDia)) {
                    FranjaDisponible candidata = new FranjaDisponible(tecnico.getId(), inicioBloque, finBloque, distanciaKm,
                            tecnico.getUsuario().getNombre(), tecnico.getUsuario().getApellido());
                    FranjaDisponible actual = mejorFranjaPorHorario.get(inicioBloque);
                    if (actual == null || candidata.getDistanciaKm() < actual.getDistanciaKm()) {
                        mejorFranjaPorHorario.put(inicioBloque, candidata);
                    }
                }
                inicioBloque = finBloque;
            }
        }

        if (!algunoDentroDeCobertura) {
            return fueraDeCobertura(configuracion);
        }

        List<FranjaDisponible> resultado = new ArrayList<>(mejorFranjaPorHorario.values());
        resultado.sort(Comparator.comparing(FranjaDisponible::getInicio));
        return new ResultadoFranjas(resultado, resultado.isEmpty() ? MotivoSinFranjas.SIN_HORARIOS : null);
    }

    private ResultadoFranjas fueraDeCobertura(ConfiguracionFranjas configuracion) {
        return new ResultadoFranjas(List.of(), MotivoSinFranjas.FUERA_DE_COBERTURA,
                contactoAdminConfig.resolver(configuracion.getTelefonoWhatsappAdmin()));
    }

    private Coordenadas coordenadasBase(Tecnico tecnico) {
        try {
            Coordenadas base = Coordenadas.opcional(tecnico.getLatitudBase(), tecnico.getLongitudBase());
            if (base == null) {
                log.warn("El técnico {} no tiene coordenadas base; no se ofrece", tecnico.getId());
            }
            return base;
        } catch (CoordenadasInvalidasException e) {
            log.warn("El técnico {} tiene coordenadas base inválidas; no se ofrece", tecnico.getId());
            return null;
        }
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
