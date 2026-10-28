package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.AutenticarUsuarioUseCase.ResultadoAutenticacion;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TokenGeneratorPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentasUseCasesTest {

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private PasswordEncoderPort encoder;
    @Mock
    private TokenGeneratorPort tokens;

    private Usuario usuario(RolUsuario rol, boolean activo, String hash) {
        Usuario u = new Usuario();
        u.setId(5L);
        u.setCorreo("ana@mail.com");
        u.setRol(rol);
        u.setActivo(activo);
        u.setContrasena(hash);
        return u;
    }

    // ---- CrearAdministrador ----

    @Test
    void crearAdministradorGuardaConRolAdminActivoYContrasenaEncriptada() {
        CrearAdministradorUseCaseImpl useCase = new CrearAdministradorUseCaseImpl(usuarios, encoder);
        when(usuarios.buscarPorCorreo("admin@mail.com")).thenReturn(Optional.empty());
        when(encoder.encriptar("clave12345")).thenReturn("HASH");
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario r = useCase.crear("Ada", "Root", "admin@mail.com", "clave12345", "300", TipoDocumento.CC, "9");

        assertEquals(RolUsuario.ADMINISTRADOR, r.getRol());
        assertTrue(r.isActivo());
        assertEquals("HASH", r.getContrasena());
        assertEquals("300", r.getTelefono());
        assertEquals(TipoDocumento.CC, r.getTipoDocumento());
        assertEquals("9", r.getNumeroDocumento());
        assertNotNull(r.getCreadoEn());
    }

    @Test
    void crearAdministradorConCorreoExistenteFalla() {
        CrearAdministradorUseCaseImpl useCase = new CrearAdministradorUseCaseImpl(usuarios, encoder);
        when(usuarios.buscarPorCorreo("admin@mail.com")).thenReturn(Optional.of(new Usuario()));

        assertThrows(CorreoYaRegistradoException.class,
                () -> useCase.crear("Ada", "Root", "admin@mail.com", "x", "3", TipoDocumento.CC, "9"));

        verify(usuarios, never()).guardar(any());
    }

    // ---- AutenticarUsuario ----

    private AutenticarUsuarioUseCaseImpl autenticador() {
        return new AutenticarUsuarioUseCaseImpl(usuarios, clientes, encoder, tokens);
    }

    @Test
    void autenticarConCredencialesCorrectasDevuelveTokenYPerfil() {
        Usuario u = usuario(RolUsuario.TECNICO, true, "HASH");
        u.setTelefono("300");
        u.setTipoDocumento(TipoDocumento.CC);
        u.setNumeroDocumento("1");
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(encoder.coincide("clave", "HASH")).thenReturn(true);
        when(tokens.generarToken(u)).thenReturn("jwt");
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.empty());

        ResultadoAutenticacion r = autenticador().autenticar("ana@mail.com", "clave");

        assertEquals("jwt", r.token());
        assertTrue(r.perfilCompleto());
    }

    @Test
    void autenticarClienteSinDireccionTienePerfilIncompleto() {
        Usuario u = usuario(RolUsuario.CLIENTE, true, "HASH");
        u.setTelefono("300");
        u.setTipoDocumento(TipoDocumento.CC);
        u.setNumeroDocumento("1");
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(encoder.coincide("clave", "HASH")).thenReturn(true);
        when(tokens.generarToken(u)).thenReturn("jwt");
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.of(new Cliente()));

        assertFalse(autenticador().autenticar("ana@mail.com", "clave").perfilCompleto());
    }

    @Test
    void autenticarCorreoInexistenteFalla() {
        when(usuarios.buscarPorCorreo("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> autenticador().autenticar("x@mail.com", "c"));
    }

    @Test
    void autenticarUsuarioInactivoFallaSinVerificarContrasena() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario(RolUsuario.CLIENTE, false, "HASH")));

        assertThrows(CredencialesInvalidasException.class, () -> autenticador().autenticar("ana@mail.com", "c"));

        verify(encoder, never()).coincide(any(), any());
    }

    @Test
    void autenticarUsuarioSoloGoogleSinContrasenaNoPuedeEntrarConClave() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario(RolUsuario.CLIENTE, true, null)));

        assertThrows(CredencialesInvalidasException.class, () -> autenticador().autenticar("ana@mail.com", "c"));

        verify(encoder, never()).coincide(any(), any());
    }

    @Test
    void autenticarContrasenaIncorrectaFallaSinEmitirToken() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario(RolUsuario.CLIENTE, true, "HASH")));
        when(encoder.coincide("mala", "HASH")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> autenticador().autenticar("ana@mail.com", "mala"));

        verify(tokens, never()).generarToken(any());
    }

    // ---- CompletarPerfil ----

    private CompletarPerfilUseCaseImpl completar() {
        return new CompletarPerfilUseCaseImpl(usuarios, clientes);
    }

    @Test
    void completarPerfilDeClienteActualizaUsuarioYDireccion() {
        Usuario u = usuario(RolUsuario.CLIENTE, true, null);
        Cliente c = new Cliente();
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(usuarios.guardar(u)).thenReturn(u);
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.of(c));

        Usuario r = completar().completar("ana@mail.com", "300", TipoDocumento.CE, "77", "Calle 9");

        assertEquals("300", r.getTelefono());
        assertEquals(TipoDocumento.CE, r.getTipoDocumento());
        assertEquals("77", r.getNumeroDocumento());
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clientes).guardar(captor.capture());
        assertEquals("Calle 9", captor.getValue().getDireccion());
    }

    @Test
    void completarPerfilSinDireccionNoTocaAlCliente() {
        Usuario u = usuario(RolUsuario.CLIENTE, true, null);
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(usuarios.guardar(u)).thenReturn(u);

        completar().completar("ana@mail.com", "300", TipoDocumento.CE, "77", "  ");
        completar().completar("ana@mail.com", "300", TipoDocumento.CE, "77", null);

        verify(clientes, never()).buscarPorUsuarioId(any());
        verify(clientes, never()).guardar(any());
    }

    @Test
    void completarPerfilDeTecnicoIgnoraLaDireccion() {
        Usuario u = usuario(RolUsuario.TECNICO, true, null);
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(usuarios.guardar(u)).thenReturn(u);

        completar().completar("ana@mail.com", "300", TipoDocumento.CC, "1", "Calle 9");

        verify(clientes, never()).guardar(any());
    }

    @Test
    void completarPerfilClienteSinFilaDeClienteSoloGuardaElUsuario() {
        Usuario u = usuario(RolUsuario.CLIENTE, true, null);
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(usuarios.guardar(u)).thenReturn(u);
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.empty());

        completar().completar("ana@mail.com", "300", TipoDocumento.CC, "1", "Calle 9");

        verify(clientes, never()).guardar(any());
    }

    @Test
    void completarPerfilDeCorreoDesconocidoFalla() {
        when(usuarios.buscarPorCorreo("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> completar().completar("x@mail.com", "3", TipoDocumento.CC, "1", "d"));

        verify(usuarios, never()).guardar(any());
    }
}
