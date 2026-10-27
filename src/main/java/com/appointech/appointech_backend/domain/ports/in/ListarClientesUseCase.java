package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cliente;

import java.util.List;

public interface ListarClientesUseCase {
    List<Cliente> listar();
}
