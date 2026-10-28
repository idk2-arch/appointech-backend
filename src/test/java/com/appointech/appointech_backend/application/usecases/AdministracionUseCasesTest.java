package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNombreDuplicadoException;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EspecialidadAdminInfo;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AdministracionUseCasesTest {

    @Mock
    private EspecialidadRepositoryPort especialidades;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TecnicoRepositoryPort tecnicos;

    private Especialidad esp(Long id, String nombre, boolean activo) {
        Especialidad e = new Especialidad();
        e.setId(id);
        e.setNombre(nombre);
        e.setActivo(activo);
        return e;
    }

    // ---- CrearEspecialidad ----

    @Test
    void crearEspecialidadNuevaQuedaActiva() {
        when(especialidades.buscarPorNombre("Gas")).thenReturn(Optional.empty());
        when(especialidades.guardar(any(Especialidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Especialidad r = new CrearEspecialidadUseCaseImpl(especialidades).crear("Gas", "Instalaciones");

        assertEquals("Gas", r.getNombre());
        assertEquals("Instalaciones", r.getDescripcion());
        assertTrue(r.isActivo());
    }

    @Test
    void crearEspecialidadConNombreExistenteFalla() {
        when(especialidades.buscarPorNombre("Gas")).thenReturn(Optional.of(esp(1L, "Gas", true)));

        assertThrows(EspecialidadNombreDuplicadoException.class,
                () -> new CrearEspecialidadUseCaseImpl(especialidades).crear("Gas", "d"));

        verify(especialidades, never()).guardar(any());
    }

    // ---- ActualizarEspecialidad ----

    @Test
    void actualizarEspecialidadCambiaNombreYDescripcion() {
        Especialidad existente = esp(1L, "Gas", true);
        when(especialidades.buscarPorId(1L)).thenReturn(Optional.of(existente));
        when(especialidades.buscarPorNombre("Gas natural")).thenReturn(Optional.empty());
        when(especialidades.guardar(existente)).thenReturn(existente);

        Especialidad r = new ActualizarEspecialidadUseCaseImpl(especialidades).actualizar(1L, "Gas natural", "nueva");

        assertEquals("Gas natural", r.getNombre());
        assertEquals("nueva", r.getDescripcion());
    }

    @Test
    void actualizarConservandoSuPropioNombreEsValido() {
        Especialidad existente = esp(1L, "Gas", true);
        when(especialidades.buscarPorId(1L)).thenReturn(Optional.of(existente));
        when(especialidades.buscarPorNombre("Gas")).thenReturn(Optional.of(existente));
        when(especialidades.guardar(existente)).thenReturn(existente);

        assertEquals("otra", new ActualizarEspecialidadUseCaseImpl(especialidades).actualizar(1L, "Gas", "otra").getDescripcion());
    }

    @Test
    void actualizarConNombreDeOtraEspecialidadFalla() {
        when(especialidades.buscarPorId(1L)).thenReturn(Optional.of(esp(1L, "Gas", true)));
        when(especialidades.buscarPorNombre("Plomeria")).thenReturn(Optional.of(esp(2L, "Plomeria", true)));

        assertThrows(EspecialidadNombreDuplicadoException.class,
                () -> new ActualizarEspecialidadUseCaseImpl(especialidades).actualizar(1L, "Plomeria", "d"));

        verify(especialidades, never()).guardar(any());
    }

    @Test
    void actualizarEspecialidadInexistenteFalla() {
        when(especialidades.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThrows(EspecialidadNoEncontradaException.class,
                () -> new ActualizarEspecialidadUseCaseImpl(especialidades).actualizar(9L, "X", "d"));
    }

    // ---- CambiarEstadoEspecialidad ----

    @Test
    void cambiarEstadoDeEspecialidadPersisteElNuevoValor() {
        Especialidad existente = esp(1L, "Gas", true);
        when(especialidades.buscarPorId(1L)).thenReturn(Optional.of(existente));
        when(especialidades.guardar(existente)).thenReturn(existente);

        assertFalse(new CambiarEstadoEspecialidadUseCaseImpl(especialidades).cambiarEstado(1L, false).isActivo());
    }

    @Test
    void cambiarEstadoDeEspecialidadInexistenteFalla() {
        when(especialidades.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThrows(EspecialidadNoEncontradaException.class,
                () -> new CambiarEstadoEspecialidadUseCaseImpl(especialidades).cambiarEstado(9L, true));
    }

    // ---- ListarEspecialidadesAdmin ----

    @Test
    void listarAdminIncluyeElConteoDeTecnicosDeCadaEspecialidad() {
        when(especialidades.listarTodas()).thenReturn(List.of(esp(1L, "Gas", true), esp(2L, "Plomeria", false)));
        when(especialidades.contarTecnicosAsignados(1L)).thenReturn(4L);
        when(especialidades.contarTecnicosAsignados(2L)).thenReturn(0L);

        List<EspecialidadAdminInfo> r = new ListarEspecialidadesAdminUseCaseImpl(especialidades).listar();

        assertEquals(2, r.size());
        assertEquals(4L, r.get(0).getTecnicosAsignados());
        assertEquals("Plomeria", r.get(1).getEspecialidad().getNombre());
        assertEquals(0L, r.get(1).getTecnicosAsignados());
    }

    // ---- ActualizarUsuario / CambiarEstadoUsuario ----

    @Test
    void actualizarUsuarioCambiaNombreApellidoYTelefono() {
        Usuario u = new Usuario();
        u.setId(3L);
        when(usuarios.buscarPorId(3L)).thenReturn(Optional.of(u));
        when(usuarios.guardar(u)).thenReturn(u);

        Usuario r = new ActualizarUsuarioUseCaseImpl(usuarios).actualizar(3L, "Nuevo", "Apellido", "311");

        assertEquals("Nuevo", r.getNombre());
        assertEquals("Apellido", r.getApellido());
        assertEquals("311", r.getTelefono());
    }

    @Test
    void actualizarUsuarioInexistenteFalla() {
        when(usuarios.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> new ActualizarUsuarioUseCaseImpl(usuarios).actualizar(9L, "a", "b", "c"));

        verify(usuarios, never()).guardar(any());
    }

    @Test
    void cambiarEstadoDeUsuarioDesactivaYReactiva() {
        Usuario u = new Usuario();
        u.setActivo(true);
        when(usuarios.buscarPorId(3L)).thenReturn(Optional.of(u));
        when(usuarios.guardar(u)).thenReturn(u);
        CambiarEstadoUsuarioUseCaseImpl caso = new CambiarEstadoUsuarioUseCaseImpl(usuarios);

        assertFalse(caso.cambiarEstado(3L, false).isActivo());
        assertTrue(caso.cambiarEstado(3L, true).isActivo());
    }

    @Test
    void cambiarEstadoDeUsuarioInexistenteFalla() {
        when(usuarios.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> new CambiarEstadoUsuarioUseCaseImpl(usuarios).cambiarEstado(9L, true));
    }

    // ---- ActualizarUbicacionTecnico ----

    @Test
    void actualizarUbicacionGuardaLaNuevaBase() {
        Tecnico t = new Tecnico();
        when(tecnicos.buscarPorId(2L)).thenReturn(Optional.of(t));
        when(tecnicos.guardar(t)).thenReturn(t);

        Tecnico r = new ActualizarUbicacionTecnicoUseCaseImpl(tecnicos).actualizar(2L, 4.65, -74.05);

        assertEquals(4.65, r.getLatitudBase());
        assertEquals(-74.05, r.getLongitudBase());
    }

    @Test
    void actualizarUbicacionFueraDeColombiaSeRechazaSinConsultarAlTecnico() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> new ActualizarUbicacionTecnicoUseCaseImpl(tecnicos).actualizar(2L, 40.0, -74.0));

        verify(tecnicos, never()).buscarPorId(any());
    }

    @Test
    void actualizarUbicacionDeTecnicoInexistenteFalla() {
        when(tecnicos.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class,
                () -> new ActualizarUbicacionTecnicoUseCaseImpl(tecnicos).actualizar(9L, 4.6, -74.0));

        verify(tecnicos, never()).guardar(any());
    }
}
