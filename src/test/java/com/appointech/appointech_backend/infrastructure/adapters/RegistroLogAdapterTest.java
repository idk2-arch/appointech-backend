package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.infrastructure.entities.LogSistemaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.LogSistemaJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroLogAdapterTest {

    @Mock
    private LogSistemaJpaRepository jpa;
    @InjectMocks
    private RegistroLogAdapter adapter;

    private LogSistemaEntity entity(long id) {
        LogSistemaEntity e = new LogSistemaEntity();
        e.setId(id);
        e.setNivel("INFO");
        e.setTipo("NEGOCIO");
        e.setOrigen("CitaService");
        e.setUsuario("ana@mail.com");
        e.setMensaje("Cita creada");
        e.setCreadoEn(LocalDateTime.of(2026, 6, 1, 8, 0));
        return e;
    }

    @Test
    void registrarPersisteTodosLosCamposConFechaActual() {
        LocalDateTime antes = LocalDateTime.now();

        adapter.registrar(NivelLog.WARN, TipoLog.SEGURIDAD, "Filtro", "admin@mail.com", "Acceso denegado");

        ArgumentCaptor<LogSistemaEntity> captor = ArgumentCaptor.forClass(LogSistemaEntity.class);
        verify(jpa).save(captor.capture());
        LogSistemaEntity e = captor.getValue();
        assertEquals("WARN", e.getNivel());
        assertEquals("SEGURIDAD", e.getTipo());
        assertEquals("Filtro", e.getOrigen());
        assertEquals("admin@mail.com", e.getUsuario());
        assertEquals("Acceso denegado", e.getMensaje());
        assertFalse(e.getCreadoEn().isBefore(antes));
    }

    @Test
    void listarSinFiltrosUsaLaConsultaGeneralYMapeaADominio() {
        when(jpa.findAllByOrderByCreadoEnDesc(any(Pageable.class))).thenReturn(List.of(entity(1L)));

        List<RegistroLog> logs = adapter.listar(null, null, 25);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findAllByOrderByCreadoEnDesc(pagina.capture());
        assertEquals(0, pagina.getValue().getPageNumber());
        assertEquals(25, pagina.getValue().getPageSize());
        RegistroLog log = logs.get(0);
        assertEquals(1L, log.getId());
        assertEquals("INFO", log.getNivel());
        assertEquals("NEGOCIO", log.getTipo());
        assertEquals("CitaService", log.getOrigen());
        assertEquals("ana@mail.com", log.getUsuario());
        assertEquals("Cita creada", log.getMensaje());
        verifyNoMoreInteractions(jpa);
    }

    @Test
    void listarSoloPorNivel() {
        when(jpa.findByNivelOrderByCreadoEnDesc(eq("ERROR"), any(Pageable.class))).thenReturn(List.of(entity(2L)));

        assertEquals(2L, adapter.listar("ERROR", null, 10).get(0).getId());
        verifyNoMoreInteractions(jpa);
    }

    @Test
    void listarSoloPorTipo() {
        when(jpa.findByTipoOrderByCreadoEnDesc(eq("AUDITORIA"), any(Pageable.class))).thenReturn(List.of());

        assertTrue(adapter.listar(null, "AUDITORIA", 10).isEmpty());
        verifyNoMoreInteractions(jpa);
    }

    @Test
    void listarPorNivelYTipo() {
        when(jpa.findByNivelAndTipoOrderByCreadoEnDesc(eq("INFO"), eq("NEGOCIO"), any(Pageable.class)))
                .thenReturn(List.of(entity(3L), entity(4L)));

        assertEquals(2, adapter.listar("INFO", "NEGOCIO", 10).size());
        verifyNoMoreInteractions(jpa);
    }

    @Test
    void eliminarAnterioresDevuelveLaCantidadBorrada() {
        LocalDateTime limite = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(jpa.eliminarAnterioresA(limite)).thenReturn(7);

        assertEquals(7, adapter.eliminarAnterioresA(limite));
    }
}
