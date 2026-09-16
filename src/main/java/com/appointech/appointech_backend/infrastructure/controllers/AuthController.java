package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.AutenticarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrarClienteUseCase registrarClienteUseCase;
    private final RegistrarTecnicoUseCase registrarTecnicoUseCase;
    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    public AuthController(RegistrarClienteUseCase registrarClienteUseCase,
                          RegistrarTecnicoUseCase registrarTecnicoUseCase,
                          AutenticarUsuarioUseCase autenticarUsuarioUseCase) {
        this.registrarClienteUseCase = registrarClienteUseCase;
        this.registrarTecnicoUseCase = registrarTecnicoUseCase;
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
    }

    @PostMapping("/registro/cliente")
    public ResponseEntity<UsuarioResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        Cliente cliente = registrarClienteUseCase.registrar(
                request.nombre(),
                request.apellido(),
                request.correo(),
                request.contrasena(),
                request.telefono(),
                request.tipoDocumento(),
                request.numeroDocumento(),
                request.direccion()
        );

        UsuarioResponse response = new UsuarioResponse(
                cliente.getUsuario().getId(),
                cliente.getUsuario().getNombre(),
                cliente.getUsuario().getApellido(),
                cliente.getUsuario().getCorreo(),
                cliente.getUsuario().getRol().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/registro/tecnico")
    public ResponseEntity<UsuarioResponse> registrarTecnico(@Valid @RequestBody RegistroTecnicoRequest request) {
        Tecnico tecnico = registrarTecnicoUseCase.registrar(
                request.nombre(),
                request.apellido(),
                request.correo(),
                request.contrasena(),
                request.telefono(),
                request.tipoDocumento(),
                request.numeroDocumento(),
                request.direccionBase(),
                request.radioCoberturaKm()
        );

        UsuarioResponse response = new UsuarioResponse(
                tecnico.getUsuario().getId(),
                tecnico.getUsuario().getNombre(),
                tecnico.getUsuario().getApellido(),
                tecnico.getUsuario().getCorreo(),
                tecnico.getUsuario().getRol().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = autenticarUsuarioUseCase.autenticar(request.correo(), request.contrasena());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}