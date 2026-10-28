package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ConstructorPerfilPropio;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DatosPerfilEditable;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.PerfilPropio;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerfilPropioUseCasesTest {

    private UsuarioRepositoryPort usuarios;
    private ClienteRepositoryPort clientes;
    private TecnicoRepositoryPort tecnicos;
    private ConsultarPerfilPropioUseCaseImpl consultar;
    private ActualizarPerfilPropioUseCaseImpl actualizar;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositoryPort.class);
        clientes = mock(ClienteRepositoryPort.class);
        tecnicos = mock(TecnicoRepositoryPort.class);
        ConstructorPerfilPropio constructor = new ConstructorPerfilPropio(clientes, tecnicos);
        consultar = new ConsultarPerfilPropioUseCaseImpl(usuarios, constructor);
        actualizar = new ActualizarPerfilPropioUseCaseImpl(usuarios, clientes, tecnicos, constructor);
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Usuario usuario(Long id, RolUsuario rol, String correo) {
        Usuario u = new Usuario(id, "Ana", "Gomez", correo, "hash", "3001112233", TipoDocumento.CC, "123", rol, true,
                LocalDateTime.of(2026, 1, 1, 0, 0));
        when(usuarios.buscarPorCorreo(correo)).thenReturn(Optional.of(u));
        return u;
    }

    private Cliente cliente(Usuario u) {
        Cliente c = new Cliente(7L, u, "Calle 1", 4.6, -74.0);
        when(clientes.buscarPorUsuarioId(u.getId())).thenReturn(Optional.of(c));
        return c;
    }

    private Tecnico tecnico(Usuario u) {
        Tecnico t = new Tecnico(8L, u, "Carrera 2", 4.7, -74.1, 15,
                List.of(new Especialidad(1L, "Plomeria", "Tuberias", true)));
        when(tecnicos.buscarPorUsuarioId(u.getId())).thenReturn(Optional.of(t));
        return t;
    }

    // ---------- consultar ----------

    @Test
    void elClienteVeSuDireccionSinRadioNiEspecialidades() {
        cliente(usuario(1L, RolUsuario.CLIENTE, "c@mail.com"));

        PerfilPropio perfil = consultar.consultar("c@mail.com");

        assertEquals("Calle 1", perfil.direccion());
        assertNull(perfil.radioCoberturaKm());
        assertTrue(perfil.especialidades().isEmpty());
    }

    @Test
    void elTecnicoVeDireccionBaseRadioYEspecialidades() {
        tecnico(usuario(2L, RolUsuario.TECNICO, "t@mail.com"));

        PerfilPropio perfil = consultar.consultar("t@mail.com");

        assertEquals("Carrera 2", perfil.direccion());
        assertEquals(15, perfil.radioCoberturaKm());
        assertEquals("Plomeria", perfil.especialidades().get(0).getNombre());
    }

    @Test
    void elTecnicoSinListaDeEspecialidadesVeUnaListaVacia() {
        Tecnico t = tecnico(usuario(2L, RolUsuario.TECNICO, "t@mail.com"));
        t.setEspecialidades(null);

        assertTrue(consultar.consultar("t@mail.com").especialidades().isEmpty());
    }

    @Test
    void elAdministradorSoloTieneDatosDeUsuario() {
        Usuario admin = usuario(3L, RolUsuario.ADMINISTRADOR, "a@mail.com");

        PerfilPropio perfil = consultar.consultar("a@mail.com");

        assertSame(admin, perfil.usuario());
        assertNull(perfil.direccion());
        assertTrue(perfil.especialidades().isEmpty());
    }

    @Test
    void siFaltaLaFilaDeClienteOTecnicoDevuelveSoloElUsuario() {
        usuario(1L, RolUsuario.CLIENTE, "c@mail.com");
        usuario(2L, RolUsuario.TECNICO, "t@mail.com");
        when(clientes.buscarPorUsuarioId(1L)).thenReturn(Optional.empty());
        when(tecnicos.buscarPorUsuarioId(2L)).thenReturn(Optional.empty());

        assertNull(consultar.consultar("c@mail.com").direccion());
        assertNull(consultar.consultar("t@mail.com").direccion());
    }

    @Test
    void consultarUnCorreoDesconocidoLanzaUsuarioNoEncontrado() {
        when(usuarios.buscarPorCorreo("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> consultar.consultar("x@mail.com"));
    }

    // ---------- actualizar ----------

    @Test
    void elClienteEditaNombreApellidoTelefonoYDireccionSinTocarLoDemas() {
        Usuario u = usuario(1L, RolUsuario.CLIENTE, "c@mail.com");
        Cliente c = cliente(u);

        PerfilPropio perfil = actualizar.actualizar("c@mail.com",
                new DatosPerfilEditable(" Maria ", " Lopez ", " 3109998877 ", " Calle 99 "));

        assertEquals("Maria", u.getNombre());
        assertEquals("Lopez", u.getApellido());
        assertEquals("3109998877", u.getTelefono());
        assertEquals("Calle 99", c.getDireccion());
        assertEquals("c@mail.com", u.getCorreo());
        assertEquals("123", u.getNumeroDocumento());
        assertEquals(TipoDocumento.CC, u.getTipoDocumento());
        assertEquals(RolUsuario.CLIENTE, u.getRol());
        assertTrue(u.isActivo());
        assertEquals("Calle 99", perfil.direccion());
        verify(clientes).guardar(c);
    }

    @Test
    void elTecnicoEditaSuDireccionBaseSinTocarRadioNiEspecialidades() {
        Usuario u = usuario(2L, RolUsuario.TECNICO, "t@mail.com");
        Tecnico t = tecnico(u);

        PerfilPropio perfil = actualizar.actualizar("t@mail.com",
                new DatosPerfilEditable("Luis", "Perez", "3101234567", "Avenida 5"));

        assertEquals("Avenida 5", t.getDireccionBase());
        assertEquals(15, t.getRadioCoberturaKm());
        assertEquals(1, t.getEspecialidades().size());
        assertEquals(15, perfil.radioCoberturaKm());
        verify(tecnicos).guardar(t);
    }

    @Test
    void sinDireccionEnLaPeticionLaDireccionNoCambia() {
        Usuario u = usuario(1L, RolUsuario.CLIENTE, "c@mail.com");
        Cliente c = cliente(u);

        actualizar.actualizar("c@mail.com", new DatosPerfilEditable("Maria", "Lopez", "3109998877", null));

        assertEquals("Calle 1", c.getDireccion());
        verify(clientes, never()).guardar(any());
    }

    @Test
    void elAdministradorEditaSusDatosYSuDireccionSeIgnora() {
        Usuario u = usuario(3L, RolUsuario.ADMINISTRADOR, "a@mail.com");

        PerfilPropio perfil = actualizar.actualizar("a@mail.com",
                new DatosPerfilEditable("Root", "Admin", "3100000000", "Calle ignorada"));

        assertEquals("Root", u.getNombre());
        assertNull(perfil.direccion());
        verify(clientes, never()).guardar(any());
        verify(tecnicos, never()).guardar(any());
    }

    @Test
    void siClienteOTecnicoNoTienenFilaLaDireccionNoSeGuarda() {
        usuario(1L, RolUsuario.CLIENTE, "c@mail.com");
        usuario(2L, RolUsuario.TECNICO, "t@mail.com");
        when(clientes.buscarPorUsuarioId(1L)).thenReturn(Optional.empty());
        when(tecnicos.buscarPorUsuarioId(2L)).thenReturn(Optional.empty());

        actualizar.actualizar("c@mail.com", new DatosPerfilEditable("A", "B", "3100000000", "Dir"));
        actualizar.actualizar("t@mail.com", new DatosPerfilEditable("A", "B", "3100000000", "Dir"));

        verify(clientes, never()).guardar(any());
        verify(tecnicos, never()).guardar(any());
    }

    @Test
    void actualizarSoloAfectaAlUsuarioDelTokenYNuncaALosDemas() {
        Usuario propio = usuario(1L, RolUsuario.CLIENTE, "c@mail.com");
        Usuario ajeno = usuario(9L, RolUsuario.CLIENTE, "otro@mail.com");
        cliente(propio);
        cliente(ajeno);

        actualizar.actualizar("c@mail.com", new DatosPerfilEditable("Nuevo", "Nombre", "3100000000", "Dir"));

        assertEquals("Nuevo", propio.getNombre());
        assertEquals("Ana", ajeno.getNombre());
        verify(usuarios, never()).buscarPorId(any());
        verify(usuarios, never()).buscarPorCorreo("otro@mail.com");
    }

    @Test
    void actualizarUnCorreoDesconocidoLanzaUsuarioNoEncontrado() {
        when(usuarios.buscarPorCorreo("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> actualizar.actualizar("x@mail.com",
                new DatosPerfilEditable("A", "B", "3100000000", null)));
        verify(usuarios, never()).guardar(any());
    }
}
