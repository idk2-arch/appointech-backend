package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.infrastructure.adapters.GroqChatCliente;
import com.appointech.appointech_backend.infrastructure.adapters.GroqPreDiagnosticoAdapter;
import com.appointech.appointech_backend.infrastructure.adapters.GroqRedactorTecnicoAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GroqConfigTest {

    private final GroqConfig config = new GroqConfig();

    private GroqChatCliente clienteSinLlave() {
        return config.groqChatCliente("", "modelo", "https://groq.prueba/openai/v1", 6000, 1024);
    }

    @Test
    void ambosPuertosComparteElMismoClienteDeGroq() {
        GroqChatCliente cliente = clienteSinLlave();

        assertInstanceOf(GroqPreDiagnosticoAdapter.class, config.preDiagnosticoPort(cliente));
        assertInstanceOf(GroqRedactorTecnicoAdapter.class, config.redactorTecnicoPort(cliente));
    }

    @Test
    void sinLlaveLosPuertosQuedanNoDisponiblesSinTocarLaRed() {
        GroqChatCliente cliente = clienteSinLlave();

        assertFalse(config.preDiagnosticoPort(cliente).generar("Fuga", null).disponible());
        assertFalse(config.redactorTecnicoPort(cliente).redactar("Fuga", null).disponible());
    }
}
