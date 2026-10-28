package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.in.ActualizarEstadoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.CancelarCitaTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarDiagnosticoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarEstadoCitaRequest;
import com.appointech.appointech_backend.infrastructure.dto.CancelarCitaTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.DiagnosticoRequest;
import com.appointech.appointech_backend.infrastructure.dto.DiagnosticoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Acciones del técnico sobre sus citas: avanzar estado, cancelar y registrar el diagnóstico. */
@RestController
@RequestMapping("/api/tecnico/citas")
@PreAuthorize("hasRole('TECNICO')")
public class TecnicoGestionCitaController {

    private final ActualizarEstadoCitaUseCase actualizarEstado;
    private final CancelarCitaTecnicoUseCase cancelarCita;
    private final RegistrarDiagnosticoUseCase registrarDiagnostico;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCita;

    public TecnicoGestionCitaController(ActualizarEstadoCitaUseCase actualizarEstado,
                                        CancelarCitaTecnicoUseCase cancelarCita,
                                        RegistrarDiagnosticoUseCase registrarDiagnostico,
                                        ObtenerDetalleCitaUseCase obtenerDetalleCita) {
        this.actualizarEstado = actualizarEstado;
        this.cancelarCita = cancelarCita;
        this.registrarDiagnostico = registrarDiagnostico;
        this.obtenerDetalleCita = obtenerDetalleCita;
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CitaResponse> actualizarEstado(@PathVariable Long id,
                                                         @Valid @RequestBody ActualizarEstadoCitaRequest request,
                                                         Authentication authentication) {
        Cita cita = actualizarEstado.actualizar(authentication.getName(), id, request.estado());
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(obtenerDetalleCita.detallar(cita)));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CitaResponse> cancelar(@PathVariable Long id,
                                                 @Valid @RequestBody CancelarCitaTecnicoRequest request,
                                                 Authentication authentication) {
        Cita cita = cancelarCita.cancelar(authentication.getName(), id, request.motivo());
        return ResponseEntity.ok(CitaResponseMapper.aRespuesta(obtenerDetalleCita.detallar(cita)));
    }

    /** POST crea el diagnóstico y también lo corrige si ya existe; PUT hace lo mismo. */
    @PostMapping("/{id}/diagnostico")
    public ResponseEntity<DiagnosticoResponse> registrarDiagnosticoPost(@PathVariable Long id,
                                                                       @Valid @RequestBody DiagnosticoRequest request,
                                                                       Authentication authentication) {
        return guardarDiagnostico(id, request, authentication);
    }

    @PutMapping("/{id}/diagnostico")
    public ResponseEntity<DiagnosticoResponse> registrarDiagnosticoPut(@PathVariable Long id,
                                                                      @Valid @RequestBody DiagnosticoRequest request,
                                                                      Authentication authentication) {
        return guardarDiagnostico(id, request, authentication);
    }

    private ResponseEntity<DiagnosticoResponse> guardarDiagnostico(Long id, DiagnosticoRequest request,
                                                                   Authentication authentication) {
        Cita cita = registrarDiagnostico.registrar(authentication.getName(), id,
                request.diagnostico(), request.recomendaciones());
        return ResponseEntity.ok(CitaDetalleMapper.aDiagnostico(cita));
    }
}
