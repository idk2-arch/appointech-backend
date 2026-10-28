package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.infrastructure.adapters.AlmacenamientoFotosS3Adapter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar, deja el bucket de fotos creado; si el almacenamiento no responde solo se deja un WARN. */
@Component
public class InicializadorAlmacenamientoFotos implements ApplicationRunner {

    private final AlmacenamientoFotosS3Adapter adaptador;

    public InicializadorAlmacenamientoFotos(AlmacenamientoFotosS3Adapter adaptador) {
        this.adaptador = adaptador;
    }

    @Override
    public void run(ApplicationArguments args) {
        adaptador.asegurarBucket();
    }
}
