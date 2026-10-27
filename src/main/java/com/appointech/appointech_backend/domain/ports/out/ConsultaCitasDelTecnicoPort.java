package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ConsultaCitasDelTecnicoPort {

    /** Citas del técnico en esos estados que se cruzan con [inicio, fin), ordenadas por hora de inicio. */
    List<Cita> buscarPorEstadosEntre(Long tecnicoId, Collection<EstadoCita> estados,
                                     LocalDateTime inicio, LocalDateTime fin);
}
