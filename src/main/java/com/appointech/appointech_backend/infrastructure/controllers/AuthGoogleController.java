package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase.ResultadoLogin;
import com.appointech.appointech_backend.infrastructure.dto.AuthResponse;
import com.appointech.appointech_backend.infrastructure.dto.GoogleLoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Adaptador de entrada: recibe el ID token que Angular obtiene de Google.
 * POST /api/auth/google  { "idToken": "eyJ..." }
 *
 * Depende de la INTERFAZ del caso de uso (puerto de entrada), no de la implementación.
 * Responde con el MISMO formato (AuthResponse) que el login normal.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthGoogleController {

    private final LoginConGoogleUseCase loginConGoogle;

    public AuthGoogleController(LoginConGoogleUseCase loginConGoogle) {
        this.loginConGoogle = loginConGoogle;
    }

    @PostMapping("/google")
    public ResponseEntity<?> login(@RequestBody GoogleLoginRequest request) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Falta el token de Google"));
        }

        ResultadoLogin resultado = loginConGoogle.ejecutar(request.idToken());
        return ResponseEntity.ok(new AuthResponse(resultado.token(), resultado.perfilCompleto()));
    }
}