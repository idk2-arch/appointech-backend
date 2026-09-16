package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.CambiarEstadoUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearAdministradorUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarUsuariosUseCase;
import com.appointech.appointech_backend.infrastructure.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

    private final CrearAdministradorUseCase crearAdministradorUseCase;
    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    private final CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;

    public AdminUsuarioController(CrearAdministradorUseCase crearAdministradorUseCase,
                                  ListarUsuariosUseCase listarUsuariosUseCase,
                                  ActualizarUsuarioUseCase actualizarUsuarioUseCase,
                                  CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase) {
        this.crearAdministradorUseCase = crearAdministradorUseCase;
        this.listarUsuariosUseCase = listarUsuariosUseCase;
        this.actualizarUsuarioUseCase = actualizarUsuarioUseCase;
        this.cambiarEstadoUsuarioUseCase = cambiarEstadoUsuarioUseCase;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioAdminResponse>> listar() {
        List<Usuario> usuarios = listarUsuariosUseCase.listar();
        return ResponseEntity.ok(usuarios.stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminResponse> crearAdministrador(@Valid @RequestBody CrearAdministradorRequest request) {
        Usuario usuario = crearAdministradorUseCase.crear(
                request.nombre(),
                request.apellido(),
                request.correo(),
                request.contrasena(),
                request.telefono(),
                request.tipoDocumento(),
                request.numeroDocumento()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(usuario));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioAdminResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {

        Usuario usuario = actualizarUsuarioUseCase.actualizar(id, request.nombre(), request.apellido(), request.telefono());
        return ResponseEntity.ok(toResponse(usuario));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioAdminResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambiarEstadoUsuarioRequest request) {

        Usuario usuario = cambiarEstadoUsuarioUseCase.cambiarEstado(id, request.activo());
        return ResponseEntity.ok(toResponse(usuario));
    }

    private UsuarioAdminResponse toResponse(Usuario usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getTipoDocumento() != null ? usuario.getTipoDocumento().name() : null,
                usuario.getNumeroDocumento(),
                usuario.getRol().name(),
                usuario.isActivo(),
                usuario.getCreadoEn()
        );
    }
}