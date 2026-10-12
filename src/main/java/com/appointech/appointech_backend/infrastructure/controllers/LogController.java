package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.ports.in.ConsultarLogsUseCase;
import com.appointech.appointech_backend.infrastructure.dto.LogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class LogController {

    private final ConsultarLogsUseCase consultarLogsUseCase;

    public LogController(ConsultarLogsUseCase consultarLogsUseCase) {
        this.consultarLogsUseCase = consultarLogsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<LogResponse>> consultar(@RequestParam(required = false) String nivel,
                                                       @RequestParam(required = false) String tipo,
                                                       @RequestParam(defaultValue = "100") int limite) {
        List<LogResponse> respuesta = consultarLogsUseCase.consultar(nivel, tipo, limite).stream()
                .map(log -> new LogResponse(log.getId(), log.getNivel(), log.getTipo(), log.getOrigen(),
                        log.getUsuario(), log.getMensaje(), log.getCreadoEn()))
                .toList();
        return ResponseEntity.ok(respuesta);
    }
}
