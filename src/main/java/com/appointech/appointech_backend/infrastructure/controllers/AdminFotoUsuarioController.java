package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ActualizarFotoUsuarioUseCase;
import com.appointech.appointech_backend.infrastructure.dto.FotoPerfilRequest;
import com.appointech.appointech_backend.infrastructure.dto.FotoPerfilResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminFotoUsuarioController {

    private final ActualizarFotoUsuarioUseCase actualizarFotoUsuario;

    public AdminFotoUsuarioController(ActualizarFotoUsuarioUseCase actualizarFotoUsuario) {
        this.actualizarFotoUsuario = actualizarFotoUsuario;
    }

    @PutMapping("/{id}/foto")
    public ResponseEntity<FotoPerfilResponse> reemplazar(@PathVariable Long id,
                                                         @Valid @RequestBody FotoPerfilRequest request) {
        String clave = actualizarFotoUsuario.reemplazar(id, request.imagenBase64());
        return ResponseEntity.ok(new FotoPerfilResponse(FotoUrlMapper.aUrl(clave)));
    }
}
