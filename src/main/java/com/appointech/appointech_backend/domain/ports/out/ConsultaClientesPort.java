package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.ClienteResumen;

import java.util.Collection;
import java.util.Map;

public interface ConsultaClientesPort {

    /** Datos de contacto por id de cliente; los ids que no existen no aparecen en el mapa. */
    Map<Long, ClienteResumen> resumenesPorId(Collection<Long> clienteIds);
}
