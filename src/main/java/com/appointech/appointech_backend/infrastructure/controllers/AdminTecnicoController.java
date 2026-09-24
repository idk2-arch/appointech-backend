package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CrearTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.EspecialidadResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tecnicos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminTecnicoController {

    private final RegistrarTecnicoUseCase registrarTecnicoUseCase;

    public AdminTecnicoController(RegistrarTecnicoUseCase registrarTecnicoUseCase) {
        this.registrarTecnicoUseCase = registrarTecnicoUseCase;
    }

    @PostMapping
    public ResponseEntity<TecnicoResponse> crear(@Valid @RequestBody CrearTecnicoRequest request) {
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

        TecnicoResponse response = new TecnicoResponse(
                tecnico.getId(),
                tecnico.getUsuario().getNombre(),
                tecnico.getUsuario().getApellido(),
                tecnico.getUsuario().getCorreo(),
                tecnico.getDireccionBase(),
                tecnico.getRadioCoberturaKm(),
                tecnico.getEspecialidades().stream()
                        .map(e -> new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion()))
                        .toList()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}