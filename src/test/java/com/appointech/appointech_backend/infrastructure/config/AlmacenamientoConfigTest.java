package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.infrastructure.adapters.AlmacenamientoFotosS3Adapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlmacenamientoConfigTest {

    private final AlmacenamientoConfig config = new AlmacenamientoConfig();

    @Test
    void construyeElClienteConEndpointYClavesDelEntorno() {
        try (S3Client cliente = config.s3Client("http://localhost:9000", "usuario-prueba", "clave-prueba-123", "us-east-1")) {
            assertNotNull(cliente);
        }
    }

    @Test
    void sinEndpointNiClavesUsaLosValoresPorDefectoDelSdk() {
        try (S3Client cliente = config.s3Client("", "", "", "us-east-1")) {
            assertNotNull(cliente);
        }
    }

    @Test
    void elInicializadorPideAlAdaptadorAsegurarElBucketAlArrancar() {
        AlmacenamientoFotosS3Adapter adaptador = mock(AlmacenamientoFotosS3Adapter.class);

        new InicializadorAlmacenamientoFotos(adaptador).run(new DefaultApplicationArguments());

        verify(adaptador).asegurarBucket();
    }
}
