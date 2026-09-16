package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {
    Usuario guardar(Usuario usuario);
    Optional<Usuario> buscarPorCorreo(String correo);
    Optional<Usuario> buscarPorId(Long id);
    List<Usuario> listarTodos();
}