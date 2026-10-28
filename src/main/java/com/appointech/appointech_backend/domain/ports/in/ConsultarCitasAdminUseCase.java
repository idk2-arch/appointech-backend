package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;

public interface ConsultarCitasAdminUseCase {

    /** Citas de todos los técnicos, de la más reciente a la más antigua, según los filtros indicados. */
    PaginaResultado<CitaResumenAdmin> consultar(FiltroCitasAdmin filtro);
}
