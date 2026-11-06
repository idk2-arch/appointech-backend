package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.CalificacionesDelTecnico;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCalificacionesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.CalificacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Caso de uso (RF07): el técnico ve las calificaciones que recibió y su promedio (calculado en la base, no en memoria). */
@Service
public class ConsultarCalificacionesTecnicoUseCaseImpl implements ConsultarCalificacionesTecnicoUseCase {

    private static final int DECIMALES_DEL_PROMEDIO = 2;

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final CalificacionRepositoryPort calificacionRepositoryPort;

    public ConsultarCalificacionesTecnicoUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                                     CalificacionRepositoryPort calificacionRepositoryPort) {
        this.resolverTecnico = resolverTecnico;
        this.calificacionRepositoryPort = calificacionRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CalificacionesDelTecnico consultar(String correoTecnico, Integer pagina, Integer tamanio) {
        PaginacionOpcional paginacion = PaginacionOpcional.de(pagina, tamanio);
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);

        ListadoConTotal<CalificacionRecibida> recibidas =
                calificacionRepositoryPort.listarRecibidasPorTecnico(tecnico.getId(), paginacion);
        Double promedio = calificacionRepositoryPort.promedioDelTecnico(tecnico.getId())
                .map(ConsultarCalificacionesTecnicoUseCaseImpl::redondear)
                .orElse(null);
        return new CalificacionesDelTecnico(promedio, recibidas.total(), recibidas.elementos());
    }

    private static double redondear(double promedio) {
        return BigDecimal.valueOf(promedio).setScale(DECIMALES_DEL_PROMEDIO, RoundingMode.HALF_UP).doubleValue();
    }
}
