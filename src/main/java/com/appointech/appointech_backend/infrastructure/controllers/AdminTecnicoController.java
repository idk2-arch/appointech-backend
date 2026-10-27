package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.DatosActualizacionTecnico;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ActualizarTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ActualizarUbicacionTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearTecnicoConEspecialidadesUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarUbicacionTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.ApiResponse;
import com.appointech.appointech_backend.infrastructure.dto.CrearTecnicoRequest;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoResponse;
import com.appointech.appointech_backend.infrastructure.dto.UbicacionTecnicoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tecnicos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminTecnicoController {

    private final CrearTecnicoConEspecialidadesUseCase crearTecnicoConEspecialidadesUseCase;
    private final ActualizarTecnicoUseCase actualizarTecnicoUseCase;
    private final ActualizarUbicacionTecnicoUseCase actualizarUbicacionTecnicoUseCase;

    public AdminTecnicoController(CrearTecnicoConEspecialidadesUseCase crearTecnicoConEspecialidadesUseCase,
                                  ActualizarTecnicoUseCase actualizarTecnicoUseCase,
                                  ActualizarUbicacionTecnicoUseCase actualizarUbicacionTecnicoUseCase) {
        this.crearTecnicoConEspecialidadesUseCase = crearTecnicoConEspecialidadesUseCase;
        this.actualizarTecnicoUseCase = actualizarTecnicoUseCase;
        this.actualizarUbicacionTecnicoUseCase = actualizarUbicacionTecnicoUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TecnicoResponse>> crear(@Valid @RequestBody CrearTecnicoRequest request) {
        Tecnico tecnico = crearTecnicoConEspecialidadesUseCase.crear(
                request.nombre(),
                request.apellido(),
                request.correo(),
                request.contrasena(),
                blancoANulo(request.telefono()),
                request.tipoDocumento(),
                blancoANulo(request.numeroDocumento()),
                request.direccionBase(),
                null, // el radio por técnico ya no se usa (límite único del sistema)
                request.latitudBase(),
                request.longitudBase(),
                request.especialidadIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.exito(TecnicoResponseMapper.aRespuesta(tecnico), "Técnico creado correctamente"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TecnicoResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ActualizarTecnicoRequest request) {
        DatosActualizacionTecnico datos = new DatosActualizacionTecnico(
                request.nombre(),
                request.apellido(),
                request.correo(),
                request.telefono(),
                request.tipoDocumento(),
                request.numeroDocumento(),
                request.direccionBase(),
                null, // el radio por técnico ya no se usa (límite único del sistema)
                request.latitudBase(),
                request.longitudBase(),
                request.especialidadIds()
        );
        Tecnico tecnico = actualizarTecnicoUseCase.actualizar(id, datos);
        return ResponseEntity.ok(TecnicoResponseMapper.aRespuesta(tecnico));
    }

    @PatchMapping("/{id}/ubicacion")
    public ResponseEntity<ApiResponse<UbicacionTecnicoResponse>> actualizarUbicacion(
            @PathVariable Long id, @Valid @RequestBody ActualizarUbicacionTecnicoRequest request) {
        Tecnico tecnico = actualizarUbicacionTecnicoUseCase.actualizar(id, request.latitud(), request.longitud());
        UbicacionTecnicoResponse response = new UbicacionTecnicoResponse(
                tecnico.getId(), tecnico.getLatitudBase(), tecnico.getLongitudBase());
        return ResponseEntity.ok(ApiResponse.exito(response, "Ubicación del técnico actualizada correctamente"));
    }

    private static String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
