package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.EspecialidadNombreDuplicadoException;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EspecialidadAdminInfo;
import com.appointech.appointech_backend.domain.ports.in.ActualizarEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.in.CambiarEstadoEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarEspecialidadesAdminUseCase;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminEspecialidadControllerTest {

    private ListarEspecialidadesAdminUseCase listar;
    private CrearEspecialidadUseCase crear;
    private ActualizarEspecialidadUseCase actualizar;
    private CambiarEstadoEspecialidadUseCase cambiarEstado;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        listar = mock(ListarEspecialidadesAdminUseCase.class);
        crear = mock(CrearEspecialidadUseCase.class);
        actualizar = mock(ActualizarEspecialidadUseCase.class);
        cambiarEstado = mock(CambiarEstadoEspecialidadUseCase.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new AdminEspecialidadController(listar, crear, actualizar, cambiarEstado))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Especialidad esp(Long id, String nombre, boolean activo) {
        Especialidad e = new Especialidad();
        e.setId(id);
        e.setNombre(nombre);
        e.setDescripcion("desc");
        e.setActivo(activo);
        return e;
    }

    @Test
    void soloAdministradorPuedeUsarElControlador() {
        PreAuthorize anotacion = AdminEspecialidadController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertTrue(anotacion.value().contains("ADMINISTRADOR"));
    }

    @Test
    void listarIncluyeElConteoDeTecnicosAsignados() throws Exception {
        when(listar.listar()).thenReturn(List.of(new EspecialidadAdminInfo(esp(1L, "Plomeria", true), 3)));

        mvc.perform(get("/api/admin/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos[0].nombre").value("Plomeria"))
                .andExpect(jsonPath("$.datos[0].tecnicosAsignados").value(3));
    }

    @Test
    void crearResponde201ConConteoCero() throws Exception {
        when(crear.crear("Gas", "Instalaciones")).thenReturn(esp(4L, "Gas", true));

        mvc.perform(post("/api/admin/especialidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Gas\",\"descripcion\":\"Instalaciones\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Especialidad creada correctamente"))
                .andExpect(jsonPath("$.datos.id").value(4))
                .andExpect(jsonPath("$.datos.tecnicosAsignados").value(0));
    }

    @Test
    void crearConNombreDuplicadoResponde409() throws Exception {
        when(crear.crear(any(), any())).thenThrow(new EspecialidadNombreDuplicadoException("Gas"));

        mvc.perform(post("/api/admin/especialidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Gas\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crearSinNombreResponde400() throws Exception {
        mvc.perform(post("/api/admin/especialidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(crear);
    }

    @Test
    void actualizarResponde200() throws Exception {
        when(actualizar.actualizar(4L, "Gas natural", "d")).thenReturn(esp(4L, "Gas natural", true));

        mvc.perform(patch("/api/admin/especialidades/4").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Gas natural\",\"descripcion\":\"d\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Especialidad actualizada correctamente"))
                .andExpect(jsonPath("$.datos.nombre").value("Gas natural"));
    }

    @Test
    void cambiarEstadoUsaMensajeSegunActivarODesactivar() throws Exception {
        when(cambiarEstado.cambiarEstado(4L, true)).thenReturn(esp(4L, "Gas", true));
        when(cambiarEstado.cambiarEstado(4L, false)).thenReturn(esp(4L, "Gas", false));

        mvc.perform(patch("/api/admin/especialidades/4/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":true}"))
                .andExpect(jsonPath("$.mensaje").value("Especialidad activada correctamente"))
                .andExpect(jsonPath("$.datos.activo").value(true));
        mvc.perform(patch("/api/admin/especialidades/4/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(jsonPath("$.mensaje").value("Especialidad desactivada correctamente"))
                .andExpect(jsonPath("$.datos.activo").value(false));
    }
}
