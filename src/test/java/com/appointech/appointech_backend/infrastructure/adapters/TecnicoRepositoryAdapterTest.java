package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.entities.TecnicoEntity;
import com.appointech.appointech_backend.infrastructure.entities.TipoDocumentoEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.TecnicoJpaRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TecnicoRepositoryAdapterTest {

    @Mock
    private TecnicoJpaRepository tecnicoJpa;
    @Mock
    private EspecialidadJpaRepository especialidadJpa;
    @InjectMocks
    private TecnicoRepositoryAdapter adapter;

    private EspecialidadEntity espEntity(Long id, String nombre) {
        EspecialidadEntity e = new EspecialidadEntity();
        e.setId(id);
        e.setNombre(nombre);
        e.setDescripcion("desc " + nombre);
        return e;
    }

    private TecnicoEntity entity(Long id, boolean conEspecialidades) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id + 10);
        u.setNombre("Marta");
        u.setCorreo("marta@mail.com");
        u.setTipoDocumento(TipoDocumentoEntity.CC);
        u.setRol(RolUsuarioEntity.TECNICO);
        u.setActivo(true);
        TecnicoEntity t = new TecnicoEntity();
        t.setId(id);
        t.setUsuario(u);
        t.setDireccionBase("Base 1");
        t.setLatitudBase(4.0);
        t.setLongitudBase(-74.0);
        t.setRadioCoberturaKm(15);
        t.setEspecialidades(conEspecialidades ? List.of(espEntity(1L, "Plomeria")) : null);
        return t;
    }

    @Test
    void guardarResuelveEspecialidadesPorIdYMapeaUsuario() {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setContrasena("hash");
        u.setTipoDocumento(TipoDocumento.CE);
        u.setRol(RolUsuario.TECNICO);
        Especialidad esp = new Especialidad();
        esp.setId(1L);
        Tecnico t = new Tecnico();
        t.setUsuario(u);
        t.setRadioCoberturaKm(8);
        t.setEspecialidades(List.of(esp));
        when(especialidadJpa.findAllById(List.of(1L))).thenReturn(List.of(espEntity(1L, "Plomeria")));
        when(tecnicoJpa.save(any(TecnicoEntity.class))).thenAnswer(inv -> {
            TecnicoEntity e = inv.getArgument(0);
            e.setId(40L);
            return e;
        });

        Tecnico guardado = adapter.guardar(t);

        ArgumentCaptor<TecnicoEntity> captor = ArgumentCaptor.forClass(TecnicoEntity.class);
        verify(tecnicoJpa).save(captor.capture());
        assertEquals("hash", captor.getValue().getUsuario().getContrasena());
        assertEquals(TipoDocumentoEntity.CE, captor.getValue().getUsuario().getTipoDocumento());
        assertEquals(40L, guardado.getId());
        assertEquals(8, guardado.getRadioCoberturaKm());
        assertEquals("Plomeria", guardado.getEspecialidades().get(0).getNombre());
        assertEquals("desc Plomeria", guardado.getEspecialidades().get(0).getDescripcion());
        assertEquals(RolUsuario.TECNICO, guardado.getUsuario().getRol());
    }

    @Test
    void guardarSinEspecialidadesNoConsultaElRepositorioDeEspecialidades() {
        Tecnico t = new Tecnico();
        t.setUsuario(new Usuario());
        when(tecnicoJpa.save(any(TecnicoEntity.class))).thenAnswer(inv -> {
            TecnicoEntity e = inv.getArgument(0);
            assertTrue(e.getEspecialidades().isEmpty());
            assertNull(e.getUsuario().getRol());
            return e;
        });

        Tecnico guardado = adapter.guardar(t);

        verify(especialidadJpa, never()).findAllById(any());
        assertTrue(guardado.getEspecialidades().isEmpty());
        assertNull(guardado.getUsuario().getTipoDocumento());
    }

    @Test
    void buscarPorUsuarioIdYPorId() {
        when(tecnicoJpa.findByUsuarioId(11L)).thenReturn(Optional.of(entity(1L, true)));
        when(tecnicoJpa.findById(2L)).thenReturn(Optional.empty());
        when(tecnicoJpa.findById(1L)).thenReturn(Optional.of(entity(1L, false)));

        Tecnico t = adapter.buscarPorUsuarioId(11L).orElseThrow();
        assertEquals("Base 1", t.getDireccionBase());
        assertEquals(4.0, t.getLatitudBase());
        assertEquals(15, t.getRadioCoberturaKm());
        assertEquals(1, t.getEspecialidades().size());
        assertTrue(adapter.buscarPorId(2L).isEmpty());
        assertTrue(adapter.buscarPorId(1L).orElseThrow().getEspecialidades().isEmpty());
    }

    @Test
    void listadosYBloqueo() {
        when(tecnicoJpa.findAll()).thenReturn(List.of(entity(1L, true), entity(2L, true)));
        when(tecnicoJpa.buscarActivosPorEspecialidad(1L)).thenReturn(List.of(entity(1L, true)));
        when(tecnicoJpa.buscarPorIdBloqueando(1L)).thenReturn(Optional.of(entity(1L, true)));
        when(tecnicoJpa.buscarPorIdBloqueando(9L)).thenReturn(Optional.empty());

        assertEquals(2, adapter.listarTodos().size());
        assertEquals(1, adapter.buscarActivosPorEspecialidad(1L).size());
        assertEquals(1L, adapter.buscarPorIdBloqueando(1L).orElseThrow().getId());
        assertTrue(adapter.buscarPorIdBloqueando(9L).isEmpty());
    }
}
