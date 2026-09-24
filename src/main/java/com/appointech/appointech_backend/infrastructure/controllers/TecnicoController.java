package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarTecnicosUseCase;
import com.appointech.appointech_backend.infrastructure.dto.AsignarEspecialidadesRequest;
import com.appointech.appointech_backend.infrastructure.dto.EspecialidadResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase;
    private final ListarTecnicosUseCase listarTecnicosUseCase;

    public TecnicoController(AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase,
                             ListarTecnicosUseCase listarTecnicosUseCase) {
        this.asignarEspecialidadesTecnicoUseCase = asignarEspecialidadesTecnicoUseCase;
        this.listarTecnicosUseCase = listarTecnicosUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<TecnicoResponse>> listar() {
        List<Tecnico> tecnicos = listarTecnicosUseCase.listar();
        return ResponseEntity.ok(tecnicos.stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{id}/especialidades")
    public ResponseEntity<TecnicoResponse> asignarEspecialidades(
            @PathVariable Long id,
            @Valid @RequestBody AsignarEspecialidadesRequest request) {

        Tecnico tecnico = asignarEspecialidadesTecnicoUseCase.asignar(id, request.especialidadIds());
        return ResponseEntity.ok(toResponse(tecnico));
    }

    private TecnicoResponse toResponse(Tecnico tecnico) {
        return new TecnicoResponse(
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
    }
}