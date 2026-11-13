package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.CambiarEstadoUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearAdministradorUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarUsuariosUseCase;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUsuarioControllerTest {

    private CrearAdministradorUseCase crear;
    private ListarUsuariosUseCase listar;
    private ActualizarUsuarioUseCase actualizar;
    private CambiarEstadoUsuarioUseCase cambiarEstado;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        crear = mock(CrearAdministradorUseCase.class);
        listar = mock(ListarUsuariosUseCase.class);
        actualizar = mock(ActualizarUsuarioUseCase.class);
        cambiarEstado = mock(CambiarEstadoUsuarioUseCase.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new AdminUsuarioController(crear, listar, actualizar, cambiarEstado))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Usuario usuario(Long id, TipoDocumento tipo, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Ana");
        u.setApellido("Lopez");
        u.setCorreo("ana@mail.com");
        u.setTelefono("300");
        u.setTipoDocumento(tipo);
        u.setNumeroDocumento("123");
        u.setRol(RolUsuario.ADMINISTRADOR);
        u.setActivo(activo);
        u.setCreadoEn(LocalDateTime.of(2026, 1, 1, 0, 0));
        return u;
    }

    @Test
    void soloAdministradorPuedeUsarElControlador() {
        PreAuthorize anotacion = AdminUsuarioController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertTrue(anotacion.value().contains("ADMINISTRADOR"));
    }

    @Test
    void listarDevuelveArregloConTipoDocumentoNuloCuandoNoHay() throws Exception {
        when(listar.listar()).thenReturn(List.of(usuario(1L, TipoDocumento.CC, true), usuario(2L, null, false)));

        mvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipoDocumento").value("CC"))
                .andExpect(jsonPath("$[0].rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$[1].tipoDocumento").doesNotExist())
                .andExpect(jsonPath("$[1].activo").value(false));
    }

    @Test
    void listarConPageYSizeDevuelveSoloLaPaginaYElTotalEnLaCabecera() throws Exception {
        when(listar.listarPagina(PaginacionOpcional.de(1, 2)))
                .thenReturn(new ListadoConTotal<>(List.of(usuario(3L, TipoDocumento.CC, true)), 7));

        mvc.perform(get("/api/admin/usuarios").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "7"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3));
        verify(listar, never()).listar();
    }

    @Test
    void listarConSoloPageUsaElTamanioPorDefecto() throws Exception {
        when(listar.listarPagina(PaginacionOpcional.de(0, null)))
                .thenReturn(new ListadoConTotal<>(List.of(), 0));

        mvc.perform(get("/api/admin/usuarios").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
        verify(listar).listarPagina(new PaginacionOpcional(0, PaginacionOpcional.TAMANIO_POR_DEFECTO));
    }

    @Test
    void listarSinPaginacionSigueDevolviendoTodosSinCabeceraDeTotal() throws Exception {
        when(listar.listar()).thenReturn(List.of(usuario(1L, TipoDocumento.CC, true)));

        mvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"));
        verify(listar, never()).listarPagina(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listarConSizeFueraDeRangoEsUnaPeticionInvalida() throws Exception {
        mvc.perform(get("/api/admin/usuarios").param("page", "0").param("size", "500"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void listarConUsuarioSinRolDevuelveRolNuloSinFallar() throws Exception {
        Usuario sinRol = usuario(3L, TipoDocumento.CC, true);
        sinRol.setRol(null);
        when(listar.listar()).thenReturn(List.of(sinRol));

        mvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].rol").doesNotExist());
    }

    @Test
    void crearAdministradorResponde201() throws Exception {
        when(crear.crear("Ana", "Lopez", "ana@mail.com", "clave12345", "300", TipoDocumento.CE, "123"))
                .thenReturn(usuario(9L, TipoDocumento.CE, true));

        mvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ana","apellido":"Lopez","correo":"ana@mail.com","contrasena":"clave12345",
                         "telefono":"300","tipoDocumento":"CE","numeroDocumento":"123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.tipoDocumento").value("CE"));
    }

    @Test
    void crearAdministradorConContrasenaCortaResponde400() throws Exception {
        mvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ana","apellido":"Lopez","correo":"ana@mail.com","contrasena":"corta",
                         "tipoDocumento":"CE"}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(crear);
    }

    @Test
    void actualizarDelegaEnElCasoDeUso() throws Exception {
        when(actualizar.actualizar(4L, "Nuevo", "Apellido", "311")).thenReturn(usuario(4L, null, true));

        mvc.perform(patch("/api/admin/usuarios/4").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"apellido\":\"Apellido\",\"telefono\":\"311\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
        verify(actualizar).actualizar(4L, "Nuevo", "Apellido", "311");
    }

    @Test
    void actualizarUsuarioInexistenteResponde404() throws Exception {
        when(actualizar.actualizar(any(), any(), any(), any())).thenThrow(new UsuarioNoEncontradoException(99L));

        mvc.perform(patch("/api/admin/usuarios/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"N\",\"apellido\":\"A\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstadoPasaElBooleanoAlCasoDeUso() throws Exception {
        when(cambiarEstado.cambiarEstado(4L, false)).thenReturn(usuario(4L, null, false));

        mvc.perform(patch("/api/admin/usuarios/4/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
