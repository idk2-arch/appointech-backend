package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.infrastructure.entities.BloqueIndisponibilidadEntity;
import com.appointech.appointech_backend.infrastructure.entities.ClienteEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.BloqueIndisponibilidadJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.ClienteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultasYBloquesLecturaAdaptersTest {

    @Mock
    private ClienteJpaRepository clientesJpa;
    @Mock
    private BloqueIndisponibilidadJpaRepository bloquesJpa;

    private ClienteEntity cliente(Long id, String nombre) {
        UsuarioEntity u = new UsuarioEntity();
        u.setNombre(nombre);
        u.setApellido("Apellido " + nombre);
        u.setTelefono("300" + id);
        ClienteEntity c = new ClienteEntity();
        c.setId(id);
        c.setUsuario(u);
        return c;
    }

    @Test
    void resumenesPorIdIndexaCadaClientePorSuId() {
        when(clientesJpa.findAllById(Set.of(1L, 2L))).thenReturn(List.of(cliente(1L, "Ana"), cliente(2L, "Luis")));

        Map<Long, ClienteResumen> r = new ConsultaClientesAdapter(clientesJpa).resumenesPorId(Set.of(1L, 2L));

        assertEquals(2, r.size());
        assertEquals(new ClienteResumen("Ana", "Apellido Ana", "3001"), r.get(1L));
        assertEquals("Luis", r.get(2L).nombre());
    }

    @Test
    void resumenesPorIdIgnoraIdsInexistentes() {
        when(clientesJpa.findAllById(List.of(1L, 99L))).thenReturn(List.of(cliente(1L, "Ana")));

        Map<Long, ClienteResumen> r = new ConsultaClientesAdapter(clientesJpa).resumenesPorId(List.of(1L, 99L));

        assertTrue(r.containsKey(1L));
        assertFalse(r.containsKey(99L));
    }

    @Test
    void resumenesPorIdVacioNoConsultaLaBaseDeDatos() {
        Map<Long, ClienteResumen> r = new ConsultaClientesAdapter(clientesJpa).resumenesPorId(List.of());

        assertTrue(r.isEmpty());
        verifyNoInteractions(clientesJpa);
    }

    @Test
    void buscarBloquesDelTecnicoUsaLosLimitesCorrectosDeSolapamiento() {
        LocalDateTime inicio = LocalDateTime.of(2026, 5, 1, 8, 0);
        LocalDateTime fin = inicio.plusHours(4);
        BloqueIndisponibilidadEntity e = new BloqueIndisponibilidadEntity();
        e.setId(4L);
        e.setTecnicoId(2L);
        e.setInicio(inicio.plusHours(1));
        e.setFin(inicio.plusHours(2));
        e.setMotivo("Reunion");
        // el repositorio recibe (tecnico, inicioMenorQue=fin, finMayorQue=inicio)
        when(bloquesJpa.findByTecnicoIdAndInicioLessThanAndFinGreaterThan(2L, fin, inicio)).thenReturn(List.of(e));

        List<BloqueIndisponibilidad> r = new BloqueIndisponibilidadRepositoryAdapter(bloquesJpa)
                .buscarDelTecnicoEntre(2L, inicio, fin);

        assertEquals(1, r.size());
        assertEquals(4L, r.get(0).getId());
        assertEquals("Reunion", r.get(0).getMotivo());
        assertEquals(inicio.plusHours(2), r.get(0).getFin());
    }
}
