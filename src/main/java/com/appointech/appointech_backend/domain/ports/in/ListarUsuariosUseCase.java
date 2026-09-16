package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Usuario;

import java.util.List;

public interface ListarUsuariosUseCase {
    List<Usuario> listar();
}