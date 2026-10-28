package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.infrastructure.entities.BloqueIndisponibilidadEntity;
import com.appointech.appointech_backend.infrastructure.entities.ConfiguracionFranjasEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ConfiguracionFranjasJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.GestionBloquesJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranjasYBloquesAdaptersTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 4, 1, 0, 0);
    private static final LocalDateTime HASTA = DESDE.plusDays(7);

    @Mock
    private ConfiguracionFranjasJpaRepository franjasJpa;
    @Mock
    private GestionBloquesJpaRepository bloquesJpa;

    private BloqueIndisponibilidadEntity bloque(Long id) {
        BloqueIndisponibilidadEntity e = new BloqueIndisponibilidadEntity();
        e.setId(id);
        e.setTecnicoId(2L);
        e.setInicio(DESDE.plusHours(8));
        e.setFin(DESDE.plusHours(10));
        e.setMotivo("Cita medica");
        return e;
    }

    // ---- ConfiguracionFranjasRepositoryAdapter ----

    @Test
    void obtenerActualMapeaLaFilaMasReciente() {
        ConfiguracionFranjasEntity e = new ConfiguracionFranjasEntity();
        e.setId(3L);
        e.setDuracionBloqueMinutos(60);
        e.setRadioMaximoKm(25);
        e.setHoraInicioJornada(LocalTime.of(8, 0));
        e.setHoraFinJornada(LocalTime.of(18, 0));
        e.setTelefonoWhatsappAdmin("+57 3001234567");
        when(franjasJpa.findFirstByOrderByIdDesc()).thenReturn(Optional.of(e));

        ConfiguracionFranjas c = new ConfiguracionFranjasRepositoryAdapter(franjasJpa).obtenerActual().orElseThrow();

        assertEquals(3L, c.getId());
        assertEquals(60, c.getDuracionBloqueMinutos());
        assertEquals(25, c.getRadioMaximoKm());
        assertEquals(LocalTime.of(8, 0), c.getHoraInicioJornada());
        assertEquals(LocalTime.of(18, 0), c.getHoraFinJornada());
        assertEquals("+57 3001234567", c.getTelefonoWhatsappAdmin());
    }

    @Test
    void obtenerActualSinConfiguracionDevuelveVacio() {
        when(franjasJpa.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        assertTrue(new ConfiguracionFranjasRepositoryAdapter(franjasJpa).obtenerActual().isEmpty());
    }

    @Test
    void guardarConfiguracionPersisteTodosLosCampos() {
        ConfiguracionFranjas c = new ConfiguracionFranjas(null, 90, 40, LocalTime.of(7, 30), LocalTime.of(17, 0));
        c.setTelefonoWhatsappAdmin("3001234567");
        when(franjasJpa.save(any(ConfiguracionFranjasEntity.class))).thenAnswer(inv -> {
            ConfiguracionFranjasEntity e = inv.getArgument(0);
            e.setId(8L);
            return e;
        });

        ConfiguracionFranjas guardada = new ConfiguracionFranjasRepositoryAdapter(franjasJpa).guardar(c);

        ArgumentCaptor<ConfiguracionFranjasEntity> captor = ArgumentCaptor.forClass(ConfiguracionFranjasEntity.class);
        verify(franjasJpa).save(captor.capture());
        assertEquals(90, captor.getValue().getDuracionBloqueMinutos());
        assertEquals(40, captor.getValue().getRadioMaximoKm());
        assertEquals("3001234567", captor.getValue().getTelefonoWhatsappAdmin());
        assertEquals(8L, guardada.getId());
        assertEquals(LocalTime.of(7, 30), guardada.getHoraInicioJornada());
    }

    // ---- GestionBloquesAdapter ----

    @Test
    void guardarBloqueDevuelveElBloqueConId() {
        BloqueIndisponibilidad nuevo = new BloqueIndisponibilidad(null, 2L, DESDE, DESDE.plusHours(1), "Almuerzo");
        when(bloquesJpa.save(any(BloqueIndisponibilidadEntity.class))).thenAnswer(inv -> {
            BloqueIndisponibilidadEntity e = inv.getArgument(0);
            e.setId(31L);
            return e;
        });

        BloqueIndisponibilidad guardado = new GestionBloquesAdapter(bloquesJpa).guardar(nuevo);

        assertEquals(31L, guardado.getId());
        assertEquals(2L, guardado.getTecnicoId());
        assertEquals("Almuerzo", guardado.getMotivo());
        assertEquals(DESDE.plusHours(1), guardado.getFin());
    }

    @Test
    void listarSinHastaUsaLaConsultaAbierta() {
        when(bloquesJpa.findByTecnicoIdAndFinGreaterThanOrderByInicioAsc(2L, DESDE)).thenReturn(List.of(bloque(1L)));

        List<BloqueIndisponibilidad> r = new GestionBloquesAdapter(bloquesJpa).listarDelTecnico(2L, DESDE, null);

        assertEquals(1, r.size());
        assertEquals("Cita medica", r.get(0).getMotivo());
        verify(bloquesJpa).findByTecnicoIdAndFinGreaterThanOrderByInicioAsc(2L, DESDE);
        verifyNoMoreInteractions(bloquesJpa);
    }

    @Test
    void listarConHastaUsaLaConsultaAcotada() {
        when(bloquesJpa.findByTecnicoIdAndFinGreaterThanAndInicioLessThanOrderByInicioAsc(2L, DESDE, HASTA))
                .thenReturn(List.of(bloque(1L), bloque(2L)));

        assertEquals(2, new GestionBloquesAdapter(bloquesJpa).listarDelTecnico(2L, DESDE, HASTA).size());
        verifyNoMoreInteractions(bloquesJpa);
    }

    @Test
    void eliminarDevuelveTrueSoloSiBorroAlgunaFila() {
        when(bloquesJpa.eliminarDelTecnico(1L, 2L)).thenReturn(1);
        when(bloquesJpa.eliminarDelTecnico(9L, 2L)).thenReturn(0);
        GestionBloquesAdapter adapter = new GestionBloquesAdapter(bloquesJpa);

        assertTrue(adapter.eliminarDelTecnico(1L, 2L));
        assertFalse(adapter.eliminarDelTecnico(9L, 2L));
    }
}
