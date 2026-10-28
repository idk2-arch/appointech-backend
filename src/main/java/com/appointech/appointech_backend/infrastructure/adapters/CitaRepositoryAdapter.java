package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class CitaRepositoryAdapter implements CitaRepositoryPort {

    private final CitaJpaRepository citaJpaRepository;

    public CitaRepositoryAdapter(CitaJpaRepository citaJpaRepository) {
        this.citaJpaRepository = citaJpaRepository;
    }

    @Override
    public Cita guardar(Cita cita) {
        CitaEntity guardada = citaJpaRepository.save(toEntity(cita));
        return toDomain(guardada);
    }

    @Override
    public Optional<Cita> buscarPorId(Long id) {
        return citaJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Cita> listarPorCliente(Long clienteId) {
        return citaJpaRepository.findByClienteIdOrderByCreadaEnDescIdDesc(clienteId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Cita> buscarActivasDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
        return citaJpaRepository.buscarDelTecnicoEntre(tecnicoId, inicio, fin, EstadoCitaEntity.CANCELADA)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Cita> buscarSolicitudesManualesPendientes() {
        return citaJpaRepository.buscarSolicitudesManualesPorEstado(EstadoCitaEntity.PENDIENTE)
                .stream().map(this::toDomain).toList();
    }

    private CitaEntity toEntity(Cita cita) {
        CitaEntity entity = new CitaEntity();
        entity.setId(cita.getId());
        entity.setClienteId(cita.getClienteId());
        entity.setTecnicoId(cita.getTecnicoId());
        entity.setEspecialidadId(cita.getEspecialidadId());
        entity.setDescripcionProblema(cita.getDescripcionProblema());
        entity.setDireccionServicio(cita.getDireccionServicio());
        entity.setLatitud(cita.getLatitud());
        entity.setLongitud(cita.getLongitud());
        entity.setFechaHoraInicio(cita.getFechaHoraInicio());
        entity.setFechaHoraFin(cita.getFechaHoraFin());
        entity.setEstado(EstadoCitaEntity.valueOf(cita.getEstado().name()));
        entity.setDiagnostico(cita.getDiagnostico());
        entity.setCreadaEn(cita.getCreadaEn());
        entity.setActualizadaEn(cita.getActualizadaEn());
        entity.setAgendamientoManual(cita.isAgendamientoManual());
        entity.setTelefonoContacto(cita.getTelefonoContacto());
        entity.setRecomendaciones(cita.getRecomendaciones());
        entity.setFechaDiagnostico(cita.getFechaDiagnostico());
        return entity;
    }

    private Cita toDomain(CitaEntity entity) {
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
        cita.setAgendamientoManual(entity.isAgendamientoManual());
        cita.setTelefonoContacto(entity.getTelefonoContacto());
        cita.setRecomendaciones(entity.getRecomendaciones());
        cita.setFechaDiagnostico(entity.getFechaDiagnostico());
        return cita;
    }
}
