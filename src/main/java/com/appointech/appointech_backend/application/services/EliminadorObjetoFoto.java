package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Component;

/** Borra un objeto del almacenamiento sin tumbar la operación: si falla, deja un WARN y sigue (objeto huérfano). */
@Component
public class EliminadorObjetoFoto {

    private static final String ORIGEN = "EliminadorObjetoFoto";

    private final AlmacenamientoFotosPort almacenamiento;
    private final RegistroLogPort registroLogPort;

    public EliminadorObjetoFoto(AlmacenamientoFotosPort almacenamiento, RegistroLogPort registroLogPort) {
        this.almacenamiento = almacenamiento;
        this.registroLogPort = registroLogPort;
    }

    public void eliminar(String clave) {
        try {
            almacenamiento.eliminar(clave);
        } catch (AlmacenamientoNoDisponibleException ex) {
            registroLogPort.registrar(NivelLog.WARN, TipoLog.INTEGRACION, ORIGEN, null,
                    "No se pudo eliminar una foto del almacenamiento; quedó un objeto huérfano (" + ex.getMessage() + ")");
        }
    }
}
