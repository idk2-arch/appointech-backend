package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.EliminadorObjetoFoto;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.ports.in.EliminarFotoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import org.springframework.stereotype.Service;

/** Caso de uso: el cliente dueño quita una foto del electrodoméstico de su cita. */
@Service
public class EliminarFotoCitaUseCaseImpl implements EliminarFotoCitaUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final CitaFotoRepositoryPort citaFotoRepositoryPort;
    private final EliminadorObjetoFoto eliminadorObjeto;

    public EliminarFotoCitaUseCaseImpl(VerificadorAccesoCita verificadorAcceso,
                                       CitaFotoRepositoryPort citaFotoRepositoryPort,
                                       EliminadorObjetoFoto eliminadorObjeto) {
        this.verificadorAcceso = verificadorAcceso;
        this.citaFotoRepositoryPort = citaFotoRepositoryPort;
        this.eliminadorObjeto = eliminadorObjeto;
    }

    @Override
    public void eliminar(String correoCliente, Long citaId, Long fotoId) {
        Cita cita = verificadorAcceso.exigirDuenoCliente(correoCliente, citaId);
        if (!cita.getEstado().admiteFotosDelElectrodomestico()) {
            throw new EstadoCitaInvalidoException("Solo se pueden quitar fotos de una cita PENDIENTE o CONFIRMADA");
        }
        CitaFoto foto = citaFotoRepositoryPort.buscarPorIdYCita(fotoId, citaId)
                .orElseThrow(FotoNoEncontradaException::new);
        citaFotoRepositoryPort.eliminar(foto.id());
        eliminadorObjeto.eliminar(foto.clave());
    }
}
