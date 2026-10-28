package com.appointech.appointech_backend.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Cliente del almacenamiento de objetos compatible con S3. Todo sale del entorno (ALMACENAMIENTO_*): cambiar de
 * MinIO a otro proveedor es solo cambiar variables. Con el endpoint vacío usa el de AWS.
 */
@Configuration
public class AlmacenamientoConfig {

    private static final Duration TIEMPO_TOTAL = Duration.ofSeconds(10);
    private static final Duration TIEMPO_POR_INTENTO = Duration.ofSeconds(4);

    @Bean(destroyMethod = "close")
    public S3Client s3Client(@Value("${almacenamiento.endpoint:}") String endpoint,
                             @Value("${almacenamiento.access-key:}") String accessKey,
                             @Value("${almacenamiento.secret-key:}") String secretKey,
                             @Value("${almacenamiento.region}") String region) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .forcePathStyle(true)
                .credentialsProvider(credenciales(accessKey, secretKey))
                .overrideConfiguration(c -> c.apiCallTimeout(TIEMPO_TOTAL).apiCallAttemptTimeout(TIEMPO_POR_INTENTO));
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    /** Sin claves explícitas se usa la cadena por defecto del SDK (rol de la instancia, etc.). */
    private static AwsCredentialsProvider credenciales(String accessKey, String secretKey) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            return DefaultCredentialsProvider.builder().build();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
