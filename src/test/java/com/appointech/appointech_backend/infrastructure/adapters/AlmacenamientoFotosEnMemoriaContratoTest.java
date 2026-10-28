package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;

class AlmacenamientoFotosEnMemoriaContratoTest extends AlmacenamientoFotosPortContratoTest {

    @Override
    protected AlmacenamientoFotosPort crear() {
        return new AlmacenamientoFotosEnMemoria();
    }
}
