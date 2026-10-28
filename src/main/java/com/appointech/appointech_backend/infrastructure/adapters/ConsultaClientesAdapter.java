package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.infrastructure.entities.ClienteEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ClienteJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class ConsultaClientesAdapter implements ConsultaClientesPort {

    private final ClienteJpaRepository clienteJpaRepository;

    public ConsultaClientesAdapter(ClienteJpaRepository clienteJpaRepository) {
        this.clienteJpaRepository = clienteJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ClienteResumen> resumenesPorId(Collection<Long> clienteIds) {
        Map<Long, ClienteResumen> resumenes = new HashMap<>();
        if (clienteIds.isEmpty()) {
            return resumenes;
        }
        for (ClienteEntity cliente : clienteJpaRepository.findAllById(clienteIds)) {
            resumenes.put(cliente.getId(), new ClienteResumen(
                    cliente.getUsuario().getNombre(),
                    cliente.getUsuario().getApellido(),
                    cliente.getUsuario().getTelefono(),
                    cliente.getUsuario().getFotoClave()));
        }
        return resumenes;
    }
}
