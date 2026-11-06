package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListarSolicitudesManualesUseCaseImpl implements ListarSolicitudesManualesUseCase {

    static final int TAMANIO_POR_DEFECTO = 20;
    static final int TAMANIO_MAXIMO = 100;

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
        return listar(FiltroSolicitudesManuales.sinFiltros()).elementos();
    }

    @Override
    @Transactional(readOnly = true)
    public ListadoConTotal<SolicitudManualDetalle> listar(FiltroSolicitudesManuales filtro) {
        ListadoConTotal<Cita> encontradas = citaRepositoryPort.buscarSolicitudesManuales(normalizar(filtro));
        List<Cita> solicitudes = encontradas.elementos();
        if (solicitudes.isEmpty()) {
            return new ListadoConTotal<>(List.of(), encontradas.total());
        }

        // Una sola consulta de clientes y una de especialidades para todas las solicitudes (sin N+1).
        Map<Long, ClienteResumen> clientes = consultaClientesPort.resumenesPorId(
                solicitudes.stream().map(Cita::getClienteId).distinct().toList());
        Map<Long, String> especialidades = especialidadRepositoryPort.buscarPorIds(
                        solicitudes.stream().map(Cita::getEspecialidadId).distinct().toList())
                .stream().collect(Collectors.toMap(Especialidad::getId, Especialidad::getNombre, (a, b) -> a));

        List<SolicitudManualDetalle> detalles = solicitudes.stream()
                .map(cita -> aDetalle(cita, clientes.get(cita.getClienteId()), especialidades))
                .toList();
        return new ListadoConTotal<>(detalles, encontradas.total());
    }

    /** Aplica el estado por defecto (PENDIENTE) y valida la paginación; si solo llega uno de los dos datos, el otro toma su valor por defecto. */
    private FiltroSolicitudesManuales normalizar(FiltroSolicitudesManuales filtro) {
        EstadoCita estado = filtro.estado() == null ? EstadoCita.PENDIENTE : filtro.estado();
        if (filtro.pagina() == null && filtro.tamanio() == null) {
            return new FiltroSolicitudesManuales(filtro.motivo(), filtro.especialidadId(), estado, null, null);
        }
        int pagina = filtro.pagina() == null ? 0 : filtro.pagina();
        int tamanio = filtro.tamanio() == null ? TAMANIO_POR_DEFECTO : filtro.tamanio();
        if (pagina < 0) {
            throw new FiltroCitasInvalidoException("La página debe ser mayor o igual a 0");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new FiltroCitasInvalidoException("El tamaño debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
        return new FiltroSolicitudesManuales(filtro.motivo(), filtro.especialidadId(), estado, pagina, tamanio);
    }

    private SolicitudManualDetalle aDetalle(Cita cita, ClienteResumen cliente, Map<Long, String> especialidades) {
        String telefono = cita.getTelefonoContacto() != null ? cita.getTelefonoContacto()
                : cliente != null ? cliente.telefono() : null;
        return new SolicitudManualDetalle(
                cita.getId(), cita.getClienteId(),
                cliente != null ? cliente.nombre() : null,
                cliente != null ? cliente.apellido() : null,
                telefono, cita.getDireccionServicio(), cita.getDescripcionProblema(),
                cita.getEspecialidadId(), especialidades.get(cita.getEspecialidadId()),
                cita.getLatitud(), cita.getLongitud(), cita.getCreadaEn(), cita.getMotivoSolicitudEspecial(),
                cita.getEstado());
    }
}
