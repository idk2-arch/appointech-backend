package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.BloqueInvalidoException;
import com.appointech.appointech_backend.domain.exception.BloqueNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.BloqueSolapadoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasDelTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearBloqueUseCase;
import com.appointech.appointech_backend.domain.ports.in.EliminarBloqueUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarBloquesUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.AgendamientoExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TecnicoBloqueYCitaControllersTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 3, 9, 0);
    private final Authentication tecnico = new UsernamePasswordAuthenticationToken("tec@mail.com", null);

    private CrearBloqueUseCase crear;
    private ListarBloquesUseCase listar;
    private EliminarBloqueUseCase eliminar;
    private ConsultarCitasDelTecnicoUseCase consultarCitas;
    private RegistroLogPort logs;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        crear = mock(CrearBloqueUseCase.class);
        listar = mock(ListarBloquesUseCase.class);
        eliminar = mock(EliminarBloqueUseCase.class);
        consultarCitas = mock(ConsultarCitasDelTecnicoUseCase.class);
        logs = mock(RegistroLogPort.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new TecnicoBloqueController(crear, listar, eliminar),
                        new TecnicoCitaController(consultarCitas))
                .setControllerAdvice(new GlobalExceptionHandler(), new AgendamientoExceptionHandler(logs))
                .build();
        SecurityContextHolder.clearContext();
    }

    private BloqueIndisponibilidad bloque(Long id) {
        return new BloqueIndisponibilidad(id, 1L, INICIO, INICIO.plusHours(2), "Almuerzo");
    }

    @Test
    void crearBloqueResponde201ConElBloqueCreado() throws Exception {
        when(crear.crear("tec@mail.com", INICIO, INICIO.plusHours(2), "Almuerzo")).thenReturn(bloque(6L));

        mvc.perform(post("/api/tecnico/bloques").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-08-03T09:00:00\",\"fin\":\"2026-08-03T11:00:00\",\"motivo\":\"Almuerzo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.motivo").value("Almuerzo"));
    }

    @Test
    void crearBloqueSinFechasResponde400() throws Exception {
        mvc.perform(post("/api/tecnico/bloques").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(crear);
    }

    @Test
    void crearBloqueSolapadoResponde409YRegistraAdvertencia() throws Exception {
        when(crear.crear(any(), any(), any(), any())).thenThrow(new BloqueSolapadoException("Se solapa"));

        mvc.perform(post("/api/tecnico/bloques").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-08-03T09:00:00\",\"fin\":\"2026-08-03T11:00:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Se solapa"))
                .andExpect(jsonPath("$.status").value(409));
        verify(logs).registrar(eq(com.appointech.appointech_backend.domain.models.NivelLog.WARN),
                eq(com.appointech.appointech_backend.domain.models.TipoLog.NEGOCIO),
                eq("AgendamientoExceptionHandler"), any(), eq("409 - Se solapa"));
    }

    @Test
    void crearBloqueInvalidoResponde400() throws Exception {
        when(crear.crear(any(), any(), any(), any())).thenThrow(new BloqueInvalidoException("Fin antes del inicio"));

        mvc.perform(post("/api/tecnico/bloques").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-08-03T09:00:00\",\"fin\":\"2026-08-03T08:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fin antes del inicio"));
    }

    @Test
    void listarBloquesPasaLasFechasOpcionales() throws Exception {
        when(listar.listar("tec@mail.com", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(bloque(1L), bloque(2L)));
        when(listar.listar("tec@mail.com", null, null)).thenReturn(List.of());

        mvc.perform(get("/api/tecnico/bloques").principal(tecnico).param("desde", "2026-08-01").param("hasta", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/tecnico/bloques").principal(tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eliminarBloqueResponde204() throws Exception {
        mvc.perform(delete("/api/tecnico/bloques/6").principal(tecnico))
                .andExpect(status().isNoContent());

        verify(eliminar).eliminar("tec@mail.com", 6L);
    }

    @Test
    void eliminarBloqueInexistenteResponde404() throws Exception {
        org.mockito.Mockito.doThrow(new BloqueNoEncontradoException(6L)).when(eliminar).eliminar(any(), eq(6L));

        mvc.perform(delete("/api/tecnico/bloques/6").principal(tecnico))
                .andExpect(status().isNotFound());
    }

    @Test
    void citasDelDiaMapeaCitaYCliente() throws Exception {
        Cita cita = new Cita();
        cita.setId(9L);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(INICIO);
        cita.setFechaHoraFin(INICIO.plusHours(1));
        when(consultarCitas.consultar("tec@mail.com", LocalDate.of(2026, 8, 3)))
                .thenReturn(List.of(new CitaConCliente(cita, new ClienteResumen("Luis", "Perez", "300"))));

        mvc.perform(get("/api/tecnico/citas").principal(tecnico).param("fecha", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$[0].cliente.nombre").value("Luis"));
    }

    @Test
    void citasDelDiaSinFechaResponde400() throws Exception {
        mvc.perform(get("/api/tecnico/citas").principal(tecnico))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(consultarCitas);
    }
}
