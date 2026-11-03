package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.PreDiagnostico;
import com.appointech.appointech_backend.domain.ports.in.GenerarPreDiagnosticoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.PreDiagnosticoRequest;
import com.appointech.appointech_backend.infrastructure.dto.PreDiagnosticoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Orientación con IA antes de crear la solicitud; no recibe ni usa datos del cliente más que el texto del problema. */
@RestController
@RequestMapping("/api/solicitudes/pre-diagnostico")
@PreAuthorize("hasRole('CLIENTE')")
public class PreDiagnosticoController {

    private final GenerarPreDiagnosticoUseCase generarPreDiagnostico;

    public PreDiagnosticoController(GenerarPreDiagnosticoUseCase generarPreDiagnostico) {
        this.generarPreDiagnostico = generarPreDiagnostico;
    }

    @PostMapping
    public ResponseEntity<PreDiagnosticoResponse> generar(@Valid @RequestBody PreDiagnosticoRequest request) {
        PreDiagnostico resultado = generarPreDiagnostico.generar(request.descripcionProblema(), request.especialidad());
        return ResponseEntity.ok(new PreDiagnosticoResponse(resultado.disponible(), resultado.sugerencia()));
    }
}
