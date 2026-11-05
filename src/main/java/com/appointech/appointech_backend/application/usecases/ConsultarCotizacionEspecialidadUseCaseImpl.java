package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso: el cliente consulta el valor estimado de una especialidad antes de pedir el servicio.
 * <p>Historias de usuario: SCRUM-23 (RF21 - cotización).
 */
@Service
public class ConsultarCotizacionEspecialidadUseCaseImpl implements ConsultarCotizacionEspecialidadUseCase {

    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public ConsultarCotizacionEspecialidadUseCaseImpl(EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public Cotizacion cotizar(Long especialidadId) {
        Especialidad especialidad = especialidadRepositoryPort.buscarPorId(especialidadId)
                .filter(Especialidad::isActivo)
                .orElseThrow(() -> new EspecialidadNoEncontradaException(List.of(especialidadId)));
        return Cotizacion.de(especialidad);
    }
}
