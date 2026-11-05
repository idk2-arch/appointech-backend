package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionEspecialidadUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CotizacionResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Valor estimado del servicio: por especialidad (antes de pedir la cita) y por cita. */
@RestController
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Identificador inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso: el rol no corresponde o la cita no es suya"),
        @ApiResponse(responseCode = "404", description = "La especialidad o la cita no existen")
})
public class CotizacionController {

    private final ConsultarCotizacionEspecialidadUseCase cotizacionEspecialidad;
    private final ConsultarCotizacionCitaUseCase cotizacionCita;

    public CotizacionController(ConsultarCotizacionEspecialidadUseCase cotizacionEspecialidad,
                                ConsultarCotizacionCitaUseCase cotizacionCita) {
        this.cotizacionEspecialidad = cotizacionEspecialidad;
        this.cotizacionCita = cotizacionCita;
    }

    @GetMapping("/api/especialidades/{especialidadId}/cotizacion")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CotizacionResponse> cotizarEspecialidad(@PathVariable Long especialidadId) {
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(cotizacionEspecialidad.cotizar(especialidadId)));
    }

    /** La pertenencia (cliente dueño, técnico asignado o administrador) la decide el caso de uso. */
    @GetMapping("/api/citas/{citaId}/cotizacion")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<CotizacionResponse> cotizarCita(@PathVariable Long citaId, Authentication authentication) {
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(cotizacionCita.cotizar(authentication.getName(), citaId)));
    }
}
