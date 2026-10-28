package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** fotoUrl se agrega al final de cada respuesta; es null cuando no hay foto y /api/fotos/&lt;clave&gt; cuando la hay. */
class FotoUrlEnRespuestasTest {

    private static final String CLAVE = "11111111-2222-3333-4444-555555555555";
    private static final String URL = "/api/fotos/" + CLAVE;

    private Usuario usuario(String fotoClave) {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("Ana");
        u.setApellido("Gomez");
        u.setCorreo("ana@mail.com");
        u.setRol(RolUsuario.TECNICO);
        u.setFotoClave(fotoClave);
        return u;
    }

    private Cita cita() {
        Cita c = new Cita();
        c.setId(10L);
        c.setEstado(EstadoCita.CONFIRMADA);
        return c;
    }

    @Test
    void elMapeadorDeUrlDevuelveNullSinFotoYLaRutaPublicaConFoto() {
        assertNull(FotoUrlMapper.aUrl(null));
        assertEquals(URL, FotoUrlMapper.aUrl(CLAVE));
    }

    @Test
    void laCitaDelClienteTraeLaFotoDelTecnico() {
        assertEquals(URL, CitaResponseMapper.aRespuesta(new DetalleCita(cita(), "Luis", "Perez", "300", "Plomeria", CLAVE)).fotoUrl());
        assertNull(CitaResponseMapper.aRespuesta(new DetalleCita(cita(), "Luis", "Perez", "300", "Plomeria")).fotoUrl());
    }

    @Test
    void elDetalleDeCitaTraeLaFotoDelTecnico() {
        DetalleCita detalle = new DetalleCita(cita(), "Luis", "Perez", "300", "Plomeria", CLAVE);

        assertEquals(URL, CitaDetalleMapper.aRespuesta(new DetalleCitaCompleto(detalle, List.of())).fotoUrl());
        assertNull(CitaDetalleMapper.aRespuesta(new DetalleCitaCompleto(
                new DetalleCita(cita(), null, null, null, null), List.of())).fotoUrl());
    }

    @Test
    void elTecnicoVeLaFotoDelClienteEnSuAgenda() {
        ClienteResumen conFoto = new ClienteResumen("Ana", "Gomez", "300", CLAVE);
        ClienteResumen sinFoto = new ClienteResumen("Ana", "Gomez", "300");

        assertEquals(CLAVE, conFoto.fotoClave());
        assertNull(sinFoto.fotoClave());
    }

    @Test
    void laRespuestaDeTecnicoYClienteAdminTraenSuFoto() {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(1L);
        tecnico.setUsuario(usuario(CLAVE));
        tecnico.setEspecialidades(new ArrayList<>());
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setUsuario(usuario(null));

        assertEquals(URL, TecnicoResponseMapper.aRespuesta(tecnico).fotoUrl());
        assertNull(ClienteAdminResponseMapper.aRespuesta(cliente).fotoUrl());
        cliente.setUsuario(usuario(CLAVE));
        assertEquals(URL, ClienteAdminResponseMapper.aRespuesta(cliente).fotoUrl());
    }
}
