package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCotizacion;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: cotización de una cita (costo de referencia de su especialidad), que queda guardada en el pago.
 * <p>Historias de usuario: SCRUM-23 (RF21 - cotización).
 */
@Service
public class ConsultarCotizacionCitaUseCaseImpl implements ConsultarCotizacionCitaUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;
    private final GestorCotizacion gestorCotizacion;

    public ConsultarCotizacionCitaUseCaseImpl(VerificadorAccesoCita verificadorAcceso,
                                              EspecialidadRepositoryPort especialidadRepositoryPort,
                                              GestorCotizacion gestorCotizacion) {
        this.verificadorAcceso = verificadorAcceso;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
        this.gestorCotizacion = gestorCotizacion;
    }

    @Override
    @Transactional
    public Cotizacion cotizar(String correo, Long citaId) {
        Cita cita = verificadorAcceso.exigirAcceso(correo, citaId);
        Especialidad especialidad = especialidadRepositoryPort.buscarPorId(cita.getEspecialidadId())
                .orElseThrow(() -> new EspecialidadNoEncontradaException(List.of(cita.getEspecialidadId())));
        return gestorCotizacion.registrar(citaId, especialidad);
    }
}
