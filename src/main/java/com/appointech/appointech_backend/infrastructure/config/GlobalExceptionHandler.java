package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNombreDuplicadoException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Normaliza todos los errores de la API con el formato RFC 7807 (Problem Details),
 * usando la clase ProblemDetail que ya incluye Spring Framework.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ProblemDetail manejarCorreoYaRegistrado(CorreoYaRegistradoException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail manejarCredencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(TecnicoNoEncontradoException.class)
    public ProblemDetail manejarTecnicoNoEncontrado(TecnicoNoEncontradoException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ProblemDetail manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EspecialidadNoEncontradaException.class)
    public ProblemDetail manejarEspecialidadNoEncontrada(EspecialidadNoEncontradaException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(EspecialidadNombreDuplicadoException.class)
    public ProblemDetail manejarEspecialidadNombreDuplicado(EspecialidadNombreDuplicadoException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return construirProblemDetail(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");

        return construirProblemDetail(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    private ProblemDetail construirProblemDetail(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}