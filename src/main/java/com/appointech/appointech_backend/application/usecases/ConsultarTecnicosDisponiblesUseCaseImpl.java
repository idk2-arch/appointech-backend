package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ReglasAsignacionManual;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TecnicoDisponible;
import com.appointech.appointech_backend.domain.ports.in.ConsultarTecnicosDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso (RF15): ayuda al administrador a elegir técnico para una solicitud manual. Reutiliza el filtro por
 * especialidad (RF27) y las mismas reglas de ocupación que la asignación.
 */
@Service
public class ConsultarTecnicosDisponiblesUseCaseImpl implements ConsultarTecnicosDisponiblesUseCase {

    private static final Comparator<TecnicoDisponible> POR_CERCANIA = Comparator
            .comparing(TecnicoDisponible::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TecnicoDisponible::tecnicoId);

    private final ReglasAsignacionManual reglasAsignacionManual;
    private final FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase;
    private final CalculadoraDistanciaPort calculadoraDistanciaPort;

    public ConsultarTecnicosDisponiblesUseCaseImpl(ReglasAsignacionManual reglasAsignacionManual,
                                                   FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
                                                   CalculadoraDistanciaPort calculadoraDistanciaPort) {
        this.reglasAsignacionManual = reglasAsignacionManual;
        this.filtrarTecnicosPorEspecialidadUseCase = filtrarTecnicosPorEspecialidadUseCase;
        this.calculadoraDistanciaPort = calculadoraDistanciaPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TecnicoDisponible> consultar(Long solicitudId, LocalDateTime inicio, LocalDateTime fin) {
        reglasAsignacionManual.exigirRangoValido(inicio, fin);
        Cita solicitud = reglasAsignacionManual.solicitudPendiente(solicitudId);

        List<Tecnico> libres = filtrarTecnicosPorEspecialidadUseCase.filtrar(solicitud.getEspecialidadId()).stream()
                .filter(tecnico -> !reglasAsignacionManual.tecnicoOcupado(tecnico.getId(), solicitud.getId(), inicio, fin))
                .toList();
        if (libres.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> distanciasKm = distanciasKm(solicitud, libres);
        return libres.stream()
                .map(tecnico -> new TecnicoDisponible(tecnico.getId(), tecnico.getUsuario().getNombre(),
                        tecnico.getUsuario().getApellido(), tecnico.getUsuario().getTelefono(),
                        tecnico.getUsuario().getFotoClave(), distanciasKm.get(tecnico.getId())))
                .sorted(POR_CERCANIA)
                .toList();
    }

    /** Distancia por técnico; no aparecen los que no tienen base utilizable ni, si la solicitud no tiene ubicación, ninguno. */
    private Map<Long, Double> distanciasKm(Cita solicitud, List<Tecnico> tecnicos) {
        Coordenadas destinoCliente = coordenadasOnulo(solicitud.getLatitud(), solicitud.getLongitud());
        Map<Long, Double> resultado = new HashMap<>();
        if (destinoCliente == null) {
            return resultado;
        }
        List<Tecnico> conBase = new ArrayList<>();
        List<Coordenadas> bases = new ArrayList<>();
        for (Tecnico tecnico : tecnicos) {
            Coordenadas base = coordenadasOnulo(tecnico.getLatitudBase(), tecnico.getLongitudBase());
            if (base != null) {
                conBase.add(tecnico);
                bases.add(base);
            }
        }
        if (conBase.isEmpty()) {
            return resultado;
        }
        List<Distancia> distancias = calculadoraDistanciaPort.calcular(destinoCliente, bases);
        if (distancias.size() != conBase.size()) {
            throw new IllegalStateException("El calculador de distancias devolvió una cantidad inesperada de resultados");
        }
        for (int i = 0; i < conBase.size(); i++) {
            resultado.put(conBase.get(i).getId(), distancias.get(i).kilometros());
        }
        return resultado;
    }

    private Coordenadas coordenadasOnulo(Double latitud, Double longitud) {
        try {
            return Coordenadas.opcional(latitud, longitud);
        } catch (CoordenadasInvalidasException e) {
            return null;
        }
    }
}
