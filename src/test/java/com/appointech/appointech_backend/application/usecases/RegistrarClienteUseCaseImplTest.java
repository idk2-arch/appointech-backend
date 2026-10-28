package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
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
class RegistrarClienteUseCaseImplTest {

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private PasswordEncoderPort encoder;
    @InjectMocks
    private RegistrarClienteUseCaseImpl useCase;

    @Test
    void registraUsuarioClienteConContrasenaEncriptadaYDireccion() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.empty());
        when(encoder.encriptar("clave12345")).thenReturn("HASH");
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(21L);
            return u;
        });
        when(clientes.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = useCase.registrar("Ana", "Lopez", "ana@mail.com", "clave12345", "300",
                TipoDocumento.CC, "123", "Calle 1");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        Usuario u = captor.getValue();
        assertEquals("HASH", u.getContrasena());
        assertEquals(RolUsuario.CLIENTE, u.getRol());
        assertTrue(u.isActivo());
        assertNotNull(u.getCreadoEn());
        assertEquals("300", u.getTelefono());
        assertEquals(TipoDocumento.CC, u.getTipoDocumento());
        assertEquals("123", u.getNumeroDocumento());
        assertEquals("Calle 1", resultado.getDireccion());
        assertEquals(21L, resultado.getUsuario().getId());
    }

    @Test
    void correoYaRegistradoLanzaExcepcionYNoGuardaNada() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(new Usuario()));

        assertThrows(CorreoYaRegistradoException.class, () -> useCase.registrar("Ana", "Lopez", "ana@mail.com",
                "clave12345", "300", TipoDocumento.CC, "123", "Calle 1"));

        verify(usuarios, never()).guardar(any());
        verify(clientes, never()).guardar(any());
        verify(encoder, never()).encriptar(any());
    }
}
