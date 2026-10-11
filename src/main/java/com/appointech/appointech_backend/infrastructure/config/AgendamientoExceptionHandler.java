package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class AgendamientoExceptionHandler {

    private final RegistroLogPort registroLogPort;

    public AgendamientoExceptionHandler(RegistroLogPort registroLogPort) {
        this.registroLogPort = registroLogPort;
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarSolicitudNoEncontrada(SolicitudNoEncontradaException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FranjaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarFranjaNoDisponible(FranjaNoDisponibleException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(FranjaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> manejarFranjaInvalida(FranjaInvalidaException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String usuario = autenticacion != null ? autenticacion.getName() : null;
        NivelLog nivel = status == HttpStatus.CONFLICT ? NivelLog.WARN : NivelLog.INFO;
        registroLogPort.registrar(nivel, TipoLog.NEGOCIO, "AgendamientoExceptionHandler", usuario,
                status.value() + " - " + mensaje);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
