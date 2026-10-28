package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenGeneratorAdapterTest {

    private static final String SECRETO = "secreto-de-prueba-con-al-menos-32-bytes-0123456789";
    private static final SecretKey CLAVE = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(8L);
        u.setCorreo("ana@mail.com");
        u.setRol(RolUsuario.ADMINISTRADOR);
        return u;
    }

    @Test
    void generaTokenFirmadoConSubjectIdRolYExpiracion() {
        String token = new JwtTokenGeneratorAdapter(SECRETO, 3_600_000).generarToken(usuario());

        Claims claims = Jwts.parser().verifyWith(CLAVE).build().parseSignedClaims(token).getPayload();
        assertEquals("ana@mail.com", claims.getSubject());
        assertEquals("ADMINISTRADOR", claims.get("rol", String.class));
        assertEquals(8, claims.get("id", Integer.class));
        assertEquals(3_600_000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime(), 1000);
    }

    @Test
    void tokenConExpiracionNegativaYaNaceExpirado() {
        String token = new JwtTokenGeneratorAdapter(SECRETO, -10_000).generarToken(usuario());

        assertThrows(ExpiredJwtException.class,
                () -> Jwts.parser().verifyWith(CLAVE).build().parseSignedClaims(token));
    }

    @Test
    void secretoDemasiadoCortoSeRechazaAlConstruir() {
        assertThrows(RuntimeException.class, () -> new JwtTokenGeneratorAdapter("corto", 1000));
    }
}
