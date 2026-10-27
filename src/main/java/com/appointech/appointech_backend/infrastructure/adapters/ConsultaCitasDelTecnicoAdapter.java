package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitasDelTecnicoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Component
public class ConsultaCitasDelTecnicoAdapter implements ConsultaCitasDelTecnicoPort {

    private final CitasDelTecnicoJpaRepository citasDelTecnicoJpaRepository;

    public ConsultaCitasDelTecnicoAdapter(CitasDelTecnicoJpaRepository citasDelTecnicoJpaRepository) {
        this.citasDelTecnicoJpaRepository = citasDelTecnicoJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> buscarPorEstadosEntre(Long tecnicoId, Collection<EstadoCita> estados,
                                            LocalDateTime inicio, LocalDateTime fin) {
        List<EstadoCitaEntity> estadosEntidad = estados.stream()
                .map(estado -> EstadoCitaEntity.valueOf(estado.name()))
                .toList();
        return citasDelTecnicoJpaRepository.buscarPorEstadosEntre(tecnicoId, estadosEntidad, inicio, fin)
                .stream()
                .map(this::aDominio)
                .toList();
    }

    private Cita aDominio(CitaEntity entity) {
        Cita cita = new Cita();
        cita.setId(entity.getId());
        cita.setClienteId(entity.getClienteId());
        cita.setTecnicoId(entity.getTecnicoId());
        cita.setEspecialidadId(entity.getEspecialidadId());
        cita.setDescripcionProblema(entity.getDescripcionProblema());
        cita.setDireccionServicio(entity.getDireccionServicio());
        cita.setLatitud(entity.getLatitud());
        cita.setLongitud(entity.getLongitud());
        cita.setFechaHoraInicio(entity.getFechaHoraInicio());
        cita.setFechaHoraFin(entity.getFechaHoraFin());
        cita.setEstado(EstadoCita.valueOf(entity.getEstado().name()));
        cita.setDiagnostico(entity.getDiagnostico());
        cita.setCreadaEn(entity.getCreadaEn());
        cita.setActualizadaEn(entity.getActualizadaEn());
        return cita;
    }
}
