package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.domain.ports.out.ConsultaHistorialPagosPort;
import com.appointech.appointech_backend.infrastructure.repositories.HistorialPagosJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.PagoHistorialFila;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ConsultaHistorialPagosAdapter implements ConsultaHistorialPagosPort {

    private static final EstadoPago ESTADO_NINGUNO = EstadoPago.PENDIENTE;
    private static final MetodoPago METODO_NINGUNO = MetodoPago.EFECTIVO;
    private static final long ID_NINGUNO = -1L;
    private static final LocalDateTime SIN_LIMITE_INFERIOR = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime SIN_LIMITE_SUPERIOR = LocalDateTime.of(9999, 1, 1, 0, 0);

    private final HistorialPagosJpaRepository historialPagosJpaRepository;

    public ConsultaHistorialPagosAdapter(HistorialPagosJpaRepository historialPagosJpaRepository) {
        this.historialPagosJpaRepository = historialPagosJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ListadoConTotal<PagoDelHistorial> buscar(FiltroHistorialPagos filtro) {
        boolean sinFechas = filtro.desde() == null && filtro.hasta() == null;
        Page<PagoHistorialFila> pagina = historialPagosJpaRepository.buscar(
                filtro.estado() == null, filtro.estado() == null ? ESTADO_NINGUNO : filtro.estado(),
                filtro.metodo() == null, filtro.metodo() == null ? METODO_NINGUNO : filtro.metodo(),
                filtro.clienteId() == null, filtro.clienteId() == null ? ID_NINGUNO : filtro.clienteId(),
                filtro.tecnicoId() == null, filtro.tecnicoId() == null ? ID_NINGUNO : filtro.tecnicoId(),
                sinFechas,
                filtro.desde() == null ? SIN_LIMITE_INFERIOR : filtro.desde().atStartOfDay(),
                // "hasta" es inclusiva: llega hasta el inicio del día siguiente, sin incluirlo.
                filtro.hasta() == null ? SIN_LIMITE_SUPERIOR : filtro.hasta().plusDays(1).atStartOfDay(),
                PageRequest.of(filtro.pagina(), filtro.tamanio()));
        return new ListadoConTotal<>(pagina.getContent().stream().map(this::aDominio).toList(), pagina.getTotalElements());
    }

    private PagoDelHistorial aDominio(PagoHistorialFila fila) {
        return new PagoDelHistorial(
                fila.citaId(), fila.clienteId(), nombreCompleto(fila.clienteNombre(), fila.clienteApellido()),
                fila.tecnicoId(), nombreCompleto(fila.tecnicoNombre(), fila.tecnicoApellido()),
                fila.especialidad(), fila.montoCotizado(), fila.montoFinal(), fila.metodo(), fila.estado(),
                fila.pagadoEn(),
                fila.comprobanteId() == null ? null : new ComprobantePago(fila.comprobanteId(), null, null).numero());
    }

    private String nombreCompleto(String nombre, String apellido) {
        return nombre == null ? null : nombre + " " + apellido;
    }
}
