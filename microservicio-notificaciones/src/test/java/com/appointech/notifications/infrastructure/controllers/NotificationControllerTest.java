package com.appointech.notifications.infrastructure.controllers;

import com.appointech.notifications.domain.ports.EmailSender;
import com.appointech.notifications.infrastructure.config.NotificationConfig;
import com.appointech.notifications.infrastructure.security.JwtFilter;
import com.appointech.notifications.infrastructure.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real security and email service; only the output port (SMTP) is mocked. */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableWebSecurity
@Import({SecurityConfig.class, JwtFilter.class, NotificationConfig.class})
@TestPropertySource(properties = "jwt.secret=" + NotificationControllerTest.SECRET)
class NotificationControllerTest {

    static final String SECRET = "secreto-de-prueba-del-microservicio-con-mas-de-32-caracteres";

    private static final String CONFIRMED = """
            {"citaId":42,"correoCliente":"ana@mail.com","nombreCliente":"Ana Lopez","servicio":"Plomería",
             "tecnico":"Luis Perez","fecha":"2026-11-03","hora":"10:00","direccion":"Calle 1"}""";
    private static final String COMPLETED = """
            {"citaId":42,"correoCliente":"ana@mail.com","nombreCliente":"Ana Lopez","servicio":"Plomería",
             "tecnico":"Luis Perez","diagnostico":"Sifón roto"}""";

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;
    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void setUpMockMvc() {
        // The real security chain (with JwtFilter inside) is applied once, as at runtime.
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String token() {
        return "Bearer " + Jwts.builder()
                .subject("tec@mail.com")
                .claim("rol", "TECNICO")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/notificaciones/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"));
    }

    @Test
    void confirmedAppointmentRespondsAcceptedAndSendsTheEmail() throws Exception {
        mvc.perform(post("/api/notificaciones/cita-confirmada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIRMED))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.citaId").value(42));

        verify(emailSender).send(eq("ana@mail.com"), eq("Tu cita en AppoinTech fue confirmada"),
                contains("Hola Ana Lopez"));
    }

    @Test
    void completedAppointmentRespondsAcceptedAndSendsTheEmailWithTheDiagnosis() throws Exception {
        mvc.perform(post("/api/notificaciones/cita-finalizada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETED))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.citaId").value(42));

        verify(emailSender).send(eq("ana@mail.com"), eq("Tu servicio fue finalizado"), contains("Sifón roto"));
    }

    @Test
    void ifSendingFailsItStillRespondsAccepted() throws Exception {
        doThrow(new IllegalStateException("smtp down")).when(emailSender).send(any(), any(), any());

        mvc.perform(post("/api/notificaciones/cita-confirmada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIRMED))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recibido").value(true));
        mvc.perform(post("/api/notificaciones/cita-finalizada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETED))
                .andExpect(status().isAccepted());

        verify(emailSender, times(2)).send(any(), any(), any());
    }

    @Test
    void invalidOrMissingEmailRespondsBadRequestWithoutSending() throws Exception {
        String invalid = CONFIRMED.replace("ana@mail.com", "no-es-un-correo");
        String missing = COMPLETED.replace("\"correoCliente\":\"ana@mail.com\",", "");
        String blank = COMPLETED.replace("ana@mail.com", "  ");

        mvc.perform(post("/api/notificaciones/cita-confirmada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notificaciones/cita-finalizada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(missing))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notificaciones/cita-finalizada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(blank))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(emailSender);
    }

    @Test
    void withoutTokenRespondsUnauthorized() throws Exception {
        mvc.perform(post("/api/notificaciones/cita-confirmada")
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIRMED))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notificaciones/cita-finalizada")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETED))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(emailSender);
    }

    @Test
    void aTokenSignedWithAnotherKeyRespondsUnauthorized() throws Exception {
        String foreign = "Bearer " + Jwts.builder().subject("x@mail.com").claim("rol", "TECNICO")
                .signWith(Keys.hmacShaKeyFor("otra-clave-distinta-con-mas-de-32-caracteres-ok".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mvc.perform(post("/api/notificaciones/cita-finalizada").header("Authorization", foreign)
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETED))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(emailSender);
    }

    @Test
    void appointmentUpdatedLogsTheEventAndRespondsAccepted() throws Exception {
        mvc.perform(post("/api/notificaciones/cita-actualizada").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"citaId\":7,\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.citaId").value(7));
    }
}
