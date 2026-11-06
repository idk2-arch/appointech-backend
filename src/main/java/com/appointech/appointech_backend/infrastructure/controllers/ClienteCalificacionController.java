package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.CalificarServicioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCalificacionUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionResponse;
import com.appointech.appointech_backend.infrastructure.dto.CalificarServicioRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El cliente califica una de sus citas finalizadas y consulta la calificación que dio. */
@RestController
@RequestMapping("/api/cliente/citas/{id}/calificacion")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteCalificacionController {

    private final CalificarServicioUseCase calificarServicio;
    private final ConsultarCalificacionUseCase consultarCalificacion;

    public ClienteCalificacionController(CalificarServicioUseCase calificarServicio,
                                         ConsultarCalificacionUseCase consultarCalificacion) {
        this.calificarServicio = calificarServicio;
        this.consultarCalificacion = consultarCalificacion;
    }

    @PostMapping
    public ResponseEntity<CalificacionResponse> calificar(@PathVariable Long id,
                                                          @RequestBody CalificarServicioRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CalificacionMapper.aRespuesta(
                calificarServicio.calificar(authentication.getName(), id, request.puntuacion(), request.comentario())));
    }

    @GetMapping
    public ResponseEntity<CalificacionResponse> consultar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(CalificacionMapper.aRespuesta(consultarCalificacion.consultar(authentication.getName(), id)));
    }
}
