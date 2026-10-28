package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.DatosPerfilEditable;
import com.appointech.appointech_backend.domain.models.PerfilPropio;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarPerfilPropioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPerfilPropioUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ActualizarPerfilRequest;
import com.appointech.appointech_backend.infrastructure.dto.EspecialidadResponse;
import com.appointech.appointech_backend.infrastructure.dto.PerfilResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El usuario se identifica siempre por el JWT; nunca por un id del cuerpo o de la URL. */
@RestController
@RequestMapping("/api/perfil")
public class PerfilPropioController {

    private final ConsultarPerfilPropioUseCase consultarPerfil;
    private final ActualizarPerfilPropioUseCase actualizarPerfil;

    public PerfilPropioController(ConsultarPerfilPropioUseCase consultarPerfil,
                                  ActualizarPerfilPropioUseCase actualizarPerfil) {
        this.consultarPerfil = consultarPerfil;
        this.actualizarPerfil = actualizarPerfil;
    }

    @GetMapping
    public ResponseEntity<PerfilResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok(aRespuesta(consultarPerfil.consultar(authentication.getName())));
    }

    @PutMapping
    public ResponseEntity<PerfilResponse> actualizar(@Valid @RequestBody ActualizarPerfilRequest request,
                                                     Authentication authentication) {
        DatosPerfilEditable datos = new DatosPerfilEditable(
                request.nombre(), request.apellido(), request.telefono(), request.direccion());
        return ResponseEntity.ok(aRespuesta(actualizarPerfil.actualizar(authentication.getName(), datos)));
    }

    private static PerfilResponse aRespuesta(PerfilPropio perfil) {
        Usuario u = perfil.usuario();
        return new PerfilResponse(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(), u.getTelefono(),
                u.getTipoDocumento(), u.getNumeroDocumento(), u.getRol().name(), perfil.direccion(),
                perfil.radioCoberturaKm(),
                perfil.especialidades().stream()
                        .map(e -> new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion())).toList(),
                FotoUrlMapper.aUrl(u.getFotoClave()));
    }
}
