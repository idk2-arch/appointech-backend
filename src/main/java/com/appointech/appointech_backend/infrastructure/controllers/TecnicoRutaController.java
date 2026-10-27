package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.CalcularRutaDelDiaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.RutaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tecnico/ruta")
@PreAuthorize("hasRole('TECNICO')")
public class TecnicoRutaController {

    private final CalcularRutaDelDiaUseCase calcularRutaDelDiaUseCase;

    public TecnicoRutaController(CalcularRutaDelDiaUseCase calcularRutaDelDiaUseCase) {
        this.calcularRutaDelDiaUseCase = calcularRutaDelDiaUseCase;
    }

    @GetMapping
    public ResponseEntity<RutaResponse> rutaDelDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {
        return ResponseEntity.ok(TecnicoAgendaMappers.aRespuesta(
                calcularRutaDelDiaUseCase.calcular(authentication.getName(), fecha)));
    }
}
