package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase.ResultadoLogin;
import com.appointech.appointech_backend.infrastructure.dto.ApiResponse;
import com.appointech.appointech_backend.infrastructure.dto.AuthResponse;
import com.appointech.appointech_backend.infrastructure.dto.GoogleLoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthGoogleController {

    private final LoginConGoogleUseCase loginConGoogle;

    public AuthGoogleController(LoginConGoogleUseCase loginConGoogle) {
        this.loginConGoogle = loginConGoogle;
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody GoogleLoginRequest request) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Falta el token de Google"));
        }

        ResultadoLogin resultado = loginConGoogle.ejecutar(request.idToken());
        AuthResponse response = new AuthResponse(resultado.token(), resultado.perfilCompleto());
        return ResponseEntity.ok(ApiResponse.exito(response, "Inicio de sesión con Google exitoso"));
    }
}