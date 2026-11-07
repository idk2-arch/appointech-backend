package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.TipoNotificacion;
import com.appointech.appointech_backend.infrastructure.dto.NotificacionResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionMapperTest {

    @Test
    void laRespuestaTraeSoloLosCamposDelContrato() {
        LocalDateTime fecha = LocalDateTime.of(2030, 5, 2, 10, 0);
        Notificacion n = new Notificacion(3L, 11L, TipoNotificacion.PAGO_REGISTRADO, "Pago registrado", "mensaje", 5L, true, fecha);

        assertEquals(new NotificacionResponse(3L, "PAGO_REGISTRADO", "Pago registrado", "mensaje", 5L, true, fecha),
                NotificacionMapper.aRespuesta(n));
    }

    @Test
    void soloNoLeidasAceptaTrueYFalseEnCualquierMayusculaYFaltanteEsFalso() {
        assertTrue(NotificacionMapper.aSoloNoLeidas("true"));
        assertTrue(NotificacionMapper.aSoloNoLeidas(" TRUE "));
        assertFalse(NotificacionMapper.aSoloNoLeidas("False"));
        assertFalse(NotificacionMapper.aSoloNoLeidas(null));
        assertFalse(NotificacionMapper.aSoloNoLeidas("  "));
        assertThrows(FiltroCitasInvalidoException.class, () -> NotificacionMapper.aSoloNoLeidas("si"));
    }

    @Test
    void aBooleanoDevuelveNuloSiNoHayTexto() {
        assertNull(ParametrosDeConsulta.aBooleano("x", null));
        assertNull(ParametrosDeConsulta.aBooleano("x", ""));
    }

    @Test
    void laPaginacionSeValidaConLasMismasReglasDeSiempre() {
        assertEquals(new PaginacionOpcional(null, null), NotificacionMapper.aPaginacion(null, null));
        assertEquals(new PaginacionOpcional(0, 20), NotificacionMapper.aPaginacion(0, null));
        assertThrows(FiltroCitasInvalidoException.class, () -> NotificacionMapper.aPaginacion(0, 101));
    }
}
