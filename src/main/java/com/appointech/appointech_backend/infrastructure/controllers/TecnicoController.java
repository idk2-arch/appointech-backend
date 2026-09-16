package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.AsignarEspecialidadesRequest;
import com.appointech.appointech_backend.infrastructure.dto.EspecialidadResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoController {

    private final AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase;

    public TecnicoController(AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase) {
        this.asignarEspecialidadesTecnicoUseCase = asignarEspecialidadesTecnicoUseCase;
    }

    @PatchMapping("/{id}/especialidades")
    public ResponseEntity<TecnicoResponse> asignarEspecialidades(
            @PathVariable Long id,
            @Valid @RequestBody AsignarEspecialidadesRequest request) {

        Tecnico tecnico = asignarEspecialidadesTecnicoUseCase.asignar(id, request.especialidadIds());

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

        return ResponseEntity.ok(response);
    }
}