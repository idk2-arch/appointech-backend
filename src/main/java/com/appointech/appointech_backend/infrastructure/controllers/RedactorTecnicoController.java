package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.RedaccionTecnica;
import com.appointech.appointech_backend.domain.ports.in.RedactarDescripcionTecnicaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.RedactarTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.RedactarTecnicoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reescribe en lenguaje técnico lo que escribió el cliente; no usa más datos que el texto del problema. */
@RestController
@RequestMapping("/api/solicitudes/redactar-tecnico")
@PreAuthorize("hasRole('CLIENTE')")
public class RedactorTecnicoController {

    private final RedactarDescripcionTecnicaUseCase redactarDescripcion;

    public RedactorTecnicoController(RedactarDescripcionTecnicaUseCase redactarDescripcion) {
        this.redactarDescripcion = redactarDescripcion;
    }

    @PostMapping
    public ResponseEntity<RedactarTecnicoResponse> redactar(@Valid @RequestBody RedactarTecnicoRequest request) {
        RedaccionTecnica resultado = redactarDescripcion.redactar(request.descripcionProblema(), request.especialidad());
        return ResponseEntity.ok(new RedactarTecnicoResponse(resultado.disponible(), resultado.textoTecnico()));
    }
}
