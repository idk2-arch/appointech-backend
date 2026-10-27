package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatosDelTecnicoEnCitasYFranjasTest {

    private TecnicoRepositoryPort tecnicoRepo;
    private EspecialidadRepositoryPort especialidadRepo;
    private ObtenerDetalleCitaUseCaseImpl detalle;

    @BeforeEach
    void preparar() {
        tecnicoRepo = mock(TecnicoRepositoryPort.class);
        especialidadRepo = mock(EspecialidadRepositoryPort.class);
        detalle = new ObtenerDetalleCitaUseCaseImpl(tecnicoRepo, especialidadRepo);
    }

    @Test
    void franjaIncluyeNombreYApellidoDelTecnicoYResuelveCadaTecnicoUnaVez() {
        Tecnico t1 = tecnico(1L, "Ana", "Ruiz", "311");
        Tecnico t2 = tecnico(2L, "Luis", "Mora", "322");
        FiltrarTecnicosPorEspecialidadUseCase filtrar = mock(FiltrarTecnicosPorEspecialidadUseCase.class);
        when(filtrar.filtrar(5L)).thenReturn(List.of(t1, t2));
        CalculadoraDistanciaPort distancia = mock(CalculadoraDistanciaPort.class);
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(
                List.of(new Distancia(3.0, 5.0), new Distancia(8.0, 10.0)));
        CitaRepositoryPort citas = mock(CitaRepositoryPort.class);
        var sut = new CalcularFranjasDisponiblesUseCaseImpl(filtrar, citas,
                mock(BloqueIndisponibilidadRepositoryPort.class), mock(ConfiguracionFranjasRepositoryPort.class),
                distancia);

        List<FranjaDisponible> franjas = sut.calcular(5L, LocalDate.now().plusDays(1), 4.3365, -74.3638);

        assertFalse(franjas.isEmpty());
        // La más cercana (Ana) gana cada horario y trae su nombre.
        franjas.forEach(f -> {
            assertEquals(1L, f.getTecnicoId());
            assertEquals("Ana", f.getTecnicoNombre());
            assertEquals("Ruiz", f.getTecnicoApellido());
        });
        // Un solo cálculo de distancias para todos los técnicos (sin N+1 por franja).
        verify(distancia, times(1)).calcular(any(), any());
    }

    @Test
    void citaConfirmadaTraeNombreTelefonoDelTecnicoYNombreDeEspecialidad() {
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(tecnico(3L, "Ana", "Ruiz", "311")));
        Especialidad esp = new Especialidad();
        esp.setId(5L);
        esp.setNombre("Plomería");
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(esp));

        DetalleCita d = detalle.detallar(cita(3L, 5L, EstadoCita.CONFIRMADA));

        assertEquals("Ana", d.tecnicoNombre());
        assertEquals("Ruiz", d.tecnicoApellido());
        assertEquals("311", d.tecnicoTelefono());
        assertEquals("Plomería", d.especialidadNombre());
        verify(tecnicoRepo, times(1)).buscarPorId(3L);
        verify(especialidadRepo, times(1)).buscarPorId(5L);
    }

    @Test
    void solicitudSinTecnicoDejaLosDatosDelTecnicoEnNuloPeroTraeLaEspecialidad() {
        Especialidad esp = new Especialidad();
        esp.setId(5L);
        esp.setNombre("Plomería");
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(esp));

        DetalleCita d = detalle.detallar(cita(null, 5L, EstadoCita.PENDIENTE));

        assertNull(d.tecnicoNombre());
        assertNull(d.tecnicoApellido());
        assertNull(d.tecnicoTelefono());
        assertEquals("Plomería", d.especialidadNombre());
        verify(tecnicoRepo, never()).buscarPorId(any());
    }

    @Test
    void tecnicoOEspecialidadInexistentesDanNulosSinFallar() {
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.empty());
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.empty());

        DetalleCita d = detalle.detallar(cita(3L, 5L, EstadoCita.CONFIRMADA));

        assertNull(d.tecnicoNombre());
        assertNull(d.especialidadNombre());
    }

    private static Cita cita(Long tecnicoId, Long especialidadId, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(10L);
        c.setTecnicoId(tecnicoId);
        c.setEspecialidadId(especialidadId);
        c.setEstado(estado);
        return c;
    }

    private static Tecnico tecnico(Long id, String nombre, String apellido, String telefono) {
        Usuario u = new Usuario();
        u.setId(id + 100);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setTelefono(telefono);
        Tecnico t = new Tecnico();
        t.setId(id);
        t.setUsuario(u);
        t.setRadioCoberturaKm(20);
        t.setLatitudBase(4.3);
        t.setLongitudBase(-74.3);
        return t;
    }
}
