package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.ports.in.AgregarFotoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.EliminarFotoCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.FotoCitaRequest;
import com.appointech.appointech_backend.infrastructure.dto.FotoCitaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fotos del electrodoméstico: el cliente las adjunta y las quita de su propia cita. */
@RestController
@RequestMapping("/api/cliente/citas/{citaId}/fotos")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteCitaFotoController {

    private final AgregarFotoCitaUseCase agregarFoto;
    private final EliminarFotoCitaUseCase eliminarFoto;

    public ClienteCitaFotoController(AgregarFotoCitaUseCase agregarFoto, EliminarFotoCitaUseCase eliminarFoto) {
        this.agregarFoto = agregarFoto;
        this.eliminarFoto = eliminarFoto;
    }

    @PostMapping
    public ResponseEntity<FotoCitaResponse> agregar(@PathVariable Long citaId,
                                                    @Valid @RequestBody FotoCitaRequest request,
                                                    Authentication authentication) {
        CitaFoto foto = agregarFoto.agregar(authentication.getName(), citaId, request.imagenBase64());
        return ResponseEntity.status(HttpStatus.CREATED).body(FotoUrlMapper.aFotoCita(foto));
    }

    @DeleteMapping("/{fotoId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long citaId, @PathVariable Long fotoId,
                                         Authentication authentication) {
        eliminarFoto.eliminar(authentication.getName(), citaId, fotoId);
        return ResponseEntity.noContent().build();
    }
}
