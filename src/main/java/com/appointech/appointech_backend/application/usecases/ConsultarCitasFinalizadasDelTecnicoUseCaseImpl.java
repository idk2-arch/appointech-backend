package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.CitaFinalizadaConPago;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.EstadoCobro;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasFinalizadasDelTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasFinalizadasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: citas finalizadas del técnico (las más recientes primero, máximo {@value #LIMITE}) con la situación de su
 * cobro, para que vea cuáles aún no tienen costo final. Una cita sin pago cuenta como sin costo final.
 * <p>Historias de usuario: SCRUM-23 (RF22 - costo final).
 */
@Service
public class ConsultarCitasFinalizadasDelTecnicoUseCaseImpl implements ConsultarCitasFinalizadasDelTecnicoUseCase {

    static final int LIMITE = 50;

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final ConsultaCitasFinalizadasDelTecnicoPort consultaCitasPort;
    private final ConsultaClientesPort consultaClientesPort;
    private final CitaFotoRepositoryPort citaFotoRepositoryPort;
    private final PagoRepositoryPort pagoRepositoryPort;

    public ConsultarCitasFinalizadasDelTecnicoUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                                          ConsultaCitasFinalizadasDelTecnicoPort consultaCitasPort,
                                                          ConsultaClientesPort consultaClientesPort,
                                                          CitaFotoRepositoryPort citaFotoRepositoryPort,
                                                          PagoRepositoryPort pagoRepositoryPort) {
        this.resolverTecnico = resolverTecnico;
        this.consultaCitasPort = consultaCitasPort;
        this.consultaClientesPort = consultaClientesPort;
        this.citaFotoRepositoryPort = citaFotoRepositoryPort;
        this.pagoRepositoryPort = pagoRepositoryPort;
    }

    @Override
    public List<CitaFinalizadaConPago> consultar(String correoTecnico) {
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        List<Cita> citas = consultaCitasPort.buscarFinalizadas(tecnico.getId(), LIMITE);
        List<Long> ids = citas.stream().map(Cita::getId).toList();

        Map<Long, ClienteResumen> clientes = consultaClientesPort.resumenesPorId(
                citas.stream().map(Cita::getClienteId).distinct().toList());
        Map<Long, List<CitaFoto>> fotos = citaFotoRepositoryPort.listarPorCitas(ids);
        Map<Long, Pago> pagos = pagoRepositoryPort.buscarPorCitaIds(ids).stream()
                .collect(Collectors.toMap(Pago::getCitaId, Function.identity()));

        return citas.stream()
                .map(cita -> new CitaFinalizadaConPago(
                        new CitaConCliente(cita, clientes.get(cita.getClienteId()),
                                fotos.getOrDefault(cita.getId(), List.of())),
                        pagos.containsKey(cita.getId()) ? pagos.get(cita.getId()).estadoCobro()
                                : EstadoCobro.SIN_COSTO_FINAL))
                .toList();
    }
}
