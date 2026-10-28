package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;

public interface ConsultaCitasAdminPort {

    /** Página de citas filtrada, ordenada por fecha y hora de inicio descendente. */
    PaginaResultado<CitaResumenAdmin> buscar(FiltroCitasAdmin filtro);
}
