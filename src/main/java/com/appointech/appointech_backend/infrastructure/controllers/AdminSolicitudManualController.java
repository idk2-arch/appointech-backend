package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.AsignarSolicitudManualRequest;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualAdminResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/solicitudes-manuales")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminSolicitudManualController {

    private final ListarSolicitudesManualesUseCase listarSolicitudesManualesUseCase;
    private final AsignarSolicitudManualUseCase asignarSolicitudManualUseCase;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase;

    public AdminSolicitudManualController(ListarSolicitudesManualesUseCase listarSolicitudesManualesUseCase,
                                          AsignarSolicitudManualUseCase asignarSolicitudManualUseCase,
                                          ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase) {
        this.listarSolicitudesManualesUseCase = listarSolicitudesManualesUseCase;
        this.asignarSolicitudManualUseCase = asignarSolicitudManualUseCase;
        this.obtenerDetalleCitaUseCase = obtenerDetalleCitaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudManualAdminResponse>> listar() {
        return ResponseEntity.ok(listarSolicitudesManualesUseCase.listar().stream()
                .map(SolicitudManualAdminResponseMapper::aRespuesta)
                .toList());
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<CitaResponse> asignar(@PathVariable Long id,
                                                @Valid @RequestBody AsignarSolicitudManualRequest request,
                                                Authentication authentication) {
        Cita cita = asignarSolicitudManualUseCase.asignar(authentication.getName(), id, request.tecnicoId(),
                request.fechaHoraInicio(), request.fechaHoraFin());
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(obtenerDetalleCitaUseCase.detallar(cita)));
    }
}
