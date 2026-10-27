package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListarSolicitudesManualesUseCaseImpl implements ListarSolicitudesManualesUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final ConsultaClientesPort consultaClientesPort;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public ListarSolicitudesManualesUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                                ConsultaClientesPort consultaClientesPort,
                                                EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.consultaClientesPort = consultaClientesPort;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudManualDetalle> listar() {
        List<Cita> solicitudes = citaRepositoryPort.buscarSolicitudesManualesPendientes();
        if (solicitudes.isEmpty()) {
            return List.of();
        }

        // Una sola consulta de clientes y una de especialidades para todas las solicitudes (sin N+1).
        Map<Long, ClienteResumen> clientes = consultaClientesPort.resumenesPorId(
                solicitudes.stream().map(Cita::getClienteId).distinct().toList());
        Map<Long, String> especialidades = especialidadRepositoryPort.buscarPorIds(
                        solicitudes.stream().map(Cita::getEspecialidadId).distinct().toList())
                .stream().collect(Collectors.toMap(Especialidad::getId, Especialidad::getNombre, (a, b) -> a));

        return solicitudes.stream().map(cita -> {
            ClienteResumen cliente = clientes.get(cita.getClienteId());
            String telefono = cita.getTelefonoContacto() != null ? cita.getTelefonoContacto()
                    : cliente != null ? cliente.telefono() : null;
            return new SolicitudManualDetalle(
                    cita.getId(), cita.getClienteId(),
                    cliente != null ? cliente.nombre() : null,
                    cliente != null ? cliente.apellido() : null,
                    telefono, cita.getDireccionServicio(), cita.getDescripcionProblema(),
                    cita.getEspecialidadId(), especialidades.get(cita.getEspecialidadId()),
                    cita.getLatitud(), cita.getLongitud(), cita.getCreadaEn());
        }).toList();
    }
}
