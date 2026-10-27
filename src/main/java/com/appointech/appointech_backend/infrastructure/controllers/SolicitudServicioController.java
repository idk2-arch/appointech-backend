package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.in.CrearSolicitudServicioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.CrearSolicitudRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
@PreAuthorize("hasRole('CLIENTE')")
public class SolicitudServicioController {

    private final CrearSolicitudServicioUseCase crearSolicitudServicioUseCase;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase;

    public SolicitudServicioController(CrearSolicitudServicioUseCase crearSolicitudServicioUseCase,
                                       ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase) {
        this.crearSolicitudServicioUseCase = crearSolicitudServicioUseCase;
        this.obtenerDetalleCitaUseCase = obtenerDetalleCitaUseCase;
    }

    @PostMapping
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CrearSolicitudRequest request,
                                              Authentication authentication) {
        Cita solicitud = crearSolicitudServicioUseCase.crear(
                authentication.getName(),
                request.especialidadId(),
                request.descripcionProblema(),
                request.direccionServicio(),
                request.latitud(),
                request.longitud()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CitaResponseMapper.aRespuesta(obtenerDetalleCitaUseCase.detallar(solicitud)));
    }
}
