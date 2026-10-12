package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.infrastructure.dto.FranjaDisponibleResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoDisponibleResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase;
    private final CalcularFranjasDisponiblesUseCase calcularFranjasDisponiblesUseCase;

    public DisponibilidadController(FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicosPorEspecialidadUseCase,
                                    CalcularFranjasDisponiblesUseCase calcularFranjasDisponiblesUseCase) {
        this.filtrarTecnicosPorEspecialidadUseCase = filtrarTecnicosPorEspecialidadUseCase;
        this.calcularFranjasDisponiblesUseCase = calcularFranjasDisponiblesUseCase;
    }

    @GetMapping("/tecnicos")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<List<TecnicoDisponibleResponse>> tecnicosPorEspecialidad(@RequestParam Long especialidadId) {
        List<TecnicoDisponibleResponse> response = filtrarTecnicosPorEspecialidadUseCase.filtrar(especialidadId)
                .stream()
                .map(t -> new TecnicoDisponibleResponse(
                        t.getId(),
                        t.getUsuario().getNombre() + " " + t.getUsuario().getApellido(),
                        t.getRadioCoberturaKm()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/franjas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<FranjaDisponibleResponse>> franjas(
            @RequestParam Long especialidadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Double latitud,
            @RequestParam Double longitud) {
        List<FranjaDisponibleResponse> response = calcularFranjasDisponiblesUseCase
                .calcular(especialidadId, fecha, latitud, longitud)
                .stream()
                .map(f -> new FranjaDisponibleResponse(f.getTecnicoId(), f.getInicio(), f.getFin(),
                        Math.round(f.getDistanciaKm() * 100.0) / 100.0))
                .toList();
        return ResponseEntity.ok(response);
    }
}
