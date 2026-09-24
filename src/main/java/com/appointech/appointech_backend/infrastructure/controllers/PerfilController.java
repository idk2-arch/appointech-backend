package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.CompletarPerfilUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CompletarPerfilRequest;
import com.appointech.appointech_backend.infrastructure.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PerfilController {

    private final CompletarPerfilUseCase completarPerfilUseCase;

    public PerfilController(CompletarPerfilUseCase completarPerfilUseCase) {
        this.completarPerfilUseCase = completarPerfilUseCase;
    }

    @PatchMapping("/completar-perfil")
    public ResponseEntity<UsuarioResponse> completarPerfil(@Valid @RequestBody CompletarPerfilRequest request,
                                                           Authentication authentication) {
        Usuario usuario = completarPerfilUseCase.completar(
                authentication.getName(),
                request.telefono(),
                request.tipoDocumento(),
                request.numeroDocumento(),
                request.direccion()
        );

        UsuarioResponse response = new UsuarioResponse(
                usuario.getId(), usuario.getNombre(), usuario.getApellido(),
                usuario.getCorreo(), usuario.getRol().name()
        );

        return ResponseEntity.ok(response);
    }
}