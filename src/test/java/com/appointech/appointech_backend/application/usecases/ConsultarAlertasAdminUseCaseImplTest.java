package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarAlertasAdminUseCaseImplTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 5, 6, 9, 0);

    private final ListarSolicitudesManualesUseCase listar = mock(ListarSolicitudesManualesUseCase.class);
    private final ConsultarAlertasAdminUseCaseImpl casoDeUso = new ConsultarAlertasAdminUseCaseImpl(listar);

    private static SolicitudManualDetalle solicitud(long id, MotivoSolicitudEspecial motivo) {
        return new SolicitudManualDetalle(id, 2L, "Ana", "Ruiz", "300", "Calle 1", "Fuga", 5L, "Plomería",
                4.5, -74.1, FECHA, motivo, EstadoCita.PENDIENTE);
    }

    @Test
    void soloDevuelveLasSolicitudesSinTecnicosConLaEspecialidad() {
        SolicitudManualDetalle sinTecnicos = solicitud(1L, MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD);
        SolicitudManualDetalle otraSinTecnicos = solicitud(4L, MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD);
        when(listar.listar()).thenReturn(List.of(
                sinTecnicos,
                solicitud(2L, MotivoSolicitudEspecial.FUERA_DE_COBERTURA),
                solicitud(3L, null),
                otraSinTecnicos));

        assertEquals(List.of(sinTecnicos, otraSinTecnicos), casoDeUso.sinTecnicosParaLaEspecialidad());
    }

    @Test
    void sinSolicitudesEspecialesNoHayAlertas() {
        when(listar.listar()).thenReturn(List.of());

        assertTrue(casoDeUso.sinTecnicosParaLaEspecialidad().isEmpty());
    }
}
