package com.appointech.notificaciones;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtFiltro extends OncePerRequestFilter {

    private final SecretKey clave;

    public JwtFiltro(@Value("${jwt.secret}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String encabezado = peticion.getHeader("Authorization");

        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            try {
                Claims reclamos = Jwts.parser()
                        .verifyWith(clave)
                        .build()
                        .parseSignedClaims(encabezado.substring(7))
                        .getPayload();

                var autenticacion = new UsernamePasswordAuthenticationToken(
                        reclamos.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + reclamos.get("rol", String.class)))
                );
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        cadena.doFilter(peticion, respuesta);
    }
}
