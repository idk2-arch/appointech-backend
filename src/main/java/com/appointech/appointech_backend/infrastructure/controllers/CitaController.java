package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.SolicitarAgendamientoManualUseCase;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualRequest;
import org.springframework.http.HttpStatus;
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
    private final ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase;
    private final SolicitarAgendamientoManualUseCase solicitarAgendamientoManualUseCase;

    public CitaController(ConfirmarCitaUseCase confirmarCitaUseCase,
                          ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase,
                          SolicitarAgendamientoManualUseCase solicitarAgendamientoManualUseCase) {
        this.confirmarCitaUseCase = confirmarCitaUseCase;
        this.obtenerDetalleCitaUseCase = obtenerDetalleCitaUseCase;
        this.solicitarAgendamientoManualUseCase = solicitarAgendamientoManualUseCase;
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
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(obtenerDetalleCitaUseCase.detallar(cita)));
    }

    @PostMapping("/solicitud-manual")
    public ResponseEntity<CitaResponse> solicitudManual(@Valid @RequestBody SolicitudManualRequest request,
                                                        Authentication authentication) {
        Cita solicitud = solicitarAgendamientoManualUseCase.solicitar(
                authentication.getName(),
                request.especialidadId(),
                request.descripcionProblema(),
                request.direccionServicio(),
                request.latitud(),
                request.longitud(),
                request.telefonoContacto()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CitaResponseMapper.aRespuesta(obtenerDetalleCitaUseCase.detallar(solicitud)));
    }
}
