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
    public ResponseEntity<ApiResponse<List<EspecialidadAdminResponse>>> listar() {
        List<EspecialidadAdminInfo> especialidades = listarEspecialidadesAdminUseCase.listar();
        List<EspecialidadAdminResponse> response = especialidades.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.exito(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EspecialidadAdminResponse>> crear(@Valid @RequestBody CrearEspecialidadRequest request) {
        Especialidad especialidad = crearEspecialidadUseCase.crear(request.nombre(), request.descripcion());
        EspecialidadAdminResponse response = toResponse(new EspecialidadAdminInfo(especialidad, 0));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exito(response, "Especialidad creada correctamente"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<EspecialidadAdminResponse>> actualizar(@PathVariable Long id,
                                                                             @Valid @RequestBody ActualizarEspecialidadRequest request) {
        Especialidad especialidad = actualizarEspecialidadUseCase.actualizar(id, request.nombre(), request.descripcion());
        EspecialidadAdminResponse response = toResponse(new EspecialidadAdminInfo(especialidad, 0));
        return ResponseEntity.ok(ApiResponse.exito(response, "Especialidad actualizada correctamente"));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<EspecialidadAdminResponse>> cambiarEstado(@PathVariable Long id,
                                                                                @RequestBody CambiarEstadoEspecialidadRequest request) {
        Especialidad especialidad = cambiarEstadoEspecialidadUseCase.cambiarEstado(id, request.activo());
        EspecialidadAdminResponse response = toResponse(new EspecialidadAdminInfo(especialidad, 0));
        String mensaje = request.activo() ? "Especialidad activada correctamente" : "Especialidad desactivada correctamente";
        return ResponseEntity.ok(ApiResponse.exito(response, mensaje));
    }

    private EspecialidadAdminResponse toResponse(EspecialidadAdminInfo info) {
        Especialidad e = info.getEspecialidad();
        return new EspecialidadAdminResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.isActivo(), info.getTecnicosAsignados());
    }
}