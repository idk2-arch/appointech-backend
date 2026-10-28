package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort.GoogleUser;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenVerifierAdapterTest {

    private GoogleTokenVerifierAdapter adapter;
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void preparar() {
        adapter = new GoogleTokenVerifierAdapter("client-id-de-prueba.apps.googleusercontent.com");
        verifier = mock(GoogleIdTokenVerifier.class);
        ReflectionTestUtils.setField(adapter, "verifier", verifier);
    }

    private GoogleIdToken token(Boolean correoVerificado, String given, String family, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("g-123");
        payload.setEmail("ana@gmail.com");
        payload.setEmailVerified(correoVerificado);
        if (given != null) {
            payload.set("given_name", given);
        }
        if (family != null) {
            payload.set("family_name", family);
        }
        if (name != null) {
            payload.set("name", name);
        }
        return new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
    }

    @Test
    void tokenValidoDevuelveElUsuarioDeGoogle() throws Exception {
        when(verifier.verify("t")).thenReturn(token(true, "Ana", "Lopez", null));

        GoogleUser u = adapter.verificar("t").orElseThrow();

        assertEquals("g-123", u.googleId());
        assertEquals("ana@gmail.com", u.correo());
        assertEquals("Ana", u.nombre());
        assertEquals("Lopez", u.apellido());
    }

    @Test
    void sinNombreDePilaUsaElNombreCompletoYApellidoVacio() throws Exception {
        when(verifier.verify("t")).thenReturn(token(true, null, null, "Ana Lopez"));

        GoogleUser u = adapter.verificar("t").orElseThrow();

        assertEquals("Ana Lopez", u.nombre());
        assertEquals("", u.apellido());
    }

    @Test
    void correoNoVerificadoPorGoogleSeRechaza() throws Exception {
        when(verifier.verify("t")).thenReturn(token(false, "Ana", "Lopez", null));

        assertTrue(adapter.verificar("t").isEmpty());
    }

    @Test
    void correoVerificadoNuloSeRechaza() throws Exception {
        when(verifier.verify("t")).thenReturn(token(null, "Ana", "Lopez", null));

        assertTrue(adapter.verificar("t").isEmpty());
    }

    @Test
    void tokenQueGoogleNoValidaDevuelveVacio() throws Exception {
        when(verifier.verify("t")).thenReturn(null);

        assertEquals(Optional.empty(), adapter.verificar("t"));
    }

    @Test
    void erroresDeFirmaOEntradaSalidaSeTraducenAVacio() throws Exception {
        when(verifier.verify("firma")).thenThrow(new GeneralSecurityException("firma"));
        when(verifier.verify("io")).thenThrow(new IOException("red"));
        when(verifier.verify("arg")).thenThrow(new IllegalArgumentException("arg"));

        assertTrue(adapter.verificar("firma").isEmpty());
        assertTrue(adapter.verificar("io").isEmpty());
        assertTrue(adapter.verificar("arg").isEmpty());
    }

    @Test
    void textoQueNoEsJwtNuncaEsAceptadoPorElVerificadorReal() {
        GoogleTokenVerifierAdapter real = new GoogleTokenVerifierAdapter("cliente");

        assertTrue(real.verificar("esto-no-es-un-jwt").isEmpty());
    }
}
