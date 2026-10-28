package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Repositorio falso en memoria de los metadatos de fotos de cita. */
public class CitaFotoRepositoryEnMemoria implements CitaFotoRepositoryPort {

    private final Map<Long, CitaFoto> filas = new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public CitaFoto guardar(CitaFoto foto) {
        CitaFoto guardada = new CitaFoto(foto.id() != null ? foto.id() : siguienteId++,
                foto.citaId(), foto.clave(), foto.creadoEn());
        filas.put(guardada.id(), guardada);
        return guardada;
    }

    @Override
    public Optional<CitaFoto> buscarPorIdYCita(Long fotoId, Long citaId) {
        return Optional.ofNullable(filas.get(fotoId)).filter(f -> f.citaId().equals(citaId));
    }

    @Override
    public List<CitaFoto> listarPorCita(Long citaId) {
        return filas.values().stream().filter(f -> f.citaId().equals(citaId)).toList();
    }

    @Override
    public Map<Long, List<CitaFoto>> listarPorCitas(Collection<Long> citaIds) {
        return filas.values().stream().filter(f -> citaIds.contains(f.citaId()))
                .collect(Collectors.groupingBy(CitaFoto::citaId));
    }

    @Override
    public long contarPorCita(Long citaId) {
        return listarPorCita(citaId).size();
    }

    @Override
    public void eliminar(Long fotoId) {
        filas.remove(fotoId);
    }

    public int total() {
        return filas.size();
    }
}
