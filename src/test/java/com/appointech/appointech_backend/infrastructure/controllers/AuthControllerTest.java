package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.AutenticarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private RegistrarClienteUseCase registrarCliente;
    private RegistrarTecnicoUseCase registrarTecnico;
    private AutenticarUsuarioUseCase autenticar;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        registrarCliente = mock(RegistrarClienteUseCase.class);
        registrarTecnico = mock(RegistrarTecnicoUseCase.class);
        autenticar = mock(AutenticarUsuarioUseCase.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new AuthController(registrarCliente, registrarTecnico, autenticar))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Usuario usuario(RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(4L);
        u.setNombre("Ana");
        u.setApellido("Lopez");
        u.setCorreo("ana@mail.com");
        u.setRol(rol);
        return u;
    }

    private static final String CLIENTE_JSON = """
            {"nombre":"Ana","apellido":"Lopez","correo":"ana@mail.com","contrasena":"clave12345",
             "telefono":"3001112233","tipoDocumento":"CC","numeroDocumento":"123","direccion":"Calle 1"}""";

    @Test
    void registrarClienteResponde201ConElUsuarioCreado() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario(RolUsuario.CLIENTE));
        when(registrarCliente.registrar("Ana", "Lopez", "ana@mail.com", "clave12345",
                "3001112233", TipoDocumento.CC, "123", "Calle 1")).thenReturn(cliente);

        mvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.mensaje").value("Cliente registrado correctamente"))
                .andExpect(jsonPath("$.datos.id").value(4))
                .andExpect(jsonPath("$.datos.correo").value("ana@mail.com"))
                .andExpect(jsonPath("$.datos.rol").value("CLIENTE"));
    }

    @Test
    void registrarClienteConUsuarioSinRolResponde201ConRolNulo() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario(null));
        when(registrarCliente.registrar("Ana", "Lopez", "ana@mail.com", "clave12345",
                "3001112233", TipoDocumento.CC, "123", "Calle 1")).thenReturn(cliente);

        mvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.id").value(4))
                .andExpect(jsonPath("$.datos.rol").doesNotExist());
    }

    @Test
    void registrarClienteConCorreoDuplicadoResponde409() throws Exception {
        when(registrarCliente.registrar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new CorreoYaRegistradoException("ana@mail.com"));

        mvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.instance").value("/api/auth/registro/cliente"));
    }

    @Test
    void registrarClienteInvalidoResponde400SinLlamarAlCasoDeUso() throws Exception {
        String sinCorreoYContrasenaCorta = """
                {"nombre":"Ana","apellido":"Lopez","correo":"no-es-correo","contrasena":"corta",
                 "tipoDocumento":"CC","direccion":"Calle 1"}""";

        mvc.perform(post("/api/auth/registro/cliente").contentType(MediaType.APPLICATION_JSON)
                        .content(sinCorreoYContrasenaCorta))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrarCliente);
    }

    @Test
    void registrarTecnicoResponde201ConRolTecnico() throws Exception {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario(RolUsuario.TECNICO));
        when(registrarTecnico.registrar(eq("Ana"), eq("Lopez"), eq("ana@mail.com"), eq("clave12345"),
                eq("3001112233"), eq(TipoDocumento.CC), eq("123"), eq("Base 1"), eq(12), anyDouble(), anyDouble()))
                .thenReturn(tecnico);
        String json = """
                {"nombre":"Ana","apellido":"Lopez","correo":"ana@mail.com","contrasena":"clave12345",
                 "telefono":"3001112233","tipoDocumento":"CC","numeroDocumento":"123",
                 "direccionBase":"Base 1","radioCoberturaKm":12,"latitudBase":4.6,"longitudBase":-74.1}""";

        mvc.perform(post("/api/auth/registro/tecnico").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Técnico registrado correctamente"))
                .andExpect(jsonPath("$.datos.rol").value("TECNICO"));
    }

    @Test
    void registrarTecnicoSinDireccionBaseResponde400() throws Exception {
        String json = """
                {"nombre":"Ana","apellido":"Lopez","correo":"ana@mail.com","contrasena":"clave12345",
                 "tipoDocumento":"CC"}""";

        mvc.perform(post("/api/auth/registro/tecnico").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrarTecnico);
    }

    @Test
    void loginExitosoDevuelveTokenYPerfilCompleto() throws Exception {
        when(autenticar.autenticar("ana@mail.com", "clave12345"))
                .thenReturn(new AutenticarUsuarioUseCase.ResultadoAutenticacion("jwt-token", true));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\",\"contrasena\":\"clave12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso"))
                .andExpect(jsonPath("$.datos.token").value("jwt-token"))
                .andExpect(jsonPath("$.datos.perfilCompleto").value(true));
        verify(autenticar).autenticar("ana@mail.com", "clave12345");
    }

    @Test
    void loginConCredencialesInvalidasResponde401() throws Exception {
        when(autenticar.autenticar(any(), any())).thenThrow(new CredencialesInvalidasException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\",\"contrasena\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinContrasenaResponde400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@mail.com\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autenticar);
    }
}
