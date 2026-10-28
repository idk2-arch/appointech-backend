package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.ports.in.ConsultarDetalleCitaAdminUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador consulta el detalle completo de cualquier cita.
 * <p>Historias de usuario: SCRUM-18 (consultar estado e historial).
 */
@Service
public class ConsultarDetalleCitaAdminUseCaseImpl implements ConsultarDetalleCitaAdminUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCita;
    private final HistorialEstadoCitaPort historialPort;
    private final CitaFotoRepositoryPort citaFotoRepositoryPort;

    public ConsultarDetalleCitaAdminUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                                ObtenerDetalleCitaUseCase obtenerDetalleCita,
                                                HistorialEstadoCitaPort historialPort,
                                                CitaFotoRepositoryPort citaFotoRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.obtenerDetalleCita = obtenerDetalleCita;
        this.historialPort = historialPort;
        this.citaFotoRepositoryPort = citaFotoRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public DetalleCitaCompleto detallar(Long citaId) {
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));
        return new DetalleCitaCompleto(obtenerDetalleCita.detallar(cita), historialPort.listarPorCita(citaId),
                citaFotoRepositoryPort.listarPorCita(citaId));
    }
}
