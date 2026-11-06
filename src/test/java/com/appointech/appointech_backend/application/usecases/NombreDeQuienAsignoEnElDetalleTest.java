package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** El detalle de cita resuelve el nombre del administrador que asignó manualmente (RF15). */
class NombreDeQuienAsignoEnElDetalleTest {

    private UsuarioRepositoryPort usuarioRepo;
    private ObtenerDetalleCitaUseCaseImpl detalle;

    @BeforeEach
    void preparar() {
        usuarioRepo = mock(UsuarioRepositoryPort.class);
        detalle = new ObtenerDetalleCitaUseCaseImpl(mock(TecnicoRepositoryPort.class),
                mock(EspecialidadRepositoryPort.class), usuarioRepo);
    }

    private static Cita cita(Long asignadaPorId) {
        Cita c = new Cita();
        c.setId(15L);
        c.setEstado(EstadoCita.CONFIRMADA);
        c.setAsignacionManual(true);
        c.setAsignadaPorId(asignadaPorId);
        return c;
    }

    @Test
    void traeElNombreCompletoDelAdministradorQueAsigno() {
        Usuario admin = new Usuario();
        admin.setNombre("Root");
        admin.setApellido("Admin");
        when(usuarioRepo.buscarPorId(1L)).thenReturn(Optional.of(admin));

        DetalleCita d = detalle.detallar(cita(1L));

        assertEquals("Root Admin", d.asignadaPorNombre());
    }

    @Test
    void sinAdministradorRegistradoNoConsultaUsuariosYDejaElNombreEnNulo() {
        DetalleCita d = detalle.detallar(cita(null));

        assertNull(d.asignadaPorNombre());
        verifyNoInteractions(usuarioRepo);
    }

    @Test
    void siElAdministradorYaNoExisteElNombreQuedaEnNuloSinFallar() {
        when(usuarioRepo.buscarPorId(1L)).thenReturn(Optional.empty());

        assertNull(detalle.detallar(cita(1L)).asignadaPorNombre());
    }
}
