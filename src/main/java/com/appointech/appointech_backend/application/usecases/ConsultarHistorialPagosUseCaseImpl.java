package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.ports.in.ConsultarHistorialPagosUseCase;
import com.appointech.appointech_backend.domain.ports.out.ConsultaHistorialPagosPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso (RF25): el administrador consulta el historial de pagos con filtros y paginación. */
@Service
public class ConsultarHistorialPagosUseCaseImpl implements ConsultarHistorialPagosUseCase {

    private final ConsultaHistorialPagosPort consultaHistorialPagosPort;

    public ConsultarHistorialPagosUseCaseImpl(ConsultaHistorialPagosPort consultaHistorialPagosPort) {
        this.consultaHistorialPagosPort = consultaHistorialPagosPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ListadoConTotal<PagoDelHistorial> consultar(FiltroHistorialPagos filtro) {
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new FiltroCitasInvalidoException("desde no puede ser posterior a hasta");
        }
        PaginacionOpcional paginacion = PaginacionOpcional.de(filtro.pagina(), filtro.tamanio());
        // A diferencia de otros listados, aquí nunca se devuelve todo: la tabla crece sin límite.
        int pagina = paginacion.paginado() ? paginacion.pagina() : 0;
        int tamanio = paginacion.paginado() ? paginacion.tamanio() : PaginacionOpcional.TAMANIO_POR_DEFECTO;
        return consultaHistorialPagosPort.buscar(new FiltroHistorialPagos(filtro.estado(), filtro.metodo(),
                filtro.clienteId(), filtro.tecnicoId(), filtro.desde(), filtro.hasta(), pagina, tamanio));
    }
}
