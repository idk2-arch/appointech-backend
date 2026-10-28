package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.in.ObtenerFotoCitaUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Sirve la foto de una cita. Exige sesión; la pertenencia (cliente dueño, técnico asignado o administrador) la
 * comprueba el caso de uso, no el rol.
 */
@RestController
@RequestMapping("/api/citas/{citaId}/fotos")
public class CitaFotoController {

    /** private: la respuesta es para este usuario y no debe guardarse en cachés compartidas. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePrivate();

    private final ObtenerFotoCitaUseCase obtenerFotoCita;

    public CitaFotoController(ObtenerFotoCitaUseCase obtenerFotoCita) {
        this.obtenerFotoCita = obtenerFotoCita;
    }

    @GetMapping("/{fotoId}")
    public ResponseEntity<byte[]> obtener(@PathVariable Long citaId, @PathVariable Long fotoId,
                                          Authentication authentication) {
        FotoAlmacenada foto = obtenerFotoCita.obtener(authentication.getName(), citaId, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CACHE)
                .body(foto.contenido());
    }
}
