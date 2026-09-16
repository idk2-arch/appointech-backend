package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.ports.in.ListarEspecialidadesUseCase;
import com.appointech.appointech_backend.infrastructure.dto.EspecialidadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final ListarEspecialidadesUseCase listarEspecialidadesUseCase;

    public EspecialidadController(ListarEspecialidadesUseCase listarEspecialidadesUseCase) {
        this.listarEspecialidadesUseCase = listarEspecialidadesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<EspecialidadResponse>> listar() {
        List<Especialidad> especialidades = listarEspecialidadesUseCase.listar();

        List<EspecialidadResponse> response = especialidades.stream()
                .map(e -> new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion()))
                .toList();

        return ResponseEntity.ok(response);
    }
}