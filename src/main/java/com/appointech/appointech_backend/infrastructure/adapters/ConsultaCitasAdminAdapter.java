package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasAdminPort;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaAdminFila;
import com.appointech.appointech_backend.infrastructure.repositories.CitasAdminJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class ConsultaCitasAdminAdapter implements ConsultaCitasAdminPort {

    private static final LocalDateTime SIN_LIMITE_INFERIOR = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime SIN_LIMITE_SUPERIOR = LocalDateTime.of(9999, 1, 1, 0, 0);
    private static final long TECNICO_NINGUNO = -1L;

    private final CitasAdminJpaRepository citasAdminJpaRepository;

    public ConsultaCitasAdminAdapter(CitasAdminJpaRepository citasAdminJpaRepository) {
        this.citasAdminJpaRepository = citasAdminJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResultado<CitaResumenAdmin> buscar(FiltroCitasAdmin filtro) {
        boolean todosLosTecnicos = filtro.tecnicoId() == null;
        Page<CitaAdminFila> pagina = citasAdminJpaRepository.buscar(
                todosLosTecnicos,
                todosLosTecnicos ? TECNICO_NINGUNO : filtro.tecnicoId(),
                estados(filtro.estado()),
                filtro.fechaDesde() == null ? SIN_LIMITE_INFERIOR : filtro.fechaDesde().atStartOfDay(),
                filtro.fechaHasta() == null ? SIN_LIMITE_SUPERIOR : filtro.fechaHasta().plusDays(1).atStartOfDay(),
                PageRequest.of(filtro.pagina(), filtro.tamanio()));
        return new PaginaResultado<>(
                pagina.getContent().stream().map(this::aDominio).toList(),
                filtro.pagina(), filtro.tamanio(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    private List<EstadoCitaEntity> estados(EstadoCita estado) {
        return estado == null
                ? Arrays.asList(EstadoCitaEntity.values())
                : List.of(EstadoCitaEntity.valueOf(estado.name()));
    }

    private CitaResumenAdmin aDominio(CitaAdminFila fila) {
        return new CitaResumenAdmin(
                fila.id(), fila.fechaHoraInicio(), fila.fechaHoraFin(), EstadoCita.valueOf(fila.estado().name()),
                nombreCompleto(fila.clienteNombre(), fila.clienteApellido()),
                nombreCompleto(fila.tecnicoNombre(), fila.tecnicoApellido()),
                fila.servicio(), fila.direccion());
    }

    private String nombreCompleto(String nombre, String apellido) {
        return nombre == null ? null : nombre + " " + apellido;
    }
}
