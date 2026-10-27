package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.CitaSinUbicacionException;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.exception.UbicacionBaseNoConfiguradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.ConfiguracionRuta;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.ParadaRuta;
import com.appointech.appointech_backend.domain.models.RutaDelDia;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.CalcularRutaDelDiaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso: ruta del día del técnico. Las citas tienen hora fija, así que las paradas van en orden de hora y
 * se calcula el tramo por carretera desde la base hasta la primera cita y entre citas consecutivas.
 * Nada se guarda: la ruta se recalcula en cada petición, por eso refleja citas nuevas, canceladas y cambios.
 * <p>Historias de usuario:
 * <ul>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-15">SCRUM-15</a> - RF10 - Ruta optimizada del día</li>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-16">SCRUM-16</a> - RF20 - Recalcular ruta ante cambios</li>
 * </ul>
 */
@Service
public class CalcularRutaDelDiaUseCaseImpl implements CalcularRutaDelDiaUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final ConsultaCitasDelTecnicoPort consultaCitasPort;
    private final ConsultaClientesPort consultaClientesPort;
    private final CalculadoraDistanciaPort calculadoraDistanciaPort;
    private final ConfiguracionRuta configuracionRuta;
    private final Clock reloj;

    public CalcularRutaDelDiaUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                         ConsultaCitasDelTecnicoPort consultaCitasPort,
                                         ConsultaClientesPort consultaClientesPort,
                                         CalculadoraDistanciaPort calculadoraDistanciaPort,
                                         ConfiguracionRuta configuracionRuta,
                                         Clock reloj) {
        this.resolverTecnico = resolverTecnico;
        this.consultaCitasPort = consultaCitasPort;
        this.consultaClientesPort = consultaClientesPort;
        this.calculadoraDistanciaPort = calculadoraDistanciaPort;
        this.configuracionRuta = configuracionRuta;
        this.reloj = reloj;
    }

    @Override
    public RutaDelDia calcular(String correoTecnico, LocalDate fecha) {
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        Coordenadas origen = coordenadasBase(tecnico);
        LocalDateTime calculadaEn = LocalDateTime.now(reloj);

        List<Cita> citas = consultaCitasPort.buscarPorEstadosEntre(
                        tecnico.getId(), CrearBloqueUseCaseImpl.ESTADOS_QUE_OCUPAN,
                        fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                .stream()
                .sorted(Comparator.comparing(Cita::getFechaHoraInicio).thenComparing(Cita::getId))
                .toList();
        if (citas.isEmpty()) {
            return new RutaDelDia(fecha, calculadaEn, origen, List.of(), 0.0, 0.0);
        }

        // Se validan todas las ubicaciones antes de llamar al puerto de distancias.
        List<Coordenadas> ubicaciones = citas.stream().map(this::ubicacionDe).toList();
        Map<Long, ClienteResumen> clientes = consultaClientesPort.resumenesPorId(
                citas.stream().map(Cita::getClienteId).distinct().toList());

        List<ParadaRuta> paradas = new ArrayList<>();
        double totalKm = 0.0;
        double totalMinutos = 0.0;
        Coordenadas anterior = origen;
        Cita citaAnterior = null;

        for (int i = 0; i < citas.size(); i++) {
            Cita cita = citas.get(i);
            Coordenadas destino = ubicaciones.get(i);
            Distancia tramo = calculadoraDistanciaPort.calcular(anterior, List.of(destino)).get(0);

            LocalDateTime horaLlegada;
            boolean alcanza;
            int retrasoMinutos = 0;
            if (citaAnterior == null) {
                // Primera parada, informativo: hora a la que habría que salir de la base para llegar justo.
                horaLlegada = cita.getFechaHoraInicio().minusSeconds(segundos(tramo.minutos()));
                alcanza = true;
            } else {
                horaLlegada = citaAnterior.getFechaHoraFin().plusSeconds(segundos(tramo.minutos()));
                retrasoMinutos = configuracionRuta.retrasoEstimadoMinutos(horaLlegada, cita.getFechaHoraInicio());
                alcanza = configuracionRuta.alcanzaATiempo(retrasoMinutos);
            }

            paradas.add(new ParadaRuta(i + 1, cita.getId(), cita.getFechaHoraInicio(), cita.getFechaHoraFin(),
                    cita.getDireccionServicio(), destino, clientes.get(cita.getClienteId()),
                    tramo.kilometros(), tramo.minutos(), horaLlegada, alcanza, retrasoMinutos));
            totalKm += tramo.kilometros();
            totalMinutos += tramo.minutos();
            anterior = destino;
            citaAnterior = cita;
        }
        return new RutaDelDia(fecha, calculadaEn, origen, paradas, totalKm, totalMinutos);
    }

    private Coordenadas coordenadasBase(Tecnico tecnico) {
        try {
            Coordenadas base = Coordenadas.opcional(tecnico.getLatitudBase(), tecnico.getLongitudBase());
            if (base == null) {
                throw new UbicacionBaseNoConfiguradaException();
            }
            return base;
        } catch (CoordenadasInvalidasException e) {
            throw new UbicacionBaseNoConfiguradaException();
        }
    }

    private Coordenadas ubicacionDe(Cita cita) {
        try {
            return Coordenadas.de(cita.getLatitud(), cita.getLongitud());
        } catch (CoordenadasInvalidasException e) {
            throw new CitaSinUbicacionException(cita.getId());
        }
    }

    private long segundos(double minutos) {
        return Math.round(minutos * 60.0);
    }
}
