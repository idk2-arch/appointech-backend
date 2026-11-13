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
import com.appointech.appointech_backend.domain.ports.out.FranjaTrabajoRepositoryPort;
import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Caso de uso: calcular las franjas libres de un día combinando especialidad, franjas de trabajo que el administrador abrió,
 * citas, bloques de indisponibilidad y cercanía (con el radio de atención de cada técnico). La jornada y el radio globales
 * ya no se usan aquí; solo la duración del bloque.
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
    private final FranjaTrabajoRepositoryPort franjaTrabajoRepositoryPort;
    private final CalculadoraDistanciaPort calculadoraDistanciaPort;
    private final ContactoAdminConfig contactoAdminConfig;

    public CalcularFranjasDisponiblesUseCaseImpl(
            FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
            CitaRepositoryPort citaRepositoryPort,
            BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
            ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort,
            FranjaTrabajoRepositoryPort franjaTrabajoRepositoryPort,
            CalculadoraDistanciaPort calculadoraDistanciaPort) {
        this(filtrarTecnicosPorEspecialidadUseCase, citaRepositoryPort, bloqueIndisponibilidadRepositoryPort,
                configuracionFranjasRepositoryPort, franjaTrabajoRepositoryPort, calculadoraDistanciaPort, new ContactoAdminConfig(null));
    }

    @Autowired
    public CalcularFranjasDisponiblesUseCaseImpl(
            FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
            CitaRepositoryPort citaRepositoryPort,
            BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
            ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort,
            FranjaTrabajoRepositoryPort franjaTrabajoRepositoryPort,
            CalculadoraDistanciaPort calculadoraDistanciaPort,
            ContactoAdminConfig contactoAdminConfig) {
        this.contactoAdminConfig = contactoAdminConfig;
        this.filtrarTecnicosPorEspecialidadUseCase = filtrarTecnicosPorEspecialidadUseCase;
        this.citaRepositoryPort = citaRepositoryPort;
        this.bloqueIndisponibilidadRepositoryPort = bloqueIndisponibilidadRepositoryPort;
        this.configuracionFranjasRepositoryPort = configuracionFranjasRepositoryPort;
        this.franjaTrabajoRepositoryPort = franjaTrabajoRepositoryPort;
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
                .orElseGet(ConfiguracionFranjas::porDefecto);

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

        // Cobertura: cada técnico se evalúa con SU radio de atención (el radio global ya no interviene).
        Map<Long, Double> distanciaDeLosQueCubren = new LinkedHashMap<>();
        Map<Long, Tecnico> tecnicosQueCubren = new LinkedHashMap<>();
        for (int i = 0; i < candidatos.size(); i++) {
            Tecnico tecnico = candidatos.get(i);
            double distanciaKm = distancias.get(i).kilometros();
            if (tecnico.getRadioCoberturaKm() != null && distanciaKm <= tecnico.getRadioCoberturaKm()) {
                distanciaDeLosQueCubren.put(tecnico.getId(), distanciaKm);
                tecnicosQueCubren.put(tecnico.getId(), tecnico);
            }
        }
        if (tecnicosQueCubren.isEmpty()) {
            return fueraDeCobertura(configuracion);
        }

        // Disponibilidad: solo lo que el administrador abrió ese día; un técnico sin franjas no ofrece nada.
        int duracionMinutos = configuracion.getDuracionBloqueMinutos();
        Map<Long, List<FranjaTrabajo>> franjasPorTecnico = franjaTrabajoRepositoryPort
                .buscarDeTecnicosEnFecha(tecnicosQueCubren.keySet(), fecha).stream()
                .collect(Collectors.groupingBy(FranjaTrabajo::tecnicoId));
        Map<Long, List<Cita>> citasPorTecnico = franjasPorTecnico.isEmpty() ? Map.of() : citaRepositoryPort
                .buscarActivasDeTecnicosEntre(franjasPorTecnico.keySet(), inicioDia, finDia).stream()
                .collect(Collectors.groupingBy(Cita::getTecnicoId));
        Map<Long, List<BloqueIndisponibilidad>> bloquesPorTecnico = franjasPorTecnico.isEmpty() ? Map.of() :
                bloqueIndisponibilidadRepositoryPort.buscarDeTecnicosEntre(franjasPorTecnico.keySet(), inicioDia, finDia)
                        .stream().collect(Collectors.groupingBy(BloqueIndisponibilidad::getTecnicoId));
        for (Map.Entry<Long, List<FranjaTrabajo>> entrada : franjasPorTecnico.entrySet()) {
            Tecnico tecnico = tecnicosQueCubren.get(entrada.getKey());
            double distanciaKm = distanciaDeLosQueCubren.get(tecnico.getId());
            List<Cita> citasDelDia = citasPorTecnico.getOrDefault(tecnico.getId(), List.of());
            List<BloqueIndisponibilidad> bloquesDelDia = bloquesPorTecnico.getOrDefault(tecnico.getId(), List.of());

            for (FranjaTrabajo franja : entrada.getValue()) {
                for (FranjaTrabajo.Bloque bloque : franja.bloques(duracionMinutos)) {
                    if (bloque.inicio().isAfter(ahora) && estaLibre(bloque.inicio(), bloque.fin(), citasDelDia, bloquesDelDia)) {
                        FranjaDisponible candidata = new FranjaDisponible(tecnico.getId(), bloque.inicio(), bloque.fin(), distanciaKm,
                                tecnico.getUsuario().getNombre(), tecnico.getUsuario().getApellido());
                        FranjaDisponible actual = mejorFranjaPorHorario.get(bloque.inicio());
                        if (actual == null || candidata.getDistanciaKm() < actual.getDistanciaKm()) {
                            mejorFranjaPorHorario.put(bloque.inicio(), candidata);
                        }
                    }
                }
            }
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
