package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.EliminadorObjetoFoto;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.LimiteFotosCitaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.ImagenValidada;
import com.appointech.appointech_backend.domain.models.ValidadorImagenFoto;
import com.appointech.appointech_backend.domain.ports.in.AgregarFotoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/** Caso de uso: el cliente dueño adjunta hasta 3 fotos del electrodoméstico a su cita. */
@Service
public class AgregarFotoCitaUseCaseImpl implements AgregarFotoCitaUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final CitaFotoRepositoryPort citaFotoRepositoryPort;
    private final AlmacenamientoFotosPort almacenamiento;
    private final EliminadorObjetoFoto eliminadorObjeto;

    public AgregarFotoCitaUseCaseImpl(VerificadorAccesoCita verificadorAcceso,
                                      CitaFotoRepositoryPort citaFotoRepositoryPort,
                                      AlmacenamientoFotosPort almacenamiento,
                                      EliminadorObjetoFoto eliminadorObjeto) {
        this.verificadorAcceso = verificadorAcceso;
        this.citaFotoRepositoryPort = citaFotoRepositoryPort;
        this.almacenamiento = almacenamiento;
        this.eliminadorObjeto = eliminadorObjeto;
    }

    @Override
    public CitaFoto agregar(String correoCliente, Long citaId, String imagenBase64) {
        Cita cita = verificadorAcceso.exigirDuenoCliente(correoCliente, citaId);
        if (!cita.getEstado().admiteFotosDelElectrodomestico()) {
            throw new EstadoCitaInvalidoException("Solo se pueden adjuntar fotos a una cita PENDIENTE o CONFIRMADA");
        }
        if (citaFotoRepositoryPort.contarPorCita(citaId) >= CitaFoto.MAXIMO_POR_CITA) {
            throw new LimiteFotosCitaException(CitaFoto.MAXIMO_POR_CITA);
        }
        ImagenValidada imagen = ValidadorImagenFoto.validar(imagenBase64);
        String clave = "citas/" + citaId + "/" + UUID.randomUUID();
        almacenamiento.guardar(clave, imagen.contenido(), imagen.tipo().tipoContenido());
        return guardarMetadatosOLimpiar(new CitaFoto(null, citaId, clave, LocalDateTime.now()));
    }

    private CitaFoto guardarMetadatosOLimpiar(CitaFoto foto) {
        try {
            return citaFotoRepositoryPort.guardar(foto);
        } catch (RuntimeException ex) {
            eliminadorObjeto.eliminar(foto.clave());
            throw ex;
        }
    }
}
