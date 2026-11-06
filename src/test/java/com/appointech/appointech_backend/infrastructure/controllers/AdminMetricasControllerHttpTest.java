package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.CriteriosMetricas;
import com.appointech.appointech_backend.domain.ports.in.ConsultarMetricasUseCase;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.SecurityConfig;
import com.appointech.appointech_backend.infrastructure.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/admin/metricas con la seguridad real (JWT y roles) y el manejador de errores real. */
@WebMvcTest(value = AdminMetricasController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminMetricasControllerHttpTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA = "/api/admin/metricas";

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @MockitoBean
    private ConsultarMetricasUseCase consultar;
    @MockitoBean
    private com.appointech.appointech_backend.domain.ports.out.RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String rol) {
        return "Bearer " + Jwts.builder().subject("u@mail.com").claim("rol", rol)
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void administradorRecibe200ConLaEstructuraCompletaAgrupada() throws Exception {
        when(consultar.consultar(any())).thenReturn(MetricasMapperTest.ejemplo());

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rango.desde").value("2030-05-10"))
                .andExpect(jsonPath("$.rango.hasta").value("2030-05-12"))
                .andExpect(jsonPath("$.rango.dias").value(3))
                .andExpect(jsonPath("$.citas.total").value(8))
                .andExpect(jsonPath("$.citas.porEstado.FINALIZADA").value(6))
                .andExpect(jsonPath("$.citas.porEstado.EN_CAMINO").value(0))
                .andExpect(jsonPath("$.citas.tasaCancelacion").value(25.0))
                .andExpect(jsonPath("$.solicitudesEspeciales.total").value(3))
                .andExpect(jsonPath("$.solicitudesEspeciales.porMotivo.FUERA_DE_COBERTURA").value(2))
                .andExpect(jsonPath("$.solicitudesEspeciales.porMotivo.SIN_TECNICOS_ESPECIALIDAD").value(1))
                .andExpect(jsonPath("$.ingresos.total").value(140000.75))
                .andExpect(jsonPath("$.ingresos.cantidadPagos").value(2))
                .andExpect(jsonPath("$.ingresos.porMetodo.EFECTIVO.monto").value(90000.50))
                .andExpect(jsonPath("$.ingresos.porMetodo.EN_LINEA.cantidad").value(1))
                .andExpect(jsonPath("$.ingresos.porCobrar.cantidad").value(1))
                .andExpect(jsonPath("$.ingresos.porCobrar.monto").value(30000))
                .andExpect(jsonPath("$.especialidades.length()").value(2))
                .andExpect(jsonPath("$.especialidades[0].nombre").value("Hornos"))
                .andExpect(jsonPath("$.especialidades[0].totalCitas").value(0))
                .andExpect(jsonPath("$.especialidades[1].finalizadas").value(3))
                .andExpect(jsonPath("$.calificaciones.promedio").value(4.0))
                .andExpect(jsonPath("$.calificaciones.total").value(3))
                .andExpect(jsonPath("$.calificaciones.porTecnico[0].nombre").value("Luis Mora"))
                .andExpect(jsonPath("$.calificaciones.porTecnico[0].promedio").value(4.5))
                .andExpect(jsonPath("$.usuariosActivos.total").value(14))
                .andExpect(jsonPath("$.usuariosActivos.porRol.CLIENTE").value(10));
    }

    @Test
    void sinParametrosNoEnviaFechasYElCasoDeUsoPoneElRangoPorDefecto() throws Exception {
        when(consultar.consultar(any())).thenReturn(MetricasMapperTest.ejemplo());

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))).andExpect(status().isOk());

        ArgumentCaptor<CriteriosMetricas> captor = ArgumentCaptor.forClass(CriteriosMetricas.class);
        verify(consultar).consultar(captor.capture());
        assertEquals(new CriteriosMetricas(null, null), captor.getValue());
    }

    @Test
    void pasaLasFechasDelRango() throws Exception {
        when(consultar.consultar(any())).thenReturn(MetricasMapperTest.ejemplo());

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))
                        .param("desde", "2030-05-10").param("hasta", "2030-05-12"))
                .andExpect(status().isOk());

        ArgumentCaptor<CriteriosMetricas> captor = ArgumentCaptor.forClass(CriteriosMetricas.class);
        verify(consultar).consultar(captor.capture());
        assertEquals(new CriteriosMetricas(LocalDate.of(2030, 5, 10), LocalDate.of(2030, 5, 12)), captor.getValue());
    }

    @Test
    void fechaMalEscritaEs400ConProblemDetailSinLlegarAlCasoDeUso() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("desde", "10/05/2030"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("desde debe tener el formato yyyy-MM-dd"))
                .andExpect(jsonPath("$.instance").value(RUTA));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("hasta", "ayer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("hasta debe tener el formato yyyy-MM-dd"));
        verifyNoInteractions(consultar);
    }

    @Test
    void rangoInvertidoYRangoMayorA366DiasSon400() throws Exception {
        when(consultar.consultar(any()))
                .thenThrow(new FiltroCitasInvalidoException("desde no puede ser posterior a hasta"))
                .thenThrow(new FiltroCitasInvalidoException("El rango no puede superar 366 días"));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))
                        .param("desde", "2030-06-02").param("hasta", "2030-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("desde no puede ser posterior a hasta"));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))
                        .param("desde", "2028-01-01").param("hasta", "2030-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El rango no puede superar 366 días"));
    }

    @Test
    void clienteYTecnicoRecibenElMismo403YSinTokenEs401() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA).header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultar);
    }
}
