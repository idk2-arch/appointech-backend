package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarDetalleCitaAdminUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/citas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminCitaController {

    private final ConsultarDetalleCitaAdminUseCase consultarDetalle;

    public AdminCitaController(ConsultarDetalleCitaAdminUseCase consultarDetalle) {
        this.consultarDetalle = consultarDetalle;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaDetalleResponse> detallar(@PathVariable Long id) {
        return ResponseEntity.ok(CitaDetalleMapper.aRespuesta(consultarDetalle.detallar(id)));
    }
}
