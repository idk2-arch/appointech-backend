package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cliente;
import java.util.Optional;

public interface ClienteRepositoryPort {
    Cliente guardar(Cliente cliente);
    Optional<Cliente> buscarPorUsuarioId(Long usuarioId);
}