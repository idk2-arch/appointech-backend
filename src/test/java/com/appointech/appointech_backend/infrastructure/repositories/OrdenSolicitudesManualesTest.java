package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** El orden de las solicitudes por coordinar vive en la consulta; esta prueba lo fija para que no cambie sin querer. */
class OrdenSolicitudesManualesTest {

    @Test
    void ordenaPorFechaDeseadaAscendenteConNulosAlFinalYLuegoPorAntiguedad() throws NoSuchMethodException {
        Query consulta = CitaJpaRepository.class
                .getMethod("buscarSolicitudesManuales", EstadoCitaEntity.class, boolean.class, String.class,
                        boolean.class, Long.class, Pageable.class)
                .getAnnotation(Query.class);

        assertTrue(consulta.value().endsWith("ORDER BY c.fechaDeseada ASC NULLS LAST, c.creadaEn ASC, c.id ASC"),
                consulta.value());
    }
}
