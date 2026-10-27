package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.ClientePorIdNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.DocumentoYaRegistradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DatosActualizacionCliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionClienteAdminUseCasesTest {

    private ClienteRepositoryPort clienteRepo;
    private UsuarioRepositoryPort usuarioRepo;
    private ActualizarClienteUseCaseImpl actualizar;

    @BeforeEach
    void preparar() {
        clienteRepo = mock(ClienteRepositoryPort.class);
        usuarioRepo = mock(UsuarioRepositoryPort.class);
        actualizar = new ActualizarClienteUseCaseImpl(clienteRepo, usuarioRepo);
        when(clienteRepo.guardar(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepo.guardar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void listaTodosLosClientes() {
        when(clienteRepo.listarTodos()).thenReturn(List.of(cliente(), cliente()));

        assertEquals(2, new ListarClientesUseCaseImpl(clienteRepo).listar().size());
    }

    @Test
    void editaTodosLosDatos() {
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente()));
        when(usuarioRepo.buscarPorCorreo("nuevo@x.com")).thenReturn(Optional.empty());
        when(usuarioRepo.buscarPorNumeroDocumento("999")).thenReturn(Optional.empty());

        Cliente r = actualizar.actualizar(2L, new DatosActualizacionCliente("Luis", "Mora", "nuevo@x.com", "311",
                TipoDocumento.CE, "999", "Carrera 9", 4.5, -74.1));

        Usuario u = r.getUsuario();
        assertEquals("Luis", u.getNombre());
        assertEquals("Mora", u.getApellido());
        assertEquals("nuevo@x.com", u.getCorreo());
        assertEquals("311", u.getTelefono());
        assertEquals(TipoDocumento.CE, u.getTipoDocumento());
        assertEquals("999", u.getNumeroDocumento());
        assertEquals("Carrera 9", r.getDireccion());
        assertEquals(4.5, r.getLatitud());
        assertEquals(-74.1, r.getLongitud());
        verify(usuarioRepo).guardar(u);
    }

    @Test
    void camposNulosNoModificanNada() {
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente()));

        Cliente r = actualizar.actualizar(2L, new DatosActualizacionCliente(null, null, null, null, null, null,
                null, null, null));

        assertEquals("Ana", r.getUsuario().getNombre());
        assertEquals("ana@x.com", r.getUsuario().getCorreo());
        assertEquals("Calle 1", r.getDireccion());
        assertEquals(4.3365, r.getLatitud());
    }

    @Test
    void permiteConservarCorreoYDocumentoPropios() {
        Cliente c = cliente();
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(c));
        when(usuarioRepo.buscarPorCorreo("ana@x.com")).thenReturn(Optional.of(c.getUsuario()));
        when(usuarioRepo.buscarPorNumeroDocumento("123")).thenReturn(Optional.of(c.getUsuario()));

        Cliente r = actualizar.actualizar(2L, new DatosActualizacionCliente(null, null, "ANA@x.com", null, null,
                "123", null, null, null));

        assertEquals("ANA@x.com", r.getUsuario().getCorreo());
    }

    @Test
    void correoAjenoLanza409() {
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente()));
        when(usuarioRepo.buscarPorCorreo("otro@x.com")).thenReturn(Optional.of(otroUsuario()));

        assertThrows(CorreoYaRegistradoException.class, () -> actualizar.actualizar(2L,
                new DatosActualizacionCliente(null, null, "otro@x.com", null, null, null, null, null, null)));
        verify(usuarioRepo, never()).guardar(any());
        verify(clienteRepo, never()).guardar(any());
    }

    @Test
    void documentoAjenoLanza409() {
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente()));
        when(usuarioRepo.buscarPorNumeroDocumento("555")).thenReturn(Optional.of(otroUsuario()));

        assertThrows(DocumentoYaRegistradoException.class, () -> actualizar.actualizar(2L,
                new DatosActualizacionCliente(null, null, null, null, null, "555", null, null, null)));
        verify(usuarioRepo, never()).guardar(any());
    }

    @Test
    void clienteInexistenteLanza404() {
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.empty());

        assertThrows(ClientePorIdNoEncontradoException.class, () -> actualizar.actualizar(2L,
                new DatosActualizacionCliente("X", null, null, null, null, null, null, null, null)));
    }

    @Test
    void coordenadasIncompletasOInvalidasLanzan400SinGuardar() {
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente()));

        assertThrows(CoordenadasInvalidasException.class, () -> actualizar.actualizar(2L,
                new DatosActualizacionCliente(null, null, null, null, null, null, null, 4.5, null)));
        assertThrows(CoordenadasInvalidasException.class, () -> actualizar.actualizar(2L,
                new DatosActualizacionCliente(null, null, null, null, null, null, null, -74.1, 4.5)));
        verify(usuarioRepo, never()).guardar(any());
        verify(clienteRepo, never()).guardar(any());
    }

    private static Usuario otroUsuario() {
        Usuario o = new Usuario();
        o.setId(99L);
        return o;
    }

    private static Cliente cliente() {
        Usuario u = new Usuario();
        u.setId(8L);
        u.setNombre("Ana");
        u.setApellido("Ruiz");
        u.setCorreo("ana@x.com");
        u.setTelefono("300");
        u.setTipoDocumento(TipoDocumento.CC);
        u.setNumeroDocumento("123");
        u.setRol(RolUsuario.CLIENTE);
        u.setActivo(true);
        Cliente c = new Cliente();
        c.setId(2L);
        c.setUsuario(u);
        c.setDireccion("Calle 1");
        c.setLatitud(4.3365);
        c.setLongitud(-74.3638);
        return c;
    }
}
