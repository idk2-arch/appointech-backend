package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.CoberturaConfigurada;
import com.appointech.appointech_backend.domain.ports.in.ActualizarCoberturaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerCoberturaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CoberturaRequest;
import com.appointech.appointech_backend.infrastructure.dto.CoberturaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/configuracion/cobertura")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminCoberturaController {

    private final ObtenerCoberturaUseCase obtenerCoberturaUseCase;
    private final ActualizarCoberturaUseCase actualizarCoberturaUseCase;

    public AdminCoberturaController(ObtenerCoberturaUseCase obtenerCoberturaUseCase,
                                    ActualizarCoberturaUseCase actualizarCoberturaUseCase) {
        this.obtenerCoberturaUseCase = obtenerCoberturaUseCase;
        this.actualizarCoberturaUseCase = actualizarCoberturaUseCase;
    }

    @GetMapping
    public ResponseEntity<CoberturaResponse> obtener() {
        return ResponseEntity.ok(aRespuesta(obtenerCoberturaUseCase.obtener()));
    }

    @PutMapping
    public ResponseEntity<CoberturaResponse> actualizar(@Valid @RequestBody CoberturaRequest request) {
        return ResponseEntity.ok(aRespuesta(actualizarCoberturaUseCase.actualizar(
                request.radioMaximoKm(), request.telefonoWhatsappAdmin())));
    }

    private static CoberturaResponse aRespuesta(CoberturaConfigurada cobertura) {
        return new CoberturaResponse(cobertura.radioMaximoKm(), cobertura.telefonoWhatsappAdmin());
    }
}
