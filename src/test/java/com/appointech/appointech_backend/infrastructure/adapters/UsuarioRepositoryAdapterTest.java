package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.entities.TipoDocumentoEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.UsuarioJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryAdapterTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 10, 0);

    @Mock
    private UsuarioJpaRepository jpa;
    @InjectMocks
    private UsuarioRepositoryAdapter adapter;

    private UsuarioEntity entity(Long id, boolean conDocumento) {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(id);
        e.setNombre("Ana");
        e.setApellido("Gomez");
        e.setCorreo("ana@mail.com");
        e.setContrasena("hash");
        e.setTelefono("3001112233");
        if (conDocumento) {
            e.setTipoDocumento(TipoDocumentoEntity.CE);
        }
        e.setNumeroDocumento("123");
        e.setRol(RolUsuarioEntity.TECNICO);
        e.setActivo(true);
        e.setCreadoEn(CREADO);
        return e;
    }

    @Test
    void guardarConvierteADominioYDevuelveLoPersistido() {
        Usuario u = new Usuario();
        u.setNombre("Ana");
        u.setApellido("Gomez");
        u.setCorreo("ana@mail.com");
        u.setContrasena("hash");
        u.setTelefono("3001112233");
        u.setTipoDocumento(TipoDocumento.PASAPORTE);
        u.setNumeroDocumento("123");
        u.setRol(RolUsuario.ADMINISTRADOR);
        u.setActivo(true);
        u.setCreadoEn(CREADO);
        when(jpa.save(any(UsuarioEntity.class))).thenAnswer(inv -> {
            UsuarioEntity e = inv.getArgument(0);
            e.setId(9L);
            return e;
        });

        Usuario guardado = adapter.guardar(u);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals(TipoDocumentoEntity.PASAPORTE, captor.getValue().getTipoDocumento());
        assertEquals(RolUsuarioEntity.ADMINISTRADOR, captor.getValue().getRol());
        assertEquals("hash", captor.getValue().getContrasena());
        assertEquals(9L, guardado.getId());
        assertEquals(RolUsuario.ADMINISTRADOR, guardado.getRol());
        assertEquals(TipoDocumento.PASAPORTE, guardado.getTipoDocumento());
        assertEquals(CREADO, guardado.getCreadoEn());
        assertTrue(guardado.isActivo());
    }

    @Test
    void guardarSinTipoDocumentoNoFallaYLoDejaNulo() {
        Usuario u = new Usuario();
        u.setRol(RolUsuario.CLIENTE);
        when(jpa.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = adapter.guardar(u);

        assertNull(guardado.getTipoDocumento());
        assertEquals(RolUsuario.CLIENTE, guardado.getRol());
    }

    @Test
    void buscarPorCorreoMapeaTodosLosCampos() {
        when(jpa.findByCorreo("ana@mail.com")).thenReturn(Optional.of(entity(1L, true)));

        Usuario u = adapter.buscarPorCorreo("ana@mail.com").orElseThrow();

        assertEquals(1L, u.getId());
        assertEquals("Ana", u.getNombre());
        assertEquals("Gomez", u.getApellido());
        assertEquals("3001112233", u.getTelefono());
        assertEquals(TipoDocumento.CE, u.getTipoDocumento());
        assertEquals("123", u.getNumeroDocumento());
        assertEquals(RolUsuario.TECNICO, u.getRol());
    }

    @Test
    void buscarPorCorreoInexistenteDevuelveVacio() {
        when(jpa.findByCorreo("x@x.com")).thenReturn(Optional.empty());
        assertTrue(adapter.buscarPorCorreo("x@x.com").isEmpty());
    }

    @Test
    void buscarPorNumeroDocumentoYPorId() {
        when(jpa.findByNumeroDocumento("123")).thenReturn(Optional.of(entity(2L, false)));
        when(jpa.findById(2L)).thenReturn(Optional.of(entity(2L, false)));
        when(jpa.findById(3L)).thenReturn(Optional.empty());

        assertNull(adapter.buscarPorNumeroDocumento("123").orElseThrow().getTipoDocumento());
        assertEquals(2L, adapter.buscarPorId(2L).orElseThrow().getId());
        assertTrue(adapter.buscarPorId(3L).isEmpty());
    }

    @Test
    void listarTodosConvierteCadaEntidad() {
        when(jpa.findAll()).thenReturn(List.of(entity(1L, true), entity(2L, false)));

        List<Usuario> lista = adapter.listarTodos();

        assertEquals(List.of(1L, 2L), lista.stream().map(Usuario::getId).toList());
    }
}
