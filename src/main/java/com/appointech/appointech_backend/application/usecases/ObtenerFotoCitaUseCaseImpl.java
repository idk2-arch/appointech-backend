package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.in.ObtenerFotoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import org.springframework.stereotype.Service;

/** Caso de uso: entrega los bytes de una foto de la cita al cliente dueño, al técnico asignado o a un administrador. */
@Service
public class ObtenerFotoCitaUseCaseImpl implements ObtenerFotoCitaUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final CitaFotoRepositoryPort citaFotoRepositoryPort;
    private final AlmacenamientoFotosPort almacenamiento;

    public ObtenerFotoCitaUseCaseImpl(VerificadorAccesoCita verificadorAcceso,
                                      CitaFotoRepositoryPort citaFotoRepositoryPort,
                                      AlmacenamientoFotosPort almacenamiento) {
        this.verificadorAcceso = verificadorAcceso;
        this.citaFotoRepositoryPort = citaFotoRepositoryPort;
        this.almacenamiento = almacenamiento;
    }

    @Override
    public FotoAlmacenada obtener(String correo, Long citaId, Long fotoId) {
        verificadorAcceso.exigirAcceso(correo, citaId);
        CitaFoto foto = citaFotoRepositoryPort.buscarPorIdYCita(fotoId, citaId)
                .orElseThrow(FotoNoEncontradaException::new);
        return almacenamiento.obtener(foto.clave()).orElseThrow(FotoNoEncontradaException::new);
    }
}
