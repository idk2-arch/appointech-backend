package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase.ResultadoLogin;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort;
import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort.GoogleUser;
import com.appointech.appointech_backend.domain.ports.out.TokenGeneratorPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginConGoogleUseCaseImplTest {

    private static final GoogleUser GOOGLE = new GoogleUser("g-1", "ana@gmail.com", "Ana", "Lopez");

    @Mock
    private GoogleTokenVerifierPort verificador;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private TokenGeneratorPort tokens;
    @InjectMocks
    private LoginConGoogleUseCaseImpl useCase;

    private Usuario existente(RolUsuario rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("Ana");
        u.setCorreo("ana@gmail.com");
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    @Test
    void tokenInvalidoLanzaCredencialesInvalidasSinTocarRepositorios() {
        when(verificador.verificar("malo")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> useCase.ejecutar("malo"));

        verify(usuarios, never()).buscarPorCorreo(any());
        verify(tokens, never()).generarToken(any());
    }

    @Test
    void usuarioNuevoSeRegistraComoClienteActivoSinContrasenaYConFilaDeCliente() {
        when(verificador.verificar("ok")).thenReturn(Optional.of(GOOGLE));
        when(usuarios.buscarPorCorreo("ana@gmail.com")).thenReturn(Optional.empty());
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });
        when(tokens.generarToken(any(Usuario.class))).thenReturn("jwt");
        when(clientes.buscarPorUsuarioId(11L)).thenReturn(Optional.empty());

        ResultadoLogin r = useCase.ejecutar("ok");

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(usuario.capture());
        assertEquals(RolUsuario.CLIENTE, usuario.getValue().getRol());
        assertNull(usuario.getValue().getContrasena());
        assertTrue(usuario.getValue().isActivo());
        assertNotNull(usuario.getValue().getCreadoEn());
        ArgumentCaptor<Cliente> cliente = ArgumentCaptor.forClass(Cliente.class);
        verify(clientes).guardar(cliente.capture());
        assertEquals(11L, cliente.getValue().getUsuario().getId());
        assertEquals("jwt", r.token());
        assertEquals("CLIENTE", r.rol());
        assertEquals("Ana", r.nombre());
        assertEquals("ana@gmail.com", r.correo());
        assertFalse(r.perfilCompleto(), "un cliente recien creado no tiene telefono, documento ni direccion");
    }

    @Test
    void usuarioExistenteEntraConSuRolSinRegistrarOtro() {
        Usuario admin = existente(RolUsuario.ADMINISTRADOR, true);
        admin.setTelefono("300");
        admin.setTipoDocumento(TipoDocumento.CC);
        admin.setNumeroDocumento("55");
        when(verificador.verificar("ok")).thenReturn(Optional.of(GOOGLE));
        when(usuarios.buscarPorCorreo("ana@gmail.com")).thenReturn(Optional.of(admin));
        when(tokens.generarToken(admin)).thenReturn("jwt-admin");
        when(clientes.buscarPorUsuarioId(3L)).thenReturn(Optional.empty());

        ResultadoLogin r = useCase.ejecutar("ok");

        verify(usuarios, never()).guardar(any());
        verify(clientes, never()).guardar(any());
        assertEquals("ADMINISTRADOR", r.rol());
        assertEquals("jwt-admin", r.token());
        assertTrue(r.perfilCompleto());
    }

    @Test
    void clienteConDatosYDireccionTienePerfilCompleto() {
        Usuario u = existente(RolUsuario.CLIENTE, true);
        u.setTelefono("300");
        u.setTipoDocumento(TipoDocumento.CC);
        u.setNumeroDocumento("55");
        Cliente c = new Cliente();
        c.setDireccion("Calle 1");
        when(verificador.verificar("ok")).thenReturn(Optional.of(GOOGLE));
        when(usuarios.buscarPorCorreo("ana@gmail.com")).thenReturn(Optional.of(u));
        when(tokens.generarToken(u)).thenReturn("jwt");
        when(clientes.buscarPorUsuarioId(3L)).thenReturn(Optional.of(c));

        assertTrue(useCase.ejecutar("ok").perfilCompleto());
    }

    @Test
    void usuarioInactivoEsRechazadoSinEmitirToken() {
        when(verificador.verificar("ok")).thenReturn(Optional.of(GOOGLE));
        when(usuarios.buscarPorCorreo("ana@gmail.com")).thenReturn(Optional.of(existente(RolUsuario.CLIENTE, false)));

        assertThrows(CredencialesInvalidasException.class, () -> useCase.ejecutar("ok"));

        verify(tokens, never()).generarToken(any());
    }
}
