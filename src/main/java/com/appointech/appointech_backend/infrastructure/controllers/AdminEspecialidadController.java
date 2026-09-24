package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EspecialidadAdminInfo;
import com.appointech.appointech_backend.domain.ports.in.*;
import com.appointech.appointech_backend.infrastructure.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/especialidades")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminEspecialidadController {

    private final ListarEspecialidadesAdminUseCase listarEspecialidadesAdminUseCase;
    private final CrearEspecialidadUseCase crearEspecialidadUseCase;
    private final ActualizarEspecialidadUseCase actualizarEspecialidadUseCase;
    private final CambiarEstadoEspecialidadUseCase cambiarEstadoEspecialidadUseCase;

    public AdminEspecialidadController(ListarEspecialidadesAdminUseCase listarEspecialidadesAdminUseCase,
                                       CrearEspecialidadUseCase crearEspecialidadUseCase,
                                       ActualizarEspecialidadUseCase actualizarEspecialidadUseCase,
                                       CambiarEstadoEspecialidadUseCase cambiarEstadoEspecialidadUseCase) {
        this.listarEspecialidadesAdminUseCase = listarEspecialidadesAdminUseCase;
        this.crearEspecialidadUseCase = crearEspecialidadUseCase;
        this.actualizarEspecialidadUseCase = actualizarEspecialidadUseCase;
        this.cambiarEstadoEspecialidadUseCase = cambiarEstadoEspecialidadUseCase;
    }

    @GetMapping
    public ResponseEntity<List<EspecialidadAdminResponse>> listar() {
        List<EspecialidadAdminInfo> especialidades = listarEspecialidadesAdminUseCase.listar();
        return ResponseEntity.ok(especialidades.stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<EspecialidadAdminResponse> crear(@Valid @RequestBody CrearEspecialidadRequest request) {
        Especialidad especialidad = crearEspecialidadUseCase.crear(request.nombre(), request.descripcion());
        EspecialidadAdminResponse response = toResponse(new EspecialidadAdminInfo(especialidad, 0));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EspecialidadAdminResponse> actualizar(@PathVariable Long id,
                                                                @Valid @RequestBody ActualizarEspecialidadRequest request) {
        Especialidad especialidad = actualizarEspecialidadUseCase.actualizar(id, request.nombre(), request.descripcion());
        return ResponseEntity.ok(toResponse(new EspecialidadAdminInfo(especialidad, 0)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EspecialidadAdminResponse> cambiarEstado(@PathVariable Long id,
                                                                   @RequestBody CambiarEstadoEspecialidadRequest request) {
        Especialidad especialidad = cambiarEstadoEspecialidadUseCase.cambiarEstado(id, request.activo());
        return ResponseEntity.ok(toResponse(new EspecialidadAdminInfo(especialidad, 0)));
    }

    private EspecialidadAdminResponse toResponse(EspecialidadAdminInfo info) {
        Especialidad e = info.getEspecialidad();
        return new EspecialidadAdminResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.isActivo(), info.getTecnicosAsignados());
    }
}