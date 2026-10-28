package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ActualizarFotoPerfilUseCase;
import com.appointech.appointech_backend.infrastructure.dto.FotoPerfilRequest;
import com.appointech.appointech_backend.infrastructure.dto.FotoPerfilResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Foto de perfil propia: cualquier usuario autenticado, sea cual sea su rol. */
@RestController
@RequestMapping("/api/perfil/foto")
public class FotoPerfilController {

    private final ActualizarFotoPerfilUseCase actualizarFotoPerfil;

    public FotoPerfilController(ActualizarFotoPerfilUseCase actualizarFotoPerfil) {
        this.actualizarFotoPerfil = actualizarFotoPerfil;
    }

    @PutMapping
    public ResponseEntity<FotoPerfilResponse> reemplazar(@Valid @RequestBody FotoPerfilRequest request,
                                                         Authentication authentication) {
        String clave = actualizarFotoPerfil.reemplazar(authentication.getName(), request.imagenBase64());
        return ResponseEntity.ok(new FotoPerfilResponse(FotoUrlMapper.aUrl(clave)));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(Authentication authentication) {
        actualizarFotoPerfil.eliminar(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
