package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosCorreoCitaResolverTest {

    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private ObtenerDetalleCitaUseCase detalleUseCase;

    private Cita cita() {
        Cita c = new Cita();
        c.setClienteId(1L);
        return c;
    }

    private Cliente cliente(String nombre, String apellido) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        Cliente c = new Cliente();
        c.setUsuario(u);
        return c;
    }

    @Test
    void armaNombreDelClienteServicioYTecnicoDesdeElDetalle() {
        Cita c = cita();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente("Ana", "Lopez")));
        when(detalleUseCase.detallar(c)).thenReturn(new DetalleCita(c, "Luis", "Perez", "300", "Plomería"));

        DatosCorreoCitaResolver.DatosCorreoCita datos = new DatosCorreoCitaResolver(clientes, detalleUseCase).resolver(c);

        assertEquals("Ana Lopez", datos.nombreCliente());
        assertEquals("Plomería", datos.servicio());
        assertEquals("Luis Perez", datos.tecnico());
    }

    @Test
    void sinTecnicoNiEspecialidadNiClienteDevuelveCadenasVacias() {
        Cita c = cita();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.empty());
        when(detalleUseCase.detallar(c)).thenReturn(new DetalleCita(c, null, null, null, null));

        DatosCorreoCitaResolver.DatosCorreoCita datos = new DatosCorreoCitaResolver(clientes, detalleUseCase).resolver(c);

        assertEquals("", datos.nombreCliente());
        assertEquals("", datos.servicio());
        assertEquals("", datos.tecnico());
    }

    @Test
    void conApellidoONombreAusenteNoDejaEspaciosSobrantes() {
        Cita c = cita();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente("Ana", null)));
        when(detalleUseCase.detallar(c)).thenReturn(new DetalleCita(c, null, "Perez", null, "Gas"));

        DatosCorreoCitaResolver.DatosCorreoCita datos = new DatosCorreoCitaResolver(clientes, detalleUseCase).resolver(c);

        assertEquals("Ana", datos.nombreCliente());
        assertEquals("Perez", datos.tecnico());
    }
}
