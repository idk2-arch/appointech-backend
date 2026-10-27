package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.BloqueInvalidoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ListarBloquesUseCase;
import com.appointech.appointech_backend.domain.ports.out.GestionBloquesPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Caso de uso: el técnico consulta sus bloques de indisponibilidad.
 * <p>Historia de usuario: <a href="https://rossas2111.atlassian.net/browse/SCRUM-17">SCRUM-17</a> - RF13 - Bloques de indisponibilidad
 */
@Service
public class ListarBloquesUseCaseImpl implements ListarBloquesUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final GestionBloquesPort gestionBloquesPort;
    private final Clock reloj;

    public ListarBloquesUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                    GestionBloquesPort gestionBloquesPort,
                                    Clock reloj) {
        this.resolverTecnico = resolverTecnico;
        this.gestionBloquesPort = gestionBloquesPort;
        this.reloj = reloj;
    }

    @Override
    public List<BloqueIndisponibilidad> listar(String correoTecnico, LocalDate desde, LocalDate hasta) {
        LocalDate fechaDesde = desde != null ? desde : LocalDate.now(reloj);
        if (hasta != null && hasta.isBefore(fechaDesde)) {
            throw new BloqueInvalidoException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        LocalDateTime limiteInferior = fechaDesde.atStartOfDay();
        LocalDateTime limiteSuperior = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;

        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        return gestionBloquesPort.listarDelTecnico(tecnico.getId(), limiteInferior, limiteSuperior).stream()
                .sorted(Comparator.comparing(BloqueIndisponibilidad::getInicio))
                .toList();
    }
}
