package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarMetricasUseCase;
import com.appointech.appointech_backend.infrastructure.dto.MetricasResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Métricas generales del administrador. */
@RestController
@RequestMapping("/api/admin/metricas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminMetricasController {

    private final ConsultarMetricasUseCase consultarMetricas;

    public AdminMetricasController(ConsultarMetricasUseCase consultarMetricas) {
        this.consultarMetricas = consultarMetricas;
    }

    /** Sin parámetros: los últimos 30 días hasta hoy. */
    @GetMapping
    public ResponseEntity<MetricasResponse> consultar(@RequestParam(required = false) String desde,
                                                      @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(MetricasMapper.aRespuesta(consultarMetricas.consultar(MetricasMapper.aCriterios(desde, hasta))));
    }
}
