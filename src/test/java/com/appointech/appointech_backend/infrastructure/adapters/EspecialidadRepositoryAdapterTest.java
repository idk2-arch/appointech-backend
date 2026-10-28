package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspecialidadRepositoryAdapterTest {

    @Mock
    private EspecialidadJpaRepository jpa;
    @InjectMocks
    private EspecialidadRepositoryAdapter adapter;

    private EspecialidadEntity entity(Long id, String nombre, boolean activo) {
        EspecialidadEntity e = new EspecialidadEntity();
        e.setId(id);
        e.setNombre(nombre);
        e.setDescripcion("desc " + nombre);
        e.setActivo(activo);
        return e;
    }

    @Test
    void listarTodasIncluyeInactivas() {
        when(jpa.findAll()).thenReturn(List.of(entity(1L, "Plomeria", true), entity(2L, "Gas", false)));

        List<Especialidad> lista = adapter.listarTodas();

        assertEquals(2, lista.size());
        assertFalse(lista.get(1).isActivo());
    }

    @Test
    void listarActivasUsaLaConsultaDeActivas() {
        when(jpa.findByActivoTrue()).thenReturn(List.of(entity(1L, "Plomeria", true)));

        List<Especialidad> lista = adapter.listarActivas();

        assertEquals(1, lista.size());
        assertEquals("desc Plomeria", lista.get(0).getDescripcion());
    }

    @Test
    void buscarPorIdsPorIdYPorNombre() {
        when(jpa.findAllById(List.of(1L, 2L))).thenReturn(List.of(entity(1L, "A", true), entity(2L, "B", true)));
        when(jpa.findById(1L)).thenReturn(Optional.of(entity(1L, "A", true)));
        when(jpa.findById(9L)).thenReturn(Optional.empty());
        when(jpa.findByNombre("A")).thenReturn(Optional.of(entity(1L, "A", true)));
        when(jpa.findByNombre("Z")).thenReturn(Optional.empty());

        assertEquals(List.of("A", "B"), adapter.buscarPorIds(List.of(1L, 2L)).stream().map(Especialidad::getNombre).toList());
        assertEquals(1L, adapter.buscarPorId(1L).orElseThrow().getId());
        assertTrue(adapter.buscarPorId(9L).isEmpty());
        assertEquals("A", adapter.buscarPorNombre("A").orElseThrow().getNombre());
        assertTrue(adapter.buscarPorNombre("Z").isEmpty());
    }

    @Test
    void guardarPersisteElEstadoActivoYDevuelveLoGuardado() {
        Especialidad e = new Especialidad();
        e.setNombre("Gas");
        e.setDescripcion("Instalaciones");
        e.setActivo(false);
        when(jpa.save(any(EspecialidadEntity.class))).thenAnswer(inv -> {
            EspecialidadEntity ent = inv.getArgument(0);
            ent.setId(6L);
            return ent;
        });

        Especialidad guardada = adapter.guardar(e);

        ArgumentCaptor<EspecialidadEntity> captor = ArgumentCaptor.forClass(EspecialidadEntity.class);
        verify(jpa).save(captor.capture());
        assertFalse(captor.getValue().isActivo());
        assertEquals("Instalaciones", captor.getValue().getDescripcion());
        assertEquals(6L, guardada.getId());
        assertFalse(guardada.isActivo());
    }

    @Test
    void contarTecnicosAsignadosDelegaEnLaConsultaNativa() {
        when(jpa.contarTecnicosPorEspecialidad(3L)).thenReturn(4L);

        assertEquals(4L, adapter.contarTecnicosAsignados(3L));
    }
}
