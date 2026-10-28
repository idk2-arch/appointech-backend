package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.EliminadorObjetoFoto;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.ImagenInvalidaException;
import com.appointech.appointech_backend.domain.exception.LimiteFotosCitaException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import com.appointech.appointech_backend.infrastructure.adapters.AlmacenamientoFotosEnMemoria;
import com.appointech.appointech_backend.infrastructure.adapters.CitaFotoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FotoCitaUseCasesTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    private static final String IMAGEN = "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG);
    private static final String DUENO = "ana@mail.com";
    private static final String OTRO_CLIENTE = "bea@mail.com";
    private static final String TECNICO_ASIGNADO = "luis@mail.com";
    private static final String OTRO_TECNICO = "omar@mail.com";
    private static final String ADMIN = "admin@mail.com";
    private static final Long CITA_ID = 20L;

    private AlmacenamientoFotosEnMemoria almacenamiento;
    private CitaFotoRepositoryEnMemoria fotos;
    private RegistroLogPort registroLog;
    private CitaRepositoryPort citas;
    private UsuarioRepositoryPort usuarios;
    private ClienteRepositoryPort clientes;
    private TecnicoRepositoryPort tecnicos;
    private Cita cita;

    private AgregarFotoCitaUseCaseImpl agregar;
    private EliminarFotoCitaUseCaseImpl eliminar;
    private ObtenerFotoCitaUseCaseImpl obtener;

    @BeforeEach
    void preparar() {
        almacenamiento = new AlmacenamientoFotosEnMemoria();
        fotos = new CitaFotoRepositoryEnMemoria();
        registroLog = mock(RegistroLogPort.class);
        citas = mock(CitaRepositoryPort.class);
        usuarios = mock(UsuarioRepositoryPort.class);
        clientes = mock(ClienteRepositoryPort.class);
        tecnicos = mock(TecnicoRepositoryPort.class);

        cita = new Cita();
        cita.setId(CITA_ID);
        cita.setClienteId(1L);
        cita.setTecnicoId(5L);
        cita.setEstado(EstadoCita.CONFIRMADA);
        when(citas.buscarPorId(CITA_ID)).thenReturn(Optional.of(cita));

        usuario(DUENO, 7L, RolUsuario.CLIENTE);
        usuario(OTRO_CLIENTE, 8L, RolUsuario.CLIENTE);
        usuario(TECNICO_ASIGNADO, 9L, RolUsuario.TECNICO);
        usuario(OTRO_TECNICO, 10L, RolUsuario.TECNICO);
        usuario(ADMIN, 1L, RolUsuario.ADMINISTRADOR);
        cliente(7L, 1L);
        cliente(8L, 2L);
        tecnico(9L, 5L);
        tecnico(10L, 6L);

        VerificadorAccesoCita verificador = new VerificadorAccesoCita(citas, usuarios, clientes, tecnicos);
        reconstruir(almacenamiento, fotos, verificador);
    }

    private void reconstruir(AlmacenamientoFotosPort puertoAlmacenamiento, CitaFotoRepositoryPort puertoFotos,
                             VerificadorAccesoCita verificador) {
        EliminadorObjetoFoto eliminador = new EliminadorObjetoFoto(puertoAlmacenamiento, registroLog);
        agregar = new AgregarFotoCitaUseCaseImpl(verificador, puertoFotos, puertoAlmacenamiento, eliminador);
        eliminar = new EliminarFotoCitaUseCaseImpl(verificador, puertoFotos, eliminador);
        obtener = new ObtenerFotoCitaUseCaseImpl(verificador, puertoFotos, puertoAlmacenamiento);
    }

    private void usuario(String correo, Long id, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo(correo);
        u.setRol(rol);
        when(usuarios.buscarPorCorreo(correo)).thenReturn(Optional.of(u));
    }

    private void cliente(Long usuarioId, Long clienteId) {
        Cliente c = new Cliente();
        c.setId(clienteId);
        when(clientes.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.of(c));
    }

    private void tecnico(Long usuarioId, Long tecnicoId) {
        Tecnico t = new Tecnico();
        t.setId(tecnicoId);
        when(tecnicos.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.of(t));
    }

    // ---------- agregar ----------

    @Test
    void elDuenoAdjuntaUnaFotoConClaveConPrefijoDeCitaYUuid() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);

        assertEquals(1L, foto.id());
        assertEquals(CITA_ID, foto.citaId());
        assertTrue(foto.clave().matches("citas/20/[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"));
        assertEquals("image/png", almacenamiento.obtener(foto.clave()).orElseThrow().tipoContenido());
        assertEquals(1, fotos.contarPorCita(CITA_ID));
    }

    @Test
    void sePuedenAdjuntarTresPeroNoUnaCuarta() {
        agregar.agregar(DUENO, CITA_ID, IMAGEN);
        agregar.agregar(DUENO, CITA_ID, IMAGEN);
        agregar.agregar(DUENO, CITA_ID, IMAGEN);

        assertThrows(LimiteFotosCitaException.class, () -> agregar.agregar(DUENO, CITA_ID, IMAGEN));

        assertEquals(3, fotos.total());
        assertEquals(3, almacenamiento.claves().size());
    }

    @Test
    void soloSePuedeAdjuntarConLaCitaPendienteOConfirmada() {
        cita.setEstado(EstadoCita.PENDIENTE);
        assertEquals(CITA_ID, agregar.agregar(DUENO, CITA_ID, IMAGEN).citaId());

        for (EstadoCita estado : new EstadoCita[]{EstadoCita.EN_CAMINO, EstadoCita.EN_PROCESO,
                EstadoCita.FINALIZADA, EstadoCita.CANCELADA}) {
            cita.setEstado(estado);
            assertThrows(EstadoCitaInvalidoException.class, () -> agregar.agregar(DUENO, CITA_ID, IMAGEN));
        }
        assertEquals(1, fotos.total());
    }

    @Test
    void nadieMasQueElClienteDuenoPuedeAdjuntar() {
        assertThrows(AccesoCitaDenegadoException.class, () -> agregar.agregar(OTRO_CLIENTE, CITA_ID, IMAGEN));
        assertThrows(AccesoCitaDenegadoException.class, () -> agregar.agregar(TECNICO_ASIGNADO, CITA_ID, IMAGEN));
        assertThrows(AccesoCitaDenegadoException.class, () -> agregar.agregar(ADMIN, CITA_ID, IMAGEN));

        assertTrue(almacenamiento.claves().isEmpty());
        assertEquals(0, fotos.total());
    }

    @Test
    void unaCitaInexistenteOUnUsuarioInexistenteFallanSinGuardarNada() {
        when(citas.buscarPorId(99L)).thenReturn(Optional.empty());
        when(usuarios.buscarPorCorreo("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class, () -> agregar.agregar(DUENO, 99L, IMAGEN));
        assertThrows(UsuarioNoEncontradoException.class, () -> agregar.agregar("nadie@mail.com", CITA_ID, IMAGEN));
        assertTrue(almacenamiento.claves().isEmpty());
    }

    @Test
    void unaImagenInvalidaNoTocaElAlmacenamientoNiLaBaseDeDatos() {
        assertThrows(ImagenInvalidaException.class, () -> agregar.agregar(DUENO, CITA_ID, "data:image/svg+xml;base64,PHN2Zy8+"));

        assertTrue(almacenamiento.claves().isEmpty());
        assertEquals(0, fotos.total());
    }

    @Test
    void siElBucketNoRespondeNoQuedaNingunaFilaEnLaBaseDeDatos() {
        AlmacenamientoFotosPort caido = mock(AlmacenamientoFotosPort.class);
        doThrow(new AlmacenamientoNoDisponibleException("caido", new RuntimeException()))
                .when(caido).guardar(anyString(), any(), anyString());
        reconstruir(caido, fotos, new VerificadorAccesoCita(citas, usuarios, clientes, tecnicos));

        assertThrows(AlmacenamientoNoDisponibleException.class, () -> agregar.agregar(DUENO, CITA_ID, IMAGEN));

        assertEquals(0, fotos.total());
    }

    @Test
    void siFallaGuardarLosMetadatosSeBorraElObjetoRecienSubido() {
        CitaFotoRepositoryPort roto = mock(CitaFotoRepositoryPort.class);
        when(roto.guardar(any())).thenThrow(new IllegalStateException("bd caida"));
        reconstruir(almacenamiento, roto, new VerificadorAccesoCita(citas, usuarios, clientes, tecnicos));

        assertThrows(IllegalStateException.class, () -> agregar.agregar(DUENO, CITA_ID, IMAGEN));

        assertTrue(almacenamiento.claves().isEmpty());
    }

    // ---------- eliminar ----------

    @Test
    void elDuenoBorraLaFotoYSuObjetoDelBucket() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);

        eliminar.eliminar(DUENO, CITA_ID, foto.id());

        assertEquals(0, fotos.total());
        assertTrue(almacenamiento.obtener(foto.clave()).isEmpty());
    }

    @Test
    void noSePuedeBorrarUnaFotoDeOtraCitaOInexistente() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);
        Cita otra = new Cita();
        otra.setId(21L);
        otra.setClienteId(1L);
        otra.setEstado(EstadoCita.PENDIENTE);
        when(citas.buscarPorId(21L)).thenReturn(Optional.of(otra));

        assertThrows(FotoNoEncontradaException.class, () -> eliminar.eliminar(DUENO, 21L, foto.id()));
        assertThrows(FotoNoEncontradaException.class, () -> eliminar.eliminar(DUENO, CITA_ID, 999L));

        assertEquals(1, fotos.total());
        assertTrue(almacenamiento.obtener(foto.clave()).isPresent());
    }

    @Test
    void borrarSigueLasMismasReglasDeDuenoYDeEstado() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);

        assertThrows(AccesoCitaDenegadoException.class, () -> eliminar.eliminar(OTRO_CLIENTE, CITA_ID, foto.id()));
        assertThrows(AccesoCitaDenegadoException.class, () -> eliminar.eliminar(TECNICO_ASIGNADO, CITA_ID, foto.id()));
        cita.setEstado(EstadoCita.EN_CAMINO);
        assertThrows(EstadoCitaInvalidoException.class, () -> eliminar.eliminar(DUENO, CITA_ID, foto.id()));

        assertEquals(1, fotos.total());
        assertTrue(almacenamiento.obtener(foto.clave()).isPresent());
    }

    @Test
    void siElBucketFallaAlBorrarSeRegistraUnWarnSinDatosSensiblesYSeBorraLaFila() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);
        AlmacenamientoFotosPort fallaAlEliminar = spy(almacenamiento);
        doThrow(new AlmacenamientoNoDisponibleException("no disponible", new RuntimeException()))
                .when(fallaAlEliminar).eliminar(anyString());
        reconstruir(fallaAlEliminar, fotos, new VerificadorAccesoCita(citas, usuarios, clientes, tecnicos));

        eliminar.eliminar(DUENO, CITA_ID, foto.id());

        assertEquals(0, fotos.total());
        verify(registroLog).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), anyString(), eq(null),
                eq("No se pudo eliminar una foto del almacenamiento; quedó un objeto huérfano (no disponible)"));
    }

    // ---------- obtener ----------

    @Test
    void elDuenoElTecnicoAsignadoYElAdministradorVenLaFoto() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);

        for (String correo : new String[]{DUENO, TECNICO_ASIGNADO, ADMIN}) {
            assertEquals("image/png", obtener.obtener(correo, CITA_ID, foto.id()).tipoContenido());
        }
    }

    @Test
    void otroClienteYOtroTecnicoRecibenAccesoDenegado() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);

        assertThrows(AccesoCitaDenegadoException.class, () -> obtener.obtener(OTRO_CLIENTE, CITA_ID, foto.id()));
        assertThrows(AccesoCitaDenegadoException.class, () -> obtener.obtener(OTRO_TECNICO, CITA_ID, foto.id()));
    }

    @Test
    void siLaCitaNoTieneTecnicoNingunTecnicoPuedeVerLaFoto() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);
        cita.setTecnicoId(null);

        assertThrows(AccesoCitaDenegadoException.class, () -> obtener.obtener(TECNICO_ASIGNADO, CITA_ID, foto.id()));
    }

    @Test
    void unUsuarioSinPerfilDeClienteOTecnicoNoTieneAcceso() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);
        when(clientes.buscarPorUsuarioId(8L)).thenReturn(Optional.empty());
        when(tecnicos.buscarPorUsuarioId(10L)).thenReturn(Optional.empty());

        assertThrows(AccesoCitaDenegadoException.class, () -> obtener.obtener(OTRO_CLIENTE, CITA_ID, foto.id()));
        assertThrows(AccesoCitaDenegadoException.class, () -> obtener.obtener(OTRO_TECNICO, CITA_ID, foto.id()));
    }

    @Test
    void laFotoSeBuscaDentroDeLaCitaIndicada() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);
        Cita otra = new Cita();
        otra.setId(21L);
        otra.setClienteId(1L);
        otra.setEstado(EstadoCita.CONFIRMADA);
        when(citas.buscarPorId(21L)).thenReturn(Optional.of(otra));

        assertThrows(FotoNoEncontradaException.class, () -> obtener.obtener(DUENO, 21L, foto.id()));
        assertThrows(FotoNoEncontradaException.class, () -> obtener.obtener(DUENO, CITA_ID, 999L));
        assertThrows(SolicitudNoEncontradaException.class, () -> obtener.obtener(DUENO, 99L, foto.id()));
    }

    @Test
    void siElObjetoYaNoEstaEnElBucketResponde404() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);
        almacenamiento.eliminar(foto.clave());

        assertThrows(FotoNoEncontradaException.class, () -> obtener.obtener(DUENO, CITA_ID, foto.id()));
    }

    @Test
    void elPrefijoDeCitaNoEsAccesibleDesdeLaRutaPublicaDeFotosDePerfil() {
        CitaFoto foto = agregar.agregar(DUENO, CITA_ID, IMAGEN);

        assertFalse(foto.clave().matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"));
        assertThrows(FotoNoEncontradaException.class, () -> new ObtenerFotoUseCaseImpl(almacenamiento).obtener(foto.clave()));
    }
}
