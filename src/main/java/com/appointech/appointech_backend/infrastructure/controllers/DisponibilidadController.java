package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.infrastructure.dto.FranjaDisponibleResponse;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.infrastructure.dto.FranjasConMotivoResponse;
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
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud) {
        ResultadoFranjas resultado = calcularFranjasDisponiblesUseCase
                .calcularConMotivo(especialidadId, fecha, latitud, longitud);
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (resultado.motivo() != null) {
            // El cuerpo sigue siendo un arreglo; el motivo viaja en una cabecera.
            respuesta.header("X-Motivo-Sin-Franjas", resultado.motivo().name());
        }
        if (resultado.telefonoContactoAdmin() != null) {
            respuesta.header("X-Telefono-Contacto-Admin", resultado.telefonoContactoAdmin());
        }
        return respuesta.body(resultado.franjas().stream().map(FranjaResponseMapper::aRespuesta).toList());
    }

    @GetMapping("/franjas-con-motivo")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<FranjasConMotivoResponse> franjasConMotivo(
            @RequestParam Long especialidadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud) {
        ResultadoFranjas resultado = calcularFranjasDisponiblesUseCase
                .calcularConMotivo(especialidadId, fecha, latitud, longitud);
        return ResponseEntity.ok(new FranjasConMotivoResponse(
                resultado.franjas().stream().map(FranjaResponseMapper::aRespuesta).toList(),
                resultado.motivo() != null ? resultado.motivo().name() : null,
                resultado.telefonoContactoAdmin()));
    }
}
