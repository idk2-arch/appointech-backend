package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ReglasFranjasTrabajo;
import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;
import com.appointech.appointech_backend.domain.ports.in.ConsultarFranjasTrabajoUseCase;
import com.appointech.appointech_backend.domain.ports.out.FranjaTrabajoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Caso de uso: las franjas de un técnico en un rango de fechas, con cuántos bloques tienen y cuántos están ocupados. */
@Service
public class ConsultarFranjasTrabajoUseCaseImpl implements ConsultarFranjasTrabajoUseCase {

    static final int DIAS_POR_DEFECTO = 30;
    static final int DIAS_MAXIMOS = 90;

    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final FranjaTrabajoRepositoryPort franjaRepositoryPort;
    private final ReglasFranjasTrabajo reglas;

    public ConsultarFranjasTrabajoUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort,
                                              FranjaTrabajoRepositoryPort franjaRepositoryPort,
                                              ReglasFranjasTrabajo reglas) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.franjaRepositoryPort = franjaRepositoryPort;
        this.reglas = reglas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FranjaTrabajoConOcupacion> listar(Long tecnicoId, LocalDate desde, LocalDate hasta) {
        tecnicoRepositoryPort.buscarPorId(tecnicoId).orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));
        LocalDate inicio = desde != null ? desde : reglas.ahora().toLocalDate();
        LocalDate fin = hasta != null ? hasta : inicio.plusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) {
            throw new FiltroCitasInvalidoException("desde no puede ser posterior a hasta");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > DIAS_MAXIMOS) {
            throw new FiltroCitasInvalidoException("El rango no puede superar " + DIAS_MAXIMOS + " días");
        }

        List<Cita> citas = reglas.citasAgendadas(tecnicoId, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());
        return franjaRepositoryPort.buscarDelTecnicoEntre(tecnicoId, inicio, fin).stream()
                .map(franja -> reglas.conOcupacion(franja, citas))
                .toList();
    }
}
