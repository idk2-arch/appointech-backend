package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.infrastructure.dto.EspecialidadResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoResponse;

final class TecnicoResponseMapper {

    private TecnicoResponseMapper() {
    }

    static TecnicoResponse aRespuesta(Tecnico tecnico) {
        Usuario usuario = tecnico.getUsuario();
        return new TecnicoResponse(
                tecnico.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                tecnico.getDireccionBase(),
                tecnico.getRadioCoberturaKm(),
                tecnico.getEspecialidades().stream()
                        .map(e -> new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion()))
                        .toList(),
                usuario.getId(),
                usuario.getTelefono(),
                usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(),
                usuario.isActivo(),
                tecnico.getLatitudBase(),
                tecnico.getLongitudBase()
        );
    }
}
