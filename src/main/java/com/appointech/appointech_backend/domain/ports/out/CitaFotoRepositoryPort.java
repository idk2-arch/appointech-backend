package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.CitaFoto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CitaFotoRepositoryPort {
    CitaFoto guardar(CitaFoto foto);

    Optional<CitaFoto> buscarPorIdYCita(Long fotoId, Long citaId);

    /** Fotos de la cita, las más antiguas primero. */
    List<CitaFoto> listarPorCita(Long citaId);

    /** Fotos de varias citas en una sola consulta; las citas sin fotos no aparecen en el mapa. */
    Map<Long, List<CitaFoto>> listarPorCitas(Collection<Long> citaIds);

    long contarPorCita(Long citaId);

    void eliminar(Long fotoId);
}
