package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ConsultarAlertasAdminUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso: alertas del administrador. No guarda nada: se derivan de las solicitudes especiales pendientes,
 * así que la fuente de verdad es una sola y la alerta desaparece sola cuando la solicitud se atiende.
 * <p>Historia: RF28 - Alertar si no hay técnico con la especialidad.
 */
@Service
public class ConsultarAlertasAdminUseCaseImpl implements ConsultarAlertasAdminUseCase {

    private final ListarSolicitudesManualesUseCase listarSolicitudesManualesUseCase;

    public ConsultarAlertasAdminUseCaseImpl(ListarSolicitudesManualesUseCase listarSolicitudesManualesUseCase) {
        this.listarSolicitudesManualesUseCase = listarSolicitudesManualesUseCase;
    }

    @Override
    public List<SolicitudManualDetalle> sinTecnicosParaLaEspecialidad() {
        return listarSolicitudesManualesUseCase.listar().stream()
                .filter(s -> s.motivo() == MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD)
                .toList();
    }
}
