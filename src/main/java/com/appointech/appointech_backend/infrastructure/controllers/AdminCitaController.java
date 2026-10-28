package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasAdminUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarDetalleCitaAdminUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitasAdminPaginaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/citas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminCitaController {

    private final ConsultarDetalleCitaAdminUseCase consultarDetalle;
    private final ConsultarCitasAdminUseCase consultarCitas;

    public AdminCitaController(ConsultarDetalleCitaAdminUseCase consultarDetalle,
                               ConsultarCitasAdminUseCase consultarCitas) {
        this.consultarDetalle = consultarDetalle;
        this.consultarCitas = consultarCitas;
    }

    @GetMapping
    public ResponseEntity<CitasAdminPaginaResponse> listar(
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ResponseEntity.ok(CitaAdminMapper.aRespuesta(consultarCitas.consultar(
                CitaAdminMapper.aFiltro(tecnicoId, estado, fechaDesde, fechaHasta, pagina, tamanio))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaDetalleResponse> detallar(@PathVariable Long id) {
        return ResponseEntity.ok(CitaDetalleMapper.aRespuesta(consultarDetalle.detallar(id)));
    }
}
