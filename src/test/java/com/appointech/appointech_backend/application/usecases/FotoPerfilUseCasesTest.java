package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorFotoUsuario;
import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.ImagenInvalidaException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import com.appointech.appointech_backend.infrastructure.adapters.AlmacenamientoFotosEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FotoPerfilUseCasesTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    private static final String IMAGEN = "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG);
    private static final String UUID_REGEX = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";

    private AlmacenamientoFotosEnMemoria almacenamiento;
    private UsuarioRepositoryPort usuarios;
    private RegistroLogPort registroLog;
    private ActualizarFotoPerfilUseCaseImpl perfil;
    private ActualizarFotoUsuarioUseCaseImpl admin;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        almacenamiento = new AlmacenamientoFotosEnMemoria();
        usuarios = mock(UsuarioRepositoryPort.class);
        registroLog = mock(RegistroLogPort.class);
        GestorFotoUsuario gestor = new GestorFotoUsuario(almacenamiento, usuarios, registroLog);
        perfil = new ActualizarFotoPerfilUseCaseImpl(usuarios, gestor);
        admin = new ActualizarFotoUsuarioUseCaseImpl(usuarios, gestor);
        ana = new Usuario();
        ana.setId(7L);
        ana.setCorreo("ana@mail.com");
        ana.setNombre("Ana");
        ana.setRol(RolUsuario.CLIENTE);
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(ana));
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(ana));
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void guardaLaFotoPorPrimeraVezConUnaClaveUuidAleatoriaSinDatosDelUsuario() {
        String clave = perfil.reemplazar("ana@mail.com", IMAGEN);

        assertTrue(clave.matches(UUID_REGEX));
        assertFalse(clave.contains("ana"));
        assertEquals(clave, ana.getFotoClave());
        assertEquals("image/png", almacenamiento.obtener(clave).orElseThrow().tipoContenido());
        assertEquals(1, almacenamiento.claves().size());
        verify(usuarios).guardar(ana);
    }

    @Test
    void alReemplazarGuardaLaNuevaYEliminaElObjetoAnterior() {
        String primera = perfil.reemplazar("ana@mail.com", IMAGEN);

        String segunda = perfil.reemplazar("ana@mail.com", IMAGEN);

        assertNotEquals(primera, segunda);
        assertEquals(segunda, ana.getFotoClave());
        assertTrue(almacenamiento.obtener(primera).isEmpty());
        assertTrue(almacenamiento.obtener(segunda).isPresent());
        assertEquals(1, almacenamiento.claves().size());
    }

    @Test
    void alEliminarBorraElObjetoYLimpiaLaClave() {
        String clave = perfil.reemplazar("ana@mail.com", IMAGEN);

        perfil.eliminar("ana@mail.com");

        assertNull(ana.getFotoClave());
        assertTrue(almacenamiento.obtener(clave).isEmpty());
    }

    @Test
    void eliminarSinFotoNoHaceNada() {
        perfil.eliminar("ana@mail.com");

        verify(usuarios, never()).guardar(any());
        assertTrue(almacenamiento.claves().isEmpty());
    }

    @Test
    void unaImagenInvalidaNoTocaNiElAlmacenamientoNiElUsuario() {
        assertThrows(ImagenInvalidaException.class, () -> perfil.reemplazar("ana@mail.com", "no-es-una-imagen"));

        assertTrue(almacenamiento.claves().isEmpty());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void siElAlmacenamientoFallaNoSeActualizaElUsuarioYConservaLaFotoAnterior() {
        String anterior = perfil.reemplazar("ana@mail.com", IMAGEN);
        AlmacenamientoFotosPort caido = mock(AlmacenamientoFotosPort.class);
        doThrow(new AlmacenamientoNoDisponibleException("caido", new RuntimeException()))
                .when(caido).guardar(anyString(), any(), anyString());
        ActualizarFotoPerfilUseCaseImpl conAlmacenamientoCaido = new ActualizarFotoPerfilUseCaseImpl(
                usuarios, new GestorFotoUsuario(caido, usuarios, registroLog));

        assertThrows(AlmacenamientoNoDisponibleException.class,
                () -> conAlmacenamientoCaido.reemplazar("ana@mail.com", IMAGEN));

        assertEquals(anterior, ana.getFotoClave());
        verify(caido, never()).eliminar(anyString());
    }

    @Test
    void siFallaGuardarElUsuarioSeBorraElObjetoNuevoYSePropagaElError() {
        when(usuarios.guardar(any())).thenThrow(new IllegalStateException("bd caida"));

        assertThrows(IllegalStateException.class, () -> perfil.reemplazar("ana@mail.com", IMAGEN));

        assertTrue(almacenamiento.claves().isEmpty());
    }

    @Test
    void siNoSePuedeBorrarElObjetoAnteriorSeRegistraUnWarnYLaOperacionTerminaBien() {
        AlmacenamientoFotosEnMemoria base = new AlmacenamientoFotosEnMemoria();
        AlmacenamientoFotosPort fallaAlEliminar = spy(base);
        doThrow(new AlmacenamientoNoDisponibleException("caido", new RuntimeException()))
                .when(fallaAlEliminar).eliminar(anyString());
        ActualizarFotoPerfilUseCaseImpl caso = new ActualizarFotoPerfilUseCaseImpl(
                usuarios, new GestorFotoUsuario(fallaAlEliminar, usuarios, registroLog));
        caso.reemplazar("ana@mail.com", IMAGEN);

        String nueva = assertDoesNotThrow(() -> caso.reemplazar("ana@mail.com", IMAGEN));

        assertEquals(nueva, ana.getFotoClave());
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(registroLog).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), anyString(), eq(null), mensaje.capture());
        assertFalse(mensaje.getValue().contains(nueva));
    }

    @Test
    void unUsuarioInexistenteNoGuardaNada() {
        when(usuarios.buscarPorCorreo("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> perfil.reemplazar("nadie@mail.com", IMAGEN));
        assertThrows(UsuarioNoEncontradoException.class, () -> perfil.eliminar("nadie@mail.com"));
        assertTrue(almacenamiento.claves().isEmpty());
    }

    @Test
    void elAdministradorGuardaLaFotoDeOtroUsuario() {
        String clave = admin.reemplazar(7L, IMAGEN);

        assertEquals(clave, ana.getFotoClave());
        assertTrue(almacenamiento.obtener(clave).isPresent());
    }

    @Test
    void elAdministradorNoPuedeGuardarFotoDeUnUsuarioInexistente() {
        when(usuarios.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> admin.reemplazar(99L, IMAGEN));
        assertTrue(almacenamiento.claves().isEmpty());
    }

    @Test
    void obtenerDevuelveLaFotoDeUnaClaveUuidExistente() {
        String clave = UUID.randomUUID().toString();
        almacenamiento.guardar(clave, PNG, "image/png");

        FotoAlmacenada foto = new ObtenerFotoUseCaseImpl(almacenamiento).obtener(clave);

        assertEquals("image/png", foto.tipoContenido());
    }

    @Test
    void obtenerRespondeNoEncontradaSiNoExisteOSiLaClaveNoEsUuid() {
        AlmacenamientoFotosPort puerto = mock(AlmacenamientoFotosPort.class);
        when(puerto.obtener(anyString())).thenReturn(Optional.empty());
        ObtenerFotoUseCaseImpl caso = new ObtenerFotoUseCaseImpl(puerto);

        assertThrows(FotoNoEncontradaException.class, () -> caso.obtener(UUID.randomUUID().toString()));
        assertThrows(FotoNoEncontradaException.class, () -> caso.obtener("../../etc/passwd"));
        assertThrows(FotoNoEncontradaException.class, () -> caso.obtener("mi-foto.png"));
        assertThrows(FotoNoEncontradaException.class, () -> caso.obtener(UUID.randomUUID().toString().toUpperCase()));
        assertThrows(FotoNoEncontradaException.class, () -> caso.obtener(null));
    }

    @Test
    void laClaveInvalidaNuncaLlegaAlAlmacenamiento() {
        AlmacenamientoFotosPort puerto = mock(AlmacenamientoFotosPort.class);

        assertThrows(FotoNoEncontradaException.class, () -> new ObtenerFotoUseCaseImpl(puerto).obtener("x/../y"));

        verifyNoInteractions(puerto);
    }
}
