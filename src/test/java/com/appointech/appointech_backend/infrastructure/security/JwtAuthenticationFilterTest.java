package com.appointech.appointech_backend.infrastructure.security;

import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.infrastructure.adapters.JwtTokenGeneratorAdapter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String SECRETO = "secreto-de-prueba-con-al-menos-32-bytes-0123456789";

    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain cadena;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
        filtro = new JwtAuthenticationFilter(SECRETO);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        cadena = new MockFilterChain();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private String tokenValido() {
        Usuario u = new Usuario();
        u.setId(5L);
        u.setCorreo("ana@mail.com");
        u.setRol(RolUsuario.TECNICO);
        return new JwtTokenGeneratorAdapter(SECRETO, 60_000).generarToken(u);
    }

    @Test
    void sinCabeceraContinuaSinAutenticar() throws Exception {
        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest(), "la cadena debe continuar");
    }

    @Test
    void cabeceraQueNoEsBearerSeIgnora() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void tokenValidoAutenticaConCorreoYRol() throws Exception {
        request.addHeader("Authorization", "Bearer " + tokenValido());

        filtro.doFilter(request, response, cadena);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ana@mail.com", auth.getPrincipal());
        assertEquals("ROLE_TECNICO", auth.getAuthorities().iterator().next().getAuthority());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void tokenMalformadoNoAutenticaPeroLaCadenaContinua() throws Exception {
        request.addHeader("Authorization", "Bearer esto-no-es-un-jwt");

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() throws Exception {
        String ajeno = Jwts.builder().subject("x@x.com").claim("rol", "ADMINISTRADOR")
                .signWith(Keys.hmacShaKeyFor("otra-clave-distinta-con-32-bytes-o-mas-0000".getBytes(StandardCharsets.UTF_8)))
                .compact();
        request.addHeader("Authorization", "Bearer " + ajeno);

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenExpiradoSeRechaza() throws Exception {
        String expirado = Jwts.builder().subject("ana@mail.com").claim("rol", "CLIENTE")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();
        request.addHeader("Authorization", "Bearer " + expirado);

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }
}
