package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.DatosActualizacionCliente;
import com.appointech.appointech_backend.domain.ports.in.ActualizarClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarClientesUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarClienteRequest;
import com.appointech.appointech_backend.infrastructure.dto.ClienteAdminResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/clientes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminClienteController {

    private final ListarClientesUseCase listarClientesUseCase;
    private final ActualizarClienteUseCase actualizarClienteUseCase;

    public AdminClienteController(ListarClientesUseCase listarClientesUseCase,
                                  ActualizarClienteUseCase actualizarClienteUseCase) {
        this.listarClientesUseCase = listarClientesUseCase;
        this.actualizarClienteUseCase = actualizarClienteUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ClienteAdminResponse>> listar() {
        return ResponseEntity.ok(listarClientesUseCase.listar().stream()
                .map(ClienteAdminResponseMapper::aRespuesta)
                .toList());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClienteAdminResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody ActualizarClienteRequest request) {
        DatosActualizacionCliente datos = new DatosActualizacionCliente(
                request.nombre(),
                request.apellido(),
                request.correo(),
                request.telefono(),
                request.tipoDocumento(),
                request.numeroDocumento(),
                request.direccion(),
                request.latitud(),
                request.longitud()
        );
        return ResponseEntity.ok(ClienteAdminResponseMapper.aRespuesta(actualizarClienteUseCase.actualizar(id, datos)));
    }
}
