package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificadorTecnicoAsignadoTest {

    private CitaRepositoryPort citas;
    private UsuarioRepositoryPort usuarios;
    private TecnicoRepositoryPort tecnicos;
    private VerificadorAccesoCita verificador;
    private Cita cita;

    @BeforeEach
    void preparar() {
        citas = mock(CitaRepositoryPort.class);
        usuarios = mock(UsuarioRepositoryPort.class);
        tecnicos = mock(TecnicoRepositoryPort.class);
        verificador = new VerificadorAccesoCita(citas, usuarios, mock(ClienteRepositoryPort.class), tecnicos);
        cita = new Cita();
        cita.setId(10L);
        cita.setTecnicoId(3L);
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita));
    }

    private void usuario(String correo, Long id, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo(correo);
        u.setRol(rol);
        when(usuarios.buscarPorCorreo(correo)).thenReturn(Optional.of(u));
    }

    private void tecnico(Long usuarioId, Long tecnicoId) {
        Tecnico t = new Tecnico();
        t.setId(tecnicoId);
        when(tecnicos.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.of(t));
    }

    @Test
    void elTecnicoAsignadoPasa() {
        usuario("t@x.com", 7L, RolUsuario.TECNICO);
        tecnico(7L, 3L);

        assertSame(cita, verificador.exigirTecnicoAsignado("t@x.com", 10L));
    }

    @Test
    void otroTecnicoUnClienteOUnAdministradorSonRechazados() {
        usuario("otro@x.com", 8L, RolUsuario.TECNICO);
        tecnico(8L, 4L);
        usuario("c@x.com", 9L, RolUsuario.CLIENTE);
        usuario("a@x.com", 1L, RolUsuario.ADMINISTRADOR);

        assertThrows(AccesoCitaDenegadoException.class, () -> verificador.exigirTecnicoAsignado("otro@x.com", 10L));
        assertThrows(AccesoCitaDenegadoException.class, () -> verificador.exigirTecnicoAsignado("c@x.com", 10L));
        assertThrows(AccesoCitaDenegadoException.class, () -> verificador.exigirTecnicoAsignado("a@x.com", 10L));
    }

    @Test
    void unaCitaInexistenteEs404() {
        when(citas.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class, () -> verificador.exigirTecnicoAsignado("t@x.com", 99L));
    }
}
