package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.AdministrarFranjasTrabajoUseCase;
import com.appointech.appointech_backend.domain.ports.in.CambiarRadioAtencionUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarFranjasTrabajoUseCase;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.infrastructure.dto.FranjaTrabajoRequest;
import com.appointech.appointech_backend.infrastructure.dto.FranjaTrabajoResponse;
import com.appointech.appointech_backend.infrastructure.dto.RadioAtencionRequest;
import com.appointech.appointech_backend.infrastructure.dto.RadioAtencionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Franjas de trabajo y radio de atención de cada técnico: solo el administrador los define y los ve. */
@RestController
@RequestMapping("/api/admin/tecnicos/{tecnicoId}")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminFranjaTrabajoController {

    private final AdministrarFranjasTrabajoUseCase administrarFranjas;
    private final ConsultarFranjasTrabajoUseCase consultarFranjas;
    private final CambiarRadioAtencionUseCase cambiarRadio;

    public AdminFranjaTrabajoController(AdministrarFranjasTrabajoUseCase administrarFranjas,
                                        ConsultarFranjasTrabajoUseCase consultarFranjas,
                                        CambiarRadioAtencionUseCase cambiarRadio) {
        this.administrarFranjas = administrarFranjas;
        this.consultarFranjas = consultarFranjas;
        this.cambiarRadio = cambiarRadio;
    }

    @PostMapping("/franjas")
    public ResponseEntity<FranjaTrabajoResponse> crear(@PathVariable Long tecnicoId, @RequestBody FranjaTrabajoRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(FranjaTrabajoMapper.aRespuesta(administrarFranjas.crear(
                authentication.getName(), tecnicoId, FranjaTrabajoMapper.aFecha(request.fecha()),
                FranjaTrabajoMapper.aHora("horaInicio", request.horaInicio()),
                FranjaTrabajoMapper.aHora("horaFin", request.horaFin()))));
    }

    /** Sin fechas: desde hoy y los 30 días siguientes; el rango máximo es de 90 días. */
    @GetMapping("/franjas")
    public ResponseEntity<List<FranjaTrabajoResponse>> listar(@PathVariable Long tecnicoId,
                                                              @RequestParam(required = false) String desde,
                                                              @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(consultarFranjas.listar(tecnicoId,
                        ParametrosDeConsulta.aFecha("desde", desde), ParametrosDeConsulta.aFecha("hasta", hasta)).stream()
                .map(FranjaTrabajoMapper::aRespuesta).toList());
    }

    @PutMapping("/franjas/{franjaId}")
    public ResponseEntity<FranjaTrabajoResponse> actualizar(@PathVariable Long tecnicoId, @PathVariable Long franjaId,
                                                            @RequestBody FranjaTrabajoRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(FranjaTrabajoMapper.aRespuesta(administrarFranjas.actualizar(
                authentication.getName(), tecnicoId, franjaId, FranjaTrabajoMapper.aFecha(request.fecha()),
                FranjaTrabajoMapper.aHora("horaInicio", request.horaInicio()),
                FranjaTrabajoMapper.aHora("horaFin", request.horaFin()))));
    }

    @DeleteMapping("/franjas/{franjaId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long tecnicoId, @PathVariable Long franjaId,
                                         Authentication authentication) {
        administrarFranjas.eliminar(authentication.getName(), tecnicoId, franjaId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/radio-atencion")
    public ResponseEntity<RadioAtencionResponse> cambiarRadio(@PathVariable Long tecnicoId,
                                                              @RequestBody RadioAtencionRequest request,
                                                              Authentication authentication) {
        Tecnico tecnico = cambiarRadio.cambiar(authentication.getName(), tecnicoId, request.radioMaximoKm());
        return ResponseEntity.ok(new RadioAtencionResponse(tecnico.getId(), tecnico.getRadioCoberturaKm()));
    }
}
