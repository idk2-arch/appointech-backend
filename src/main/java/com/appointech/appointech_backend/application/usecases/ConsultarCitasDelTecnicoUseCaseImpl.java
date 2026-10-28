package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasDelTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso: citas activas del técnico en un día, con los datos de contacto del cliente.
 * Es la base de la ruta del día.
 * <p>Historia de usuario: <a href="https://rossas2111.atlassian.net/browse/SCRUM-15">SCRUM-15</a> - RF10 - Ruta optimizada del día
 */
@Service
public class ConsultarCitasDelTecnicoUseCaseImpl implements ConsultarCitasDelTecnicoUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final ConsultaCitasDelTecnicoPort consultaCitasPort;
    private final ConsultaClientesPort consultaClientesPort;
    private final CitaFotoRepositoryPort citaFotoRepositoryPort;

    public ConsultarCitasDelTecnicoUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                               ConsultaCitasDelTecnicoPort consultaCitasPort,
                                               ConsultaClientesPort consultaClientesPort,
                                               CitaFotoRepositoryPort citaFotoRepositoryPort) {
        this.resolverTecnico = resolverTecnico;
        this.consultaCitasPort = consultaCitasPort;
        this.consultaClientesPort = consultaClientesPort;
        this.citaFotoRepositoryPort = citaFotoRepositoryPort;
    }

    @Override
    public List<CitaConCliente> consultar(String correoTecnico, LocalDate fecha) {
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        List<Cita> citas = consultaCitasPort.buscarPorEstadosEntre(
                        tecnico.getId(), CrearBloqueUseCaseImpl.ESTADOS_QUE_OCUPAN,
                        fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                .stream()
                .sorted(Comparator.comparing(Cita::getFechaHoraInicio))
                .toList();

        Map<Long, ClienteResumen> clientes = consultaClientesPort.resumenesPorId(
                citas.stream().map(Cita::getClienteId).distinct().toList());
        Map<Long, List<CitaFoto>> fotos = citaFotoRepositoryPort.listarPorCitas(citas.stream().map(Cita::getId).toList());

        return citas.stream()
                .map(cita -> new CitaConCliente(cita, clientes.get(cita.getClienteId()), fotos.getOrDefault(cita.getId(), List.of())))
                .toList();
    }
}
