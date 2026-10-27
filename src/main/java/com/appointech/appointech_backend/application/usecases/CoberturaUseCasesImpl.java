package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.CoberturaConfigurada;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.ports.in.ActualizarCoberturaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerCoberturaUseCase;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/** Lectura y edición de la cobertura (límite único de distancia y teléfono de WhatsApp del administrador). */
@Service
public class CoberturaUseCasesImpl implements ObtenerCoberturaUseCase, ActualizarCoberturaUseCase {

    private final ConfiguracionFranjasRepositoryPort configuracionPort;

    public CoberturaUseCasesImpl(ConfiguracionFranjasRepositoryPort configuracionPort) {
        this.configuracionPort = configuracionPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CoberturaConfigurada obtener() {
        return aCobertura(configuracionActual());
    }

    @Override
    @Transactional
    public CoberturaConfigurada actualizar(Integer radioMaximoKm, String telefonoWhatsappAdmin) {
        ConfiguracionFranjas configuracion = configuracionActual();
        configuracion.cambiarRadioMaximo(radioMaximoKm);
        if (telefonoWhatsappAdmin != null) {
            configuracion.cambiarTelefonoWhatsappAdmin(telefonoWhatsappAdmin);
        }
        return aCobertura(configuracionPort.guardar(configuracion));
    }

    private CoberturaConfigurada aCobertura(ConfiguracionFranjas c) {
        return new CoberturaConfigurada(c.getRadioMaximoKm(), c.getTelefonoWhatsappAdmin());
    }

    private ConfiguracionFranjas configuracionActual() {
        return configuracionPort.obtenerActual()
                .orElseGet(() -> new ConfiguracionFranjas(null, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }
}
