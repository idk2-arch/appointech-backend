package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasDelTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasFinalizadasDelTecnicoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CitaFinalizadaTecnicoResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaTecnicoResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tecnico/citas")
@PreAuthorize("hasRole('TECNICO')")
public class TecnicoCitaController {

    private final ConsultarCitasDelTecnicoUseCase consultarCitasDelTecnicoUseCase;

    private final ConsultarCitasFinalizadasDelTecnicoUseCase consultarFinalizadas;

    public TecnicoCitaController(ConsultarCitasDelTecnicoUseCase consultarCitasDelTecnicoUseCase,
                                 ConsultarCitasFinalizadasDelTecnicoUseCase consultarFinalizadas) {
        this.consultarCitasDelTecnicoUseCase = consultarCitasDelTecnicoUseCase;
        this.consultarFinalizadas = consultarFinalizadas;
    }

    /** Últimas 50 citas finalizadas del técnico, las más recientes primero, con la situación de su cobro. */
    @GetMapping("/finalizadas")
    public ResponseEntity<List<CitaFinalizadaTecnicoResponse>> finalizadas(Authentication authentication) {
        return ResponseEntity.ok(consultarFinalizadas.consultar(authentication.getName()).stream()
                .map(TecnicoAgendaMappers::aRespuesta)
                .toList());
    }

    @GetMapping
    public ResponseEntity<List<CitaTecnicoResponse>> citasDelDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {
        List<CitaTecnicoResponse> respuesta = consultarCitasDelTecnicoUseCase
                .consultar(authentication.getName(), fecha)
                .stream()
                .map(TecnicoAgendaMappers::aRespuesta)
                .toList();
        return ResponseEntity.ok(respuesta);
    }
}
