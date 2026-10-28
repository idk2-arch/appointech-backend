package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

/** Guarda las fotos en un bucket compatible con S3; si el bucket no responde, falla con un 503 y deja un WARN. */
@Component
public class AlmacenamientoFotosS3Adapter implements AlmacenamientoFotosPort {

    private static final String ORIGEN = "AlmacenamientoFotosS3Adapter";
    private static final int HTTP_NO_ENCONTRADO = 404;

    private final S3Client s3;
    private final RegistroLogPort registroLogPort;
    private final String bucket;

    public AlmacenamientoFotosS3Adapter(S3Client s3, RegistroLogPort registroLogPort,
                                        @Value("${almacenamiento.bucket}") String bucket) {
        this.s3 = s3;
        this.registroLogPort = registroLogPort;
        this.bucket = bucket;
    }

    @Override
    public void guardar(String clave, byte[] contenido, String tipoContenido) {
        PutObjectRequest peticion = PutObjectRequest.builder()
                .bucket(bucket).key(clave).contentType(tipoContenido).build();
        try {
            colocar(peticion, contenido);
        } catch (SdkException ex) {
            throw noDisponible("guardar", ex);
        }
    }

    @Override
    public Optional<FotoAlmacenada> obtener(String clave) {
        try {
            ResponseBytes<GetObjectResponse> objeto = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(clave).build());
            return Optional.of(new FotoAlmacenada(objeto.asByteArray(), objeto.response().contentType()));
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        } catch (SdkException ex) {
            throw noDisponible("obtener", ex);
        }
    }

    @Override
    public void eliminar(String clave) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
        } catch (SdkException ex) {
            throw noDisponible("eliminar", ex);
        }
    }

    /** Crea el bucket si no existe. Nunca lanza: si el almacenamiento no responde, la aplicación sigue arrancando. */
    public void asegurarBucket() {
        try {
            crearBucketSiNoExiste();
        } catch (SdkException ex) {
            noDisponible("preparar el bucket", ex);
        }
    }

    /** Si el bucket se borró o no existía al arrancar, lo crea y reintenta una sola vez. */
    private void colocar(PutObjectRequest peticion, byte[] contenido) {
        try {
            s3.putObject(peticion, RequestBody.fromBytes(contenido));
        } catch (NoSuchBucketException ex) {
            crearBucketSiNoExiste();
            s3.putObject(peticion, RequestBody.fromBytes(contenido));
        }
    }

    private void crearBucketSiNoExiste() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            crear();
        } catch (S3Exception ex) {
            if (ex.statusCode() != HTTP_NO_ENCONTRADO) {
                throw ex;
            }
            crear();
        }
    }

    private void crear() {
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }

    /** El mensaje del registro no incluye el endpoint, las claves ni el contenido de la imagen. */
    private AlmacenamientoNoDisponibleException noDisponible(String operacion, SdkException causa) {
        registroLogPort.registrar(NivelLog.WARN, TipoLog.INTEGRACION, ORIGEN, null,
                "Almacenamiento de fotos no disponible al " + operacion + " (" + causa.getClass().getSimpleName() + ")");
        return new AlmacenamientoNoDisponibleException("El almacenamiento de fotos no está disponible", causa);
    }
}
