package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.in.ObtenerFotoUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Sirve las fotos a través del backend (ruta pública): el bucket nunca se expone. */
@RestController
@RequestMapping("/api/fotos")
public class FotoController {

    /** Cada subida genera una clave nueva, así que una foto con una clave dada nunca cambia. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final ObtenerFotoUseCase obtenerFoto;

    public FotoController(ObtenerFotoUseCase obtenerFoto) {
        this.obtenerFoto = obtenerFoto;
    }

    @GetMapping("/{clave}")
    public ResponseEntity<byte[]> obtener(@PathVariable String clave) {
        FotoAlmacenada foto = obtenerFoto.obtener(clave);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CACHE)
                .body(foto.contenido());
    }
}
