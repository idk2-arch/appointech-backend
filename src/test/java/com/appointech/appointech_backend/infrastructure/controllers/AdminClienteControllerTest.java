package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.ClientePorIdNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DatosActualizacionCliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarClientesUseCase;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarClienteRequest;
import com.appointech.appointech_backend.infrastructure.dto.ClienteAdminResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminClienteControllerTest {

    private ListarClientesUseCase listar;
    private ActualizarClienteUseCase actualizar;
    private AdminClienteController controller;

    @BeforeEach
    void preparar() {
        listar = mock(ListarClientesUseCase.class);
        actualizar = mock(ActualizarClienteUseCase.class);
        controller = new AdminClienteController(listar, actualizar);
    }

    @Test
    void soloAdministradorPuedeUsarElControlador() {
        PreAuthorize anotacion = AdminClienteController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertTrue(anotacion.value().contains("ADMINISTRADOR"));
    }

    @Test
    void listarResponde200ConArregloDirecto() {
        when(listar.listar()).thenReturn(List.of(cliente()));

        ResponseEntity<List<ClienteAdminResponse>> r = controller.listar();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        ClienteAdminResponse c = r.getBody().get(0);
        assertEquals(2L, c.id());
        assertEquals(8L, c.usuarioId());
        assertEquals("Ana", c.nombre());
        assertEquals("ana@x.com", c.correo());
        assertEquals(TipoDocumento.CC, c.tipoDocumento());
        assertEquals("123", c.numeroDocumento());
        assertTrue(c.activo());
        assertEquals("Calle 1", c.direccion());
        assertEquals(4.3365, c.latitud());
        assertEquals(-74.3638, c.longitud());
    }

    @Test
    void actualizarPasaTodosLosCamposYResponde200() {
        when(actualizar.actualizar(eq(2L), any())).thenReturn(cliente());

        ResponseEntity<ClienteAdminResponse> r = controller.actualizar(2L, new ActualizarClienteRequest(
                "Luis", "Mora", "l@x.com", "311", TipoDocumento.CE, "55", "Carrera 9", 4.5, -74.1));

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(2L, r.getBody().id());
        ArgumentCaptor<DatosActualizacionCliente> captor = ArgumentCaptor.forClass(DatosActualizacionCliente.class);
        verify(actualizar).actualizar(eq(2L), captor.capture());
        DatosActualizacionCliente d = captor.getValue();
        assertEquals("Luis", d.nombre());
        assertEquals("Mora", d.apellido());
        assertEquals("l@x.com", d.correo());
        assertEquals("311", d.telefono());
        assertEquals(TipoDocumento.CE, d.tipoDocumento());
        assertEquals("55", d.numeroDocumento());
        assertEquals("Carrera 9", d.direccion());
        assertEquals(4.5, d.latitud());
        assertEquals(-74.1, d.longitud());
    }

    @Test
    void clienteInexistentePropagaLaExcepcionYSeTraduceA404() {
        when(actualizar.actualizar(eq(9L), any())).thenThrow(new ClientePorIdNoEncontradoException(9L));

        ClientePorIdNoEncontradoException ex = assertThrows(ClientePorIdNoEncontradoException.class,
                () -> controller.actualizar(9L, new ActualizarClienteRequest(null, null, null, null, null, null,
                        null, null, null)));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin/clientes/9");
        ProblemDetail problema = new GlobalExceptionHandler().manejarClientePorIdNoEncontrado(ex, request);
        assertEquals(404, problema.getStatus());
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
