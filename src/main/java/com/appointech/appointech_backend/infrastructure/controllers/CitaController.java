package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.ConfirmarCitaRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/citas")
@PreAuthorize("hasRole('CLIENTE')")
public class CitaController {

    private final ConfirmarCitaUseCase confirmarCitaUseCase;

    public CitaController(ConfirmarCitaUseCase confirmarCitaUseCase) {
        this.confirmarCitaUseCase = confirmarCitaUseCase;
    }

    @PostMapping("/confirmar")
    public ResponseEntity<CitaResponse> confirmar(@Valid @RequestBody ConfirmarCitaRequest request,
                                                  Authentication authentication) {
        Cita cita = confirmarCitaUseCase.confirmar(
                authentication.getName(),
                request.solicitudId(),
                request.tecnicoId(),
                request.inicio(),
                request.fin()
        );
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(cita));
    }
}
