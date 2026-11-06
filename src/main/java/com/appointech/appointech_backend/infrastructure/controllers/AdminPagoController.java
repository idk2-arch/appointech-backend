package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.domain.ports.in.ConsultarHistorialPagosUseCase;
import com.appointech.appointech_backend.infrastructure.dto.PagoHistorialResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Historial de pagos del administrador. */
@RestController
@RequestMapping("/api/admin/pagos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminPagoController {

    static final String ENCABEZADO_TOTAL = "X-Total-Count";

    private final ConsultarHistorialPagosUseCase consultarHistorialPagos;

    public AdminPagoController(ConsultarHistorialPagosUseCase consultarHistorialPagos) {
        this.consultarHistorialPagos = consultarHistorialPagos;
    }

    /** El total de pagos que cumplen el filtro va en {@code X-Total-Count}; el cuerpo es solo la página (por defecto, la primera de 20). */
    @GetMapping
    public ResponseEntity<List<PagoHistorialResponse>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        ListadoConTotal<PagoDelHistorial> listado = consultarHistorialPagos.consultar(
                HistorialPagosMapper.aFiltro(estado, metodo, clienteId, tecnicoId, desde, hasta, page, size));
        return ResponseEntity.ok()
                .header(ENCABEZADO_TOTAL, String.valueOf(listado.total()))
                .body(listado.elementos().stream().map(HistorialPagosMapper::aRespuesta).toList());
    }
}
