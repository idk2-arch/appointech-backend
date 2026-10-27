package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.ports.in.CrearBloqueUseCase;
import com.appointech.appointech_backend.domain.ports.in.EliminarBloqueUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarBloquesUseCase;
import com.appointech.appointech_backend.infrastructure.dto.BloqueResponse;
import com.appointech.appointech_backend.infrastructure.dto.CrearBloqueRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tecnico/bloques")
@PreAuthorize("hasRole('TECNICO')")
public class TecnicoBloqueController {

    private final CrearBloqueUseCase crearBloqueUseCase;
    private final ListarBloquesUseCase listarBloquesUseCase;
    private final EliminarBloqueUseCase eliminarBloqueUseCase;

    public TecnicoBloqueController(CrearBloqueUseCase crearBloqueUseCase,
                                   ListarBloquesUseCase listarBloquesUseCase,
                                   EliminarBloqueUseCase eliminarBloqueUseCase) {
        this.crearBloqueUseCase = crearBloqueUseCase;
        this.listarBloquesUseCase = listarBloquesUseCase;
        this.eliminarBloqueUseCase = eliminarBloqueUseCase;
    }

    @PostMapping
    public ResponseEntity<BloqueResponse> crear(@Valid @RequestBody CrearBloqueRequest request,
                                                Authentication authentication) {
        BloqueIndisponibilidad bloque = crearBloqueUseCase.crear(
                authentication.getName(), request.inicio(), request.fin(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(TecnicoAgendaMappers.aRespuesta(bloque));
    }

    @GetMapping
    public ResponseEntity<List<BloqueResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        List<BloqueResponse> respuesta = listarBloquesUseCase.listar(authentication.getName(), desde, hasta)
                .stream()
                .map(TecnicoAgendaMappers::aRespuesta)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        eliminarBloqueUseCase.eliminar(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
