package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.exception.CitaYaCalificadaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.infrastructure.entities.CalificacionEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CalificacionJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.CalificacionRecibidaFila;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalificacionRepositoryAdapterTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 1, 12, 0);

    @Mock
    private CalificacionJpaRepository jpa;
    @InjectMocks
    private CalificacionRepositoryAdapter adapter;

    private static CalificacionEntity entity(long id, long citaId, int puntuacion, String comentario) {
        CalificacionEntity e = new CalificacionEntity();
        e.setId(id);
        e.setCitaId(citaId);
        e.setPuntuacion((short) puntuacion);
        e.setComentario(comentario);
        e.setCreadaEn(FECHA);
        return e;
    }

    @Test
    void guardarConvierteLosCamposYDevuelveLaCalificacionConId() {
        when(jpa.save(any(CalificacionEntity.class))).thenAnswer(inv -> {
            CalificacionEntity e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });

        Calificacion guardada = adapter.guardar(Calificacion.nueva(10L, 4, "Bien", FECHA));

        ArgumentCaptor<CalificacionEntity> captor = ArgumentCaptor.forClass(CalificacionEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals(10L, captor.getValue().getCitaId());
        assertEquals((short) 4, captor.getValue().getPuntuacion());
        assertEquals("Bien", captor.getValue().getComentario());
        assertEquals(FECHA, captor.getValue().getCreadaEn());
        assertEquals(new Calificacion(5L, 10L, 4, "Bien", FECHA), guardada);
    }

    @Test
    void siLaRestriccionUnicaDeLaCitaFallaEsUnaCitaYaCalificada() {
        when(jpa.save(any(CalificacionEntity.class))).thenThrow(new DataIntegrityViolationException("uk cita"));
        Calificacion nueva = Calificacion.nueva(10L, 4, null, FECHA);

        assertThrows(CitaYaCalificadaException.class, () -> adapter.guardar(nueva));
    }

    @Test
    void buscarPorCitaIdMapeaYRespetaAusencia() {
        when(jpa.findByCitaId(10L)).thenReturn(Optional.of(entity(5, 10, 3, null)));
        when(jpa.findByCitaId(11L)).thenReturn(Optional.empty());

        assertEquals(new Calificacion(5L, 10L, 3, null, FECHA), adapter.buscarPorCitaId(10L).orElseThrow());
        assertTrue(adapter.buscarPorCitaId(11L).isEmpty());
    }

    @Test
    void sinPaginacionPideTodoYDevuelveElTotal() {
        when(jpa.buscarRecibidasPorTecnico(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new CalificacionRecibidaFila(12L, (short) 5, "Top", FECHA, "Ana"),
                new CalificacionRecibidaFila(11L, (short) 3, null, FECHA.minusDays(1), "Luz"))));

        ListadoConTotal<CalificacionRecibida> r = adapter.listarRecibidasPorTecnico(3L, PaginacionOpcional.de(null, null));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).buscarRecibidasPorTecnico(org.mockito.ArgumentMatchers.eq(3L), pageable.capture());
        assertTrue(pageable.getValue().isUnpaged());
        assertEquals(2, r.total());
        assertEquals(new CalificacionRecibida(12L, 5, "Top", FECHA, "Ana"), r.elementos().get(0));
        assertNull(r.elementos().get(1).comentario());
        assertEquals("Luz", r.elementos().get(1).clienteNombre());
    }

    @Test
    void conPaginacionPideEsaPaginaYElTotalEsElDeTodas() {
        Pageable esperado = PageRequest.of(2, 5);
        when(jpa.buscarRecibidasPorTecnico(3L, esperado)).thenReturn(new PageImpl<>(
                List.of(new CalificacionRecibidaFila(12L, (short) 5, "Top", FECHA, "Ana")), esperado, 11));

        ListadoConTotal<CalificacionRecibida> r = adapter.listarRecibidasPorTecnico(3L, PaginacionOpcional.de(2, 5));

        assertEquals(1, r.elementos().size());
        assertEquals(11, r.total());
    }

    @Test
    void elPromedioVaciaSiElTecnicoNoTieneCalificaciones() {
        when(jpa.promedioDelTecnico(3L)).thenReturn(4.5);
        when(jpa.promedioDelTecnico(4L)).thenReturn(null);

        assertEquals(Optional.of(4.5), adapter.promedioDelTecnico(3L));
        assertTrue(adapter.promedioDelTecnico(4L).isEmpty());
    }
}
