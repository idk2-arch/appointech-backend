package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.models.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.models.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNombreDuplicadoException;
import com.appointech.appointech_backend.domain.models.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.models.exception.CitaNoCancelableException;
import com.appointech.appointech_backend.domain.models.exception.CitaNoEncontradaException;
import com.appointech.appointech_backend.domain.models.exception.CitaNoPerteneceAlTecnicoException;
import com.appointech.appointech_backend.domain.models.exception.DatosCitaInvalidosException;
import com.appointech.appointech_backend.domain.models.exception.DiagnosticoNoPermitidoException;
import com.appointech.appointech_backend.domain.models.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.models.exception.NotificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.models.exception.TransicionEstadoNoPermitidaException;
import com.appointech.appointech_backend.domain.models.exception.ComprobanteNoDisponibleException;
import com.appointech.appointech_backend.domain.models.exception.CostoFinalNoPermitidoException;
import com.appointech.appointech_backend.domain.models.exception.CotizacionNoDisponibleException;
import com.appointech.appointech_backend.domain.models.exception.DatosPagoInvalidosException;
import com.appointech.appointech_backend.domain.models.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.exception.PagoNoPermitidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> manejarCorreoYaRegistrado(CorreoYaRegistradoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(TecnicoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarTecnicoNoEncontrado(TecnicoNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EspecialidadNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarEspecialidadNoEncontrada(EspecialidadNoEncontradaException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EspecialidadNombreDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> manejarEspecialidadNombreDuplicado(EspecialidadNombreDuplicadoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccesoCitaDenegadoException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoCitaDenegado(AccesoCitaDenegadoException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CitaNoPerteneceAlTecnicoException.class)
    public ResponseEntity<Map<String, Object>> manejarCitaNoPerteneceAlTecnico(CitaNoPerteneceAlTecnicoException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CitaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarCitaNoEncontrada(CitaNoEncontradaException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NotificacionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarNotificacionNoEncontrada(NotificacionNoEncontradaException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransicionEstadoNoPermitidaException.class)
    public ResponseEntity<Map<String, Object>> manejarTransicionEstadoNoPermitida(TransicionEstadoNoPermitidaException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CitaNoCancelableException.class)
    public ResponseEntity<Map<String, Object>> manejarCitaNoCancelable(CitaNoCancelableException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DiagnosticoNoPermitidoException.class)
    public ResponseEntity<Map<String, Object>> manejarDiagnosticoNoPermitido(DiagnosticoNoPermitidoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(FranjaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarFranjaNoDisponible(FranjaNoDisponibleException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DatosCitaInvalidosException.class)
    public ResponseEntity<Map<String, Object>> manejarDatosCitaInvalidos(DatosCitaInvalidosException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CotizacionNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarCotizacionNoDisponible(CotizacionNoDisponibleException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CostoFinalNoPermitidoException.class)
    public ResponseEntity<Map<String, Object>> manejarCostoFinalNoPermitido(CostoFinalNoPermitidoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PagoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarPagoNoEncontrado(PagoNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PagoNoPermitidoException.class)
    public ResponseEntity<Map<String, Object>> manejarPagoNoPermitido(PagoNoPermitidoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ComprobanteNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarComprobanteNoDisponible(ComprobanteNoDisponibleException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DatosPagoInvalidosException.class)
    public ResponseEntity<Map<String, Object>> manejarDatosPagoInvalidos(DatosPagoInvalidosException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido (revisa estados y fechas)");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccessDeniedException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");

        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", mensaje);

        return ResponseEntity.status(status).body(body);
    }
}