package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasAdminUseCase;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasAdminPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: el administrador lista las citas de todos los técnicos con filtros y paginación. */
@Service
public class ConsultarCitasAdminUseCaseImpl implements ConsultarCitasAdminUseCase {

    static final int TAMANIO_MAXIMO = 50;

    private final ConsultaCitasAdminPort consultaCitasAdminPort;

    public ConsultarCitasAdminUseCaseImpl(ConsultaCitasAdminPort consultaCitasAdminPort) {
        this.consultaCitasAdminPort = consultaCitasAdminPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResultado<CitaResumenAdmin> consultar(FiltroCitasAdmin filtro) {
        validar(filtro);
        return consultaCitasAdminPort.buscar(filtro);
    }

    private void validar(FiltroCitasAdmin filtro) {
        if (filtro.pagina() < 0) {
            throw new FiltroCitasInvalidoException("La página debe ser mayor o igual a 0");
        }
        if (filtro.tamanio() < 1 || filtro.tamanio() > TAMANIO_MAXIMO) {
            throw new FiltroCitasInvalidoException("El tamaño debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new FiltroCitasInvalidoException("fechaDesde no puede ser posterior a fechaHasta");
        }
    }
}
