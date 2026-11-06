package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarCalificacionesTecnicoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionesTecnicoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** El técnico ve las calificaciones que recibió y su promedio. */
@RestController
@RequestMapping("/api/tecnico/calificaciones")
@PreAuthorize("hasRole('TECNICO')")
public class TecnicoCalificacionController {

    private final ConsultarCalificacionesTecnicoUseCase consultarCalificaciones;

    public TecnicoCalificacionController(ConsultarCalificacionesTecnicoUseCase consultarCalificaciones) {
        this.consultarCalificaciones = consultarCalificaciones;
    }

    @GetMapping
    public ResponseEntity<CalificacionesTecnicoResponse> listar(@RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer size,
                                                                Authentication authentication) {
        return ResponseEntity.ok(CalificacionMapper.aRespuesta(
                consultarCalificaciones.consultar(authentication.getName(), page, size)));
    }
}
