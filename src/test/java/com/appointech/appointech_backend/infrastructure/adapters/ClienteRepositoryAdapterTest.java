package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.infrastructure.entities.ClienteEntity;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.entities.TipoDocumentoEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ClienteJpaRepository;
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
class ClienteRepositoryAdapterTest {

    @Mock
    private ClienteJpaRepository jpa;
    @InjectMocks
    private ClienteRepositoryAdapter adapter;

    private ClienteEntity entity(Long id, boolean completo) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id + 100);
        u.setNombre("Luis");
        u.setApellido("Perez");
        u.setCorreo("luis@mail.com");
        u.setTelefono("3000000000");
        if (completo) {
            u.setTipoDocumento(TipoDocumentoEntity.CC);
            u.setRol(RolUsuarioEntity.CLIENTE);
        }
        u.setNumeroDocumento("999");
        u.setActivo(true);
        u.setCreadoEn(LocalDateTime.of(2026, 1, 2, 3, 4));
        ClienteEntity e = new ClienteEntity();
        e.setId(id);
        e.setDireccion("Calle 1");
        e.setLatitud(4.6);
        e.setLongitud(-74.1);
        e.setUsuario(u);
        return e;
    }

    @Test
    void guardarMapeaClienteYUsuarioEnAmbosSentidos() {
        Usuario u = new Usuario();
        u.setId(5L);
        u.setNombre("Luis");
        u.setContrasena("hash");
        u.setTipoDocumento(TipoDocumento.CC);
        u.setRol(RolUsuario.CLIENTE);
        u.setActivo(true);
        Cliente c = new Cliente();
        c.setDireccion("Calle 1");
        c.setLatitud(4.6);
        c.setLongitud(-74.1);
        c.setUsuario(u);
        when(jpa.save(any(ClienteEntity.class))).thenAnswer(inv -> {
            ClienteEntity e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });

        Cliente guardado = adapter.guardar(c);

        ArgumentCaptor<ClienteEntity> captor = ArgumentCaptor.forClass(ClienteEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals("hash", captor.getValue().getUsuario().getContrasena());
        assertEquals(RolUsuarioEntity.CLIENTE, captor.getValue().getUsuario().getRol());
        assertEquals(7L, guardado.getId());
        assertEquals("Calle 1", guardado.getDireccion());
        assertEquals(4.6, guardado.getLatitud());
        assertEquals(5L, guardado.getUsuario().getId());
        assertEquals(TipoDocumento.CC, guardado.getUsuario().getTipoDocumento());
        assertNull(guardado.getUsuario().getContrasena(), "el dominio no recibe el hash de vuelta");
    }

    @Test
    void guardarConUsuarioSinRolNiDocumentoNoFalla() {
        Cliente c = new Cliente();
        c.setUsuario(new Usuario());
        when(jpa.save(any(ClienteEntity.class))).thenAnswer(inv -> {
            ClienteEntity e = inv.getArgument(0);
            assertNull(e.getUsuario().getRol());
            assertNull(e.getUsuario().getTipoDocumento());
            return e;
        });

        Cliente guardado = adapter.guardar(c);

        assertNull(guardado.getUsuario().getRol());
    }

    @Test
    void buscarPorIdYPorUsuarioId() {
        when(jpa.findById(1L)).thenReturn(Optional.of(entity(1L, true)));
        when(jpa.findById(2L)).thenReturn(Optional.empty());
        when(jpa.findByUsuarioId(101L)).thenReturn(Optional.of(entity(1L, false)));

        Cliente c = adapter.buscarPorId(1L).orElseThrow();
        assertEquals("luis@mail.com", c.getUsuario().getCorreo());
        assertEquals(RolUsuario.CLIENTE, c.getUsuario().getRol());
        assertTrue(adapter.buscarPorId(2L).isEmpty());
        assertNull(adapter.buscarPorUsuarioId(101L).orElseThrow().getUsuario().getRol());
    }

    @Test
    void listarTodos() {
        when(jpa.findAll()).thenReturn(List.of(entity(1L, true), entity(2L, true)));
        assertEquals(List.of(1L, 2L), adapter.listarTodos().stream().map(Cliente::getId).toList());
    }
}
