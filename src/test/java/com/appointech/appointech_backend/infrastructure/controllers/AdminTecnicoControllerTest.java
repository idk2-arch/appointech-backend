package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.DatosActualizacionTecnico;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ActualizarUbicacionTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearTecnicoConEspecialidadesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarTecnicosUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.ApiResponse;
import com.appointech.appointech_backend.infrastructure.dto.CrearTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTecnicoControllerTest {

    private CrearTecnicoConEspecialidadesUseCase crear;
    private ActualizarTecnicoUseCase actualizar;
    private AdminTecnicoController controller;

    @BeforeEach
    void preparar() {
        crear = mock(CrearTecnicoConEspecialidadesUseCase.class);
        actualizar = mock(ActualizarTecnicoUseCase.class);
        controller = new AdminTecnicoController(crear, actualizar, mock(ActualizarUbicacionTecnicoUseCase.class));
    }

    @Test
    void soloAdministradorPuedeUsarElControlador() {
        PreAuthorize anotacion = AdminTecnicoController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertTrue(anotacion.value().contains("ADMINISTRADOR"));
    }

    @Test
    void crearResponde201ConTecnicoCompletoYNormalizaBlancos() {
        when(crear.crear(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(tecnico());

        ResponseEntity<ApiResponse<TecnicoResponse>> respuesta = controller.crear(new CrearTecnicoRequest(
                "Ana", "Ruiz", "ana@x.com", "clave12345", " ", TipoDocumento.CC, "", "Calle 1",
                null, 4.3365, -74.3638, List.of(1L)));

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        TecnicoResponse datos = respuesta.getBody().datos();
        assertEquals(3L, datos.id());
        assertEquals(7L, datos.usuarioId());
        assertEquals("311", datos.telefono());
        assertEquals(TipoDocumento.CC, datos.tipoDocumento());
        assertEquals("123", datos.numeroDocumento());
        assertTrue(datos.activo());
        assertEquals(4.3365, datos.latitudBase());
        assertEquals(-74.3638, datos.longitudBase());
        assertEquals(1, datos.especialidades().size());

        verify(crear).crear(eq("Ana"), eq("Ruiz"), eq("ana@x.com"), eq("clave12345"), eq((String) null),
                eq(TipoDocumento.CC), eq((String) null), eq("Calle 1"), eq((Integer) null), eq(4.3365),
                eq(-74.3638), eq(List.of(1L)));
    }

    @Test
    void actualizarPasaTodosLosCamposAlCasoDeUsoYResponde200() {
        when(actualizar.actualizar(eq(3L), any())).thenReturn(tecnico());

        ResponseEntity<TecnicoResponse> respuesta = controller.actualizar(3L, new ActualizarTecnicoRequest(
                "Luis", "Mora", "l@x.com", "300", TipoDocumento.CE, "55", "Carrera 9", 20, 4.5, -74.1, List.of(2L)));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(3L, respuesta.getBody().id());

        ArgumentCaptor<DatosActualizacionTecnico> captor = ArgumentCaptor.forClass(DatosActualizacionTecnico.class);
        verify(actualizar).actualizar(eq(3L), captor.capture());
        DatosActualizacionTecnico d = captor.getValue();
        assertEquals("Luis", d.nombre());
        assertEquals("Mora", d.apellido());
        assertEquals("l@x.com", d.correo());
        assertEquals("300", d.telefono());
        assertEquals(TipoDocumento.CE, d.tipoDocumento());
        assertEquals("55", d.numeroDocumento());
        assertEquals("Carrera 9", d.direccionBase());
        assertNull(d.radioCoberturaKm());   // el radio por técnico se ignora (límite único del sistema)
        assertEquals(4.5, d.latitudBase());
        assertEquals(-74.1, d.longitudBase());
        assertEquals(List.of(2L), d.especialidadIds());
    }

    @Test
    void tecnicoControllerExponeUbicacionYDatosDeUsuarioEnLaLista() {
        ListarTecnicosUseCase listar = mock(ListarTecnicosUseCase.class);
        when(listar.listar()).thenReturn(List.of(tecnico()));
        TecnicoController tecnicoController =
                new TecnicoController(mock(AsignarEspecialidadesTecnicoUseCase.class), listar);

        TecnicoResponse r = tecnicoController.listar().getBody().get(0);

        assertEquals(4.3365, r.latitudBase());
        assertEquals(7L, r.usuarioId());
        assertEquals("123", r.numeroDocumento());
        assertEquals("Ana", r.nombre());
    }

    @Test
    void ubicacionNulaSeSerializaComoNulo() {
        Tecnico t = tecnico();
        t.setLatitudBase(null);
        t.setLongitudBase(null);
        TecnicoResponse r = TecnicoResponseMapper.aRespuesta(t);
        assertNull(r.latitudBase());
        assertNull(r.longitudBase());
    }

    private static Tecnico tecnico() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setNombre("Ana");
        u.setApellido("Ruiz");
        u.setCorreo("ana@x.com");
        u.setTelefono("311");
        u.setTipoDocumento(TipoDocumento.CC);
        u.setNumeroDocumento("123");
        u.setRol(RolUsuario.TECNICO);
        u.setActivo(true);
        Especialidad e = new Especialidad();
        e.setId(1L);
        e.setNombre("Plomería");
        Tecnico t = new Tecnico();
        t.setId(3L);
        t.setUsuario(u);
        t.setDireccionBase("Calle 1");
        t.setRadioCoberturaKm(10);
        t.setLatitudBase(4.3365);
        t.setLongitudBase(-74.3638);
        t.setEspecialidades(List.of(e));
        return t;
    }
}
