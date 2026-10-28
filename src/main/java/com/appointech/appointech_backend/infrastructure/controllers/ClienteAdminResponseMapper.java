package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.infrastructure.dto.ClienteAdminResponse;

final class ClienteAdminResponseMapper {

    private ClienteAdminResponseMapper() {
    }

    static ClienteAdminResponse aRespuesta(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        return new ClienteAdminResponse(
                cliente.getId(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(),
                usuario.isActivo(),
                cliente.getDireccion(),
                cliente.getLatitud(),
                cliente.getLongitud(),
                FotoUrlMapper.aUrl(usuario.getFotoClave())
        );
    }
}
