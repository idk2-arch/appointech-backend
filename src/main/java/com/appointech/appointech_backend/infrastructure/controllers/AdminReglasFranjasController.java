package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.ReglasFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoActualizacionReglas;
import com.appointech.appointech_backend.domain.ports.in.ActualizarReglasFranjasUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerReglasFranjasUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ReglasFranjasRequest;
import com.appointech.appointech_backend.infrastructure.dto.ReglasFranjasResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/configuracion/franjas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReglasFranjasController {

    static final String ADVERTENCIA_FRANJAS_INVALIDADAS =
            "Cambió la duración o la jornada: las franjas que los clientes ya tenían en pantalla no se podrán "
                    + "confirmar y deberán consultarse de nuevo. Las citas ya confirmadas no se modifican.";

    private final ObtenerReglasFranjasUseCase obtenerReglasFranjasUseCase;
    private final ActualizarReglasFranjasUseCase actualizarReglasFranjasUseCase;

    public AdminReglasFranjasController(ObtenerReglasFranjasUseCase obtenerReglasFranjasUseCase,
                                        ActualizarReglasFranjasUseCase actualizarReglasFranjasUseCase) {
        this.obtenerReglasFranjasUseCase = obtenerReglasFranjasUseCase;
        this.actualizarReglasFranjasUseCase = actualizarReglasFranjasUseCase;
    }

    @GetMapping
    public ResponseEntity<ReglasFranjasResponse> obtener() {
        return ResponseEntity.ok(aRespuesta(obtenerReglasFranjasUseCase.obtener(), false));
    }

    @PutMapping
    public ResponseEntity<ReglasFranjasResponse> actualizar(@Valid @RequestBody ReglasFranjasRequest request,
                                                            Authentication authentication) {
        ResultadoActualizacionReglas resultado = actualizarReglasFranjasUseCase.actualizar(
                authentication.getName(), request.radioMaximoKm(), request.duracionBloqueMinutos(),
                request.horaInicioJornada(), request.horaFinJornada());
        return ResponseEntity.ok(aRespuesta(resultado.reglas(), resultado.franjasOfrecidasInvalidadas()));
    }

    private static ReglasFranjasResponse aRespuesta(ReglasFranjas reglas, boolean invalidadas) {
        return new ReglasFranjasResponse(reglas.radioMaximoKm(), reglas.duracionBloqueMinutos(),
                reglas.horaInicioJornada(), reglas.horaFinJornada(), invalidadas,
                invalidadas ? ADVERTENCIA_FRANJAS_INVALIDADAS : null);
    }
}
