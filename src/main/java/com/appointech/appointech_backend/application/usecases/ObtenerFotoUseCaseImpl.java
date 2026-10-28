package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.in.ObtenerFotoUseCase;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/** Caso de uso: entrega los bytes de una foto a partir de su clave (el bucket nunca se expone). */
@Service
public class ObtenerFotoUseCaseImpl implements ObtenerFotoUseCase {

    private static final Pattern FORMATO_UUID =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final AlmacenamientoFotosPort almacenamiento;

    public ObtenerFotoUseCaseImpl(AlmacenamientoFotosPort almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    @Override
    public FotoAlmacenada obtener(String clave) {
        if (clave == null || !FORMATO_UUID.matcher(clave).matches()) {
            throw new FotoNoEncontradaException();
        }
        return almacenamiento.obtener(clave).orElseThrow(FotoNoEncontradaException::new);
    }
}
