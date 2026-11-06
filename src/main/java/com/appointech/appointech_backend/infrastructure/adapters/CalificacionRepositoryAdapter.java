package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.exception.CitaYaCalificadaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.ports.out.CalificacionRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.CalificacionEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CalificacionJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.CalificacionRecibidaFila;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CalificacionRepositoryAdapter implements CalificacionRepositoryPort {

    private final CalificacionJpaRepository calificacionJpaRepository;

    public CalificacionRepositoryAdapter(CalificacionJpaRepository calificacionJpaRepository) {
        this.calificacionJpaRepository = calificacionJpaRepository;
    }

    @Override
    public Calificacion guardar(Calificacion calificacion) {
        CalificacionEntity entity = new CalificacionEntity();
        entity.setCitaId(calificacion.citaId());
        entity.setPuntuacion((short) calificacion.puntuacion());
        entity.setComentario(calificacion.comentario());
        entity.setCreadaEn(calificacion.creadaEn());
        try {
            return aDominio(calificacionJpaRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones a la vez: la restricción UNIQUE de la cita decide cuál gana.
            throw new CitaYaCalificadaException();
        }
    }

    @Override
    public Optional<Calificacion> buscarPorCitaId(Long citaId) {
        return calificacionJpaRepository.findByCitaId(citaId).map(this::aDominio);
    }

    @Override
    public ListadoConTotal<CalificacionRecibida> listarRecibidasPorTecnico(Long tecnicoId, PaginacionOpcional paginacion) {
        Pageable pageable = paginacion.paginado() ? PageRequest.of(paginacion.pagina(), paginacion.tamanio()) : Pageable.unpaged();
        Page<CalificacionRecibidaFila> pagina = calificacionJpaRepository.buscarRecibidasPorTecnico(tecnicoId, pageable);
        return new ListadoConTotal<>(pagina.getContent().stream().map(this::aRecibida).toList(), pagina.getTotalElements());
    }

    @Override
    public Optional<Double> promedioDelTecnico(Long tecnicoId) {
        return Optional.ofNullable(calificacionJpaRepository.promedioDelTecnico(tecnicoId));
    }

    private Calificacion aDominio(CalificacionEntity entity) {
        return new Calificacion(entity.getId(), entity.getCitaId(), entity.getPuntuacion(), entity.getComentario(),
                entity.getCreadaEn());
    }

    private CalificacionRecibida aRecibida(CalificacionRecibidaFila fila) {
        return new CalificacionRecibida(fila.citaId(), fila.puntuacion(), fila.comentario(), fila.creadaEn(),
                fila.clienteNombre());
    }
}
