package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.RegistroClienteGoogle;
import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort.GoogleUser;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprueba que el alta de usuario + perfil es atómica: se envuelve cada caso de uso con el
 * interceptor transaccional real de Spring (leyendo sus @Transactional) y un gestor simulado,
 * se fuerza el fallo del segundo guardado y se verifica que se pide rollback y nunca commit.
 */
class RegistroAtomicoUsuarioPerfilTest {

    private UsuarioRepositoryPort usuarios;
    private ClienteRepositoryPort clientes;
    private TecnicoRepositoryPort tecnicos;
    private PasswordEncoderPort encoder;
    private PlatformTransactionManager tx;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositoryPort.class);
        clientes = mock(ClienteRepositoryPort.class);
        tecnicos = mock(TecnicoRepositoryPort.class);
        encoder = mock(PasswordEncoderPort.class);
        tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
        when(encoder.encriptar(any())).thenReturn("hash");
        when(usuarios.buscarPorCorreo(any())).thenReturn(Optional.empty());
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(5L);
            return u;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T transaccional(T objetivo) {
        ProxyFactory fabrica = new ProxyFactory(objetivo);
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        return (T) fabrica.getProxy();
    }

    private void verificarRollbackSinCommit() {
        verify(tx).rollback(any(TransactionStatus.class));
        verify(tx, never()).commit(any(TransactionStatus.class));
    }

    @Test
    void clienteSiFallaElPerfilSeRevierteElUsuario() {
        when(clientes.guardar(any(Cliente.class))).thenThrow(new IllegalStateException("fallo BD"));
        var useCase = transaccional(new RegistrarClienteUseCaseImpl(usuarios, clientes, encoder));

        assertThrows(IllegalStateException.class, () -> useCase.registrar("Ana", "Lopez", "ana@mail.com",
                "clave12345", "300", TipoDocumento.CC, "123", "Calle 1"));

        verify(usuarios).guardar(any(Usuario.class));
        verificarRollbackSinCommit();
    }

    @Test
    void tecnicoSiFallaElPerfilSeRevierteElUsuario() {
        when(tecnicos.guardar(any(Tecnico.class))).thenThrow(new IllegalStateException("fallo BD"));
        var useCase = transaccional(new RegistrarTecnicoUseCaseImpl(usuarios, tecnicos, encoder));

        assertThrows(IllegalStateException.class, () -> useCase.registrar("Ana", "Ruiz", "t@x.com", "clave12345",
                "300", TipoDocumento.CC, "123", "Calle 1", 15, null, null));

        verify(usuarios).guardar(any(Usuario.class));
        verificarRollbackSinCommit();
    }

    @Test
    void googleSiFallaLaFilaDeClienteSeRevierteElUsuario() {
        when(clientes.guardar(any(Cliente.class))).thenThrow(new IllegalStateException("fallo BD"));
        var registro = transaccional(new RegistroClienteGoogle(usuarios, clientes));
        GoogleUser google = new GoogleUser("g-1", "ana@gmail.com", "Ana", "Lopez");

        assertThrows(IllegalStateException.class, () -> registro.registrar(google));

        verify(usuarios).guardar(any(Usuario.class));
        verificarRollbackSinCommit();
    }

    @Test
    void registrosCorrectosHacenCommitSinRollback() {
        when(clientes.guardar(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        var useCase = transaccional(new RegistrarClienteUseCaseImpl(usuarios, clientes, encoder));

        Cliente cliente = useCase.registrar("Ana", "Lopez", "ana@mail.com", "clave12345", "300",
                TipoDocumento.CC, "123", "Calle 1");

        assertEquals(5L, cliente.getUsuario().getId());
        verify(tx).commit(any(TransactionStatus.class));
        verify(tx, never()).rollback(any(TransactionStatus.class));
    }

    @Test
    void radioFueraDeRangoSeRechazaAntesDeGuardarNada() {
        var useCase = new RegistrarTecnicoUseCaseImpl(usuarios, tecnicos, encoder);

        for (int radio : new int[]{0, -5, 101, 1000}) {
            RadioCoberturaInvalidoException ex = assertThrows(RadioCoberturaInvalidoException.class,
                    () -> useCase.registrar("Ana", "Ruiz", "t@x.com", "clave12345", "300", TipoDocumento.CC,
                            "123", "Calle 1", radio, null, null));
            assertEquals("El radio de atención debe estar entre 1 y 100 km", ex.getMessage());
        }

        verify(usuarios, never()).guardar(any());
        verify(tecnicos, never()).guardar(any());
    }

    @Test
    void radioEnLosLimitesOSinRadioSeAcepta() {
        when(tecnicos.guardar(any(Tecnico.class))).thenAnswer(i -> i.getArgument(0));
        var useCase = new RegistrarTecnicoUseCaseImpl(usuarios, tecnicos, encoder);

        assertEquals(1, useCase.registrar("A", "R", "a@x.com", "clave12345", "3", TipoDocumento.CC, "1", "C",
                1, null, null).getRadioCoberturaKm());
        assertEquals(100, useCase.registrar("A", "R", "b@x.com", "clave12345", "3", TipoDocumento.CC, "2", "C",
                100, null, null).getRadioCoberturaKm());
        assertEquals(10, useCase.registrar("A", "R", "c@x.com", "clave12345", "3", TipoDocumento.CC, "3", "C",
                null, null, null).getRadioCoberturaKm());
    }
}
