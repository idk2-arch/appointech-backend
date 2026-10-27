package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.DocumentoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.DatosActualizacionTecnico;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GestionTecnicoAdminUseCasesTest {

    private static final Double LAT = 4.3365;
    private static final Double LON = -74.3638;

    private TecnicoRepositoryPort tecnicoRepo;
    private UsuarioRepositoryPort usuarioRepo;
    private EspecialidadRepositoryPort especialidadRepo;
    private PasswordEncoderPort passwordEncoder;

    private AsignarEspecialidadesTecnicoUseCase asignar;
    private ActualizarTecnicoUseCaseImpl actualizar;
    private CrearTecnicoConEspecialidadesUseCaseImpl crear;

    @BeforeEach
    void preparar() {
        tecnicoRepo = mock(TecnicoRepositoryPort.class);
        usuarioRepo = mock(UsuarioRepositoryPort.class);
        especialidadRepo = mock(EspecialidadRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoderPort.class);

        // Casos de uso reales reutilizados (no se duplica lógica), con puertos simulados.
        asignar = new AsignarEspecialidadesTecnicoUseCaseImpl(tecnicoRepo, especialidadRepo);
        RegistrarTecnicoUseCase registrar = new RegistrarTecnicoUseCaseImpl(usuarioRepo, tecnicoRepo, passwordEncoder);
        crear = new CrearTecnicoConEspecialidadesUseCaseImpl(registrar, asignar);
        actualizar = new ActualizarTecnicoUseCaseImpl(tecnicoRepo, usuarioRepo, asignar);

        when(usuarioRepo.guardar(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            if (u.getId() == null) u.setId(7L);
            return u;
        });
        when(tecnicoRepo.guardar(any(Tecnico.class))).thenAnswer(i -> {
            Tecnico t = i.getArgument(0);
            if (t.getId() == null) t.setId(3L);
            if (t.getEspecialidades() == null) t.setEspecialidades(List.of());
            return t;
        });
        when(passwordEncoder.encriptar("clave12345")).thenReturn("hash");
    }

    // ---------- crear ----------

    @Test
    void creaTecnicoConEspecialidades() {
        Especialidad e1 = especialidad(1L), e2 = especialidad(2L);
        when(usuarioRepo.buscarPorCorreo("t@x.com")).thenReturn(Optional.empty());
        when(especialidadRepo.buscarPorIds(List.of(1L, 2L))).thenReturn(List.of(e1, e2));
        when(tecnicoRepo.buscarPorId(3L)).thenAnswer(i -> Optional.of(tecnicoGuardado()));

        Tecnico resultado = crear.crear("Ana", "Ruiz", "t@x.com", "clave12345", "300", TipoDocumento.CC, "123",
                "Calle 1", 15, LAT, LON, List.of(1L, 2L));

        assertEquals(2, resultado.getEspecialidades().size());
        assertEquals(RolUsuario.TECNICO, resultado.getUsuario().getRol());
        assertEquals("hash", resultado.getUsuario().getContrasena());
        assertEquals(15, resultado.getRadioCoberturaKm());
        assertEquals(LAT, resultado.getLatitudBase());
    }

    @Test
    void creaTecnicoSinEspecialidadesNoLlamaAsignar() {
        when(usuarioRepo.buscarPorCorreo("t@x.com")).thenReturn(Optional.empty());
        AsignarEspecialidadesTecnicoUseCase asignarMock = mock(AsignarEspecialidadesTecnicoUseCase.class);
        RegistrarTecnicoUseCase registrar = new RegistrarTecnicoUseCaseImpl(usuarioRepo, tecnicoRepo, passwordEncoder);
        var sut = new CrearTecnicoConEspecialidadesUseCaseImpl(registrar, asignarMock);

        Tecnico resultado = sut.crear("Ana", "Ruiz", "t@x.com", "clave12345", null, TipoDocumento.CC, null,
                "Calle 1", null, null, null, null);

        verifyNoInteractions(asignarMock);
        assertEquals(10, resultado.getRadioCoberturaKm());
    }

    @Test
    void creaTecnicoConCorreoDuplicadoLanza409() {
        when(usuarioRepo.buscarPorCorreo("t@x.com")).thenReturn(Optional.of(new Usuario()));

        assertThrows(CorreoYaRegistradoException.class, () -> crear.crear("Ana", "Ruiz", "t@x.com", "clave12345",
                null, TipoDocumento.CC, null, "Calle 1", null, null, null, List.of(1L)));
        verify(usuarioRepo, never()).guardar(any());
    }

    @Test
    void creaTecnicoConCoordenadasIncompletasLanzaCoordenadasInvalidas() {
        assertThrows(CoordenadasInvalidasException.class, () -> crear.crear("Ana", "Ruiz", "t@x.com", "clave12345",
                null, TipoDocumento.CC, null, "Calle 1", null, LAT, null, null));
        verify(usuarioRepo, never()).guardar(any());
    }

    @Test
    void creaTecnicoConEspecialidadInexistenteLanzaExcepcion() {
        when(usuarioRepo.buscarPorCorreo("t@x.com")).thenReturn(Optional.empty());
        when(especialidadRepo.buscarPorIds(List.of(1L, 99L))).thenReturn(List.of(especialidad(1L)));
        when(tecnicoRepo.buscarPorId(3L)).thenAnswer(i -> Optional.of(tecnicoGuardado()));

        assertThrows(EspecialidadNoEncontradaException.class, () -> crear.crear("Ana", "Ruiz", "t@x.com",
                "clave12345", null, TipoDocumento.CC, null, "Calle 1", null, null, null, List.of(1L, 99L)));
    }

    // ---------- actualizar ----------

    @Test
    void editaTodosLosDatosDelTecnico() {
        Tecnico existente = tecnicoExistente();
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(existente));
        when(usuarioRepo.buscarPorCorreo("nuevo@x.com")).thenReturn(Optional.empty());
        when(especialidadRepo.buscarPorIds(List.of(5L))).thenReturn(List.of(especialidad(5L)));

        Tecnico resultado = actualizar.actualizar(3L, new DatosActualizacionTecnico("Luis", "Mora", "nuevo@x.com",
                "311", TipoDocumento.CE, "999", "Carrera 9", 25, 4.5, -74.1, List.of(5L)));

        Usuario u = resultado.getUsuario();
        assertEquals("Luis", u.getNombre());
        assertEquals("Mora", u.getApellido());
        assertEquals("nuevo@x.com", u.getCorreo());
        assertEquals("311", u.getTelefono());
        assertEquals(TipoDocumento.CE, u.getTipoDocumento());
        assertEquals("999", u.getNumeroDocumento());
        assertEquals("Carrera 9", resultado.getDireccionBase());
        assertEquals(25, resultado.getRadioCoberturaKm());
        assertEquals(4.5, resultado.getLatitudBase());
        assertEquals(-74.1, resultado.getLongitudBase());
        assertEquals(List.of(5L), resultado.getEspecialidades().stream().map(Especialidad::getId).toList());
        verify(usuarioRepo).guardar(u);
    }

    @Test
    void camposNulosNoModificanNada() {
        Tecnico existente = tecnicoExistente();
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(existente));

        Tecnico resultado = actualizar.actualizar(3L,
                new DatosActualizacionTecnico(null, null, null, null, null, null, null, null, null, null, null));

        assertEquals("Ana", resultado.getUsuario().getNombre());
        assertEquals("Calle 1", resultado.getDireccionBase());
        assertEquals(LAT, resultado.getLatitudBase());
        verify(especialidadRepo, never()).buscarPorIds(anyList());
    }

    @Test
    void permiteConservarElMismoCorreoDelPropioTecnico() {
        Tecnico existente = tecnicoExistente();
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(existente));
        when(usuarioRepo.buscarPorCorreo("ana@x.com")).thenReturn(Optional.of(existente.getUsuario()));

        Tecnico resultado = actualizar.actualizar(3L, datosSoloCorreo("ana@x.com"));

        assertEquals("ana@x.com", resultado.getUsuario().getCorreo());
    }

    @Test
    void correoDeOtroUsuarioLanza409() {
        Tecnico existente = tecnicoExistente();
        Usuario otro = new Usuario();
        otro.setId(99L);
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(existente));
        when(usuarioRepo.buscarPorCorreo("otro@x.com")).thenReturn(Optional.of(otro));

        assertThrows(CorreoYaRegistradoException.class, () -> actualizar.actualizar(3L, datosSoloCorreo("otro@x.com")));
        verify(usuarioRepo, never()).guardar(any());
        verify(tecnicoRepo, never()).guardar(any());
    }

    @Test
    void tecnicoInexistenteLanza404() {
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class, () -> actualizar.actualizar(3L, datosSoloCorreo("a@x.com")));
    }

    @Test
    void coordenadasIncompletasOInvalidasLanzan400SinGuardar() {
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(tecnicoExistente()));

        assertThrows(CoordenadasInvalidasException.class, () -> actualizar.actualizar(3L,
                new DatosActualizacionTecnico(null, null, null, null, null, null, null, null, 4.5, null, null)));
        assertThrows(CoordenadasInvalidasException.class, () -> actualizar.actualizar(3L,
                new DatosActualizacionTecnico(null, null, null, null, null, null, null, null, -74.1, 4.5, null)));
        verify(usuarioRepo, never()).guardar(any());
        verify(tecnicoRepo, never()).guardar(any());
    }

    @Test
    void especialidadInexistenteLanzaExcepcion() {
        when(tecnicoRepo.buscarPorId(3L)).thenAnswer(i -> Optional.of(tecnicoExistente()));
        when(especialidadRepo.buscarPorIds(List.of(1L, 99L))).thenReturn(List.of(especialidad(1L)));

        var ex = assertThrows(EspecialidadNoEncontradaException.class, () -> actualizar.actualizar(3L,
                new DatosActualizacionTecnico(null, null, null, null, null, null, null, null, null, null,
                        List.of(1L, 99L))));
        assertSame(EspecialidadNoEncontradaException.class, ex.getClass());
    }

    @Test
    void listaVaciaDeEspecialidadesLasQuitaTodas() {
        Tecnico existente = tecnicoExistente();
        existente.setEspecialidades(List.of(especialidad(1L)));
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(existente));
        when(especialidadRepo.buscarPorIds(List.of())).thenReturn(List.of());

        Tecnico resultado = actualizar.actualizar(3L,
                new DatosActualizacionTecnico(null, null, null, null, null, null, null, null, null, null, List.of()));

        assertEquals(0, resultado.getEspecialidades().size());
    }

    @Test
    void documentoDeOtroUsuarioLanza409AlEditar() {
        Usuario otro = new Usuario();
        otro.setId(99L);
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(tecnicoExistente()));
        when(usuarioRepo.buscarPorNumeroDocumento("555")).thenReturn(Optional.of(otro));

        assertThrows(DocumentoYaRegistradoException.class, () -> actualizar.actualizar(3L,
                new DatosActualizacionTecnico(null, null, null, null, null, "555", null, null, null, null, null)));
        verify(usuarioRepo, never()).guardar(any());
    }

    @Test
    void documentoDuplicadoLanza409AlCrear() {
        when(usuarioRepo.buscarPorCorreo("t@x.com")).thenReturn(Optional.empty());
        when(usuarioRepo.buscarPorNumeroDocumento("123")).thenReturn(Optional.of(new Usuario()));

        assertThrows(DocumentoYaRegistradoException.class, () -> crear.crear("Ana", "Ruiz", "t@x.com", "clave12345",
                null, TipoDocumento.CC, "123", "Calle 1", null, null, null, null));
        verify(usuarioRepo, never()).guardar(any());
    }

    // ---------- helpers ----------

    private static DatosActualizacionTecnico datosSoloCorreo(String correo) {
        return new DatosActualizacionTecnico(null, null, correo, null, null, null, null, null, null, null, null);
    }

    private static Especialidad especialidad(Long id) {
        Especialidad e = new Especialidad();
        e.setId(id);
        e.setNombre("Esp " + id);
        return e;
    }

    private static Tecnico tecnicoExistente() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setNombre("Ana");
        u.setApellido("Ruiz");
        u.setCorreo("ana@x.com");
        u.setRol(RolUsuario.TECNICO);
        u.setActivo(true);
        Tecnico t = new Tecnico();
        t.setId(3L);
        t.setUsuario(u);
        t.setDireccionBase("Calle 1");
        t.setRadioCoberturaKm(10);
        t.setLatitudBase(LAT);
        t.setLongitudBase(LON);
        t.setEspecialidades(List.of());
        return t;
    }

    private Tecnico tecnicoGuardado() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setRol(RolUsuario.TECNICO);
        u.setContrasena("hash");
        Tecnico t = new Tecnico();
        t.setId(3L);
        t.setUsuario(u);
        t.setRadioCoberturaKm(15);
        t.setLatitudBase(LAT);
        t.setLongitudBase(LON);
        t.setEspecialidades(List.of());
        return t;
    }
}
