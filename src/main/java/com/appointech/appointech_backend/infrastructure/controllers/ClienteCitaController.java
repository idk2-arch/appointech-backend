package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.in.CancelarCitaClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasDelClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ReprogramarCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CancelarCitaRequest;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.ReprogramarCitaRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta, cancelación y reprogramación de las citas del cliente autenticado. */
@RestController
@RequestMapping("/api/cliente/citas")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteCitaController {

    private final ConsultarCitasDelClienteUseCase consultarCitas;
    private final CancelarCitaClienteUseCase cancelarCita;
    private final ReprogramarCitaUseCase reprogramarCita;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCita;

    public ClienteCitaController(ConsultarCitasDelClienteUseCase consultarCitas,
                                 CancelarCitaClienteUseCase cancelarCita,
                                 ReprogramarCitaUseCase reprogramarCita,
                                 ObtenerDetalleCitaUseCase obtenerDetalleCita) {
        this.consultarCitas = consultarCitas;
        this.cancelarCita = cancelarCita;
        this.reprogramarCita = reprogramarCita;
        this.obtenerDetalleCita = obtenerDetalleCita;
    }

    @GetMapping
    public ResponseEntity<List<CitaResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(consultarCitas.listar(authentication.getName()).stream()
                .map(CitaResponseMapper::aRespuesta)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaDetalleResponse> detallar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(CitaDetalleMapper.aRespuesta(consultarCitas.detallar(authentication.getName(), id)));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CitaResponse> cancelar(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) CancelarCitaRequest request,
                                                 Authentication authentication) {
        String motivo = request == null ? null : request.motivo();
        Cita cita = cancelarCita.cancelar(authentication.getName(), id, motivo);
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(obtenerDetalleCita.detallar(cita)));
    }

    @PostMapping("/{id}/reprogramar")
    public ResponseEntity<CitaResponse> reprogramar(@PathVariable Long id,
                                                    @Valid @RequestBody ReprogramarCitaRequest request,
                                                    Authentication authentication) {
        Cita cita = reprogramarCita.reprogramar(authentication.getName(), id, request.inicio());
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(obtenerDetalleCita.detallar(cita)));
    }
}
