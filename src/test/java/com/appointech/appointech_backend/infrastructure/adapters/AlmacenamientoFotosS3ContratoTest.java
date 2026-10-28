package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Corre el contrato del puerto contra el adaptador S3, con un cliente simulado que se comporta como un bucket. */
class AlmacenamientoFotosS3ContratoTest extends AlmacenamientoFotosPortContratoTest {

    @Override
    protected AlmacenamientoFotosPort crear() {
        Map<String, byte[]> contenidos = new HashMap<>();
        Map<String, String> tipos = new HashMap<>();
        S3Client cliente = mock(S3Client.class);
        when(cliente.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocacion -> {
            PutObjectRequest peticion = invocacion.getArgument(0);
            contenidos.put(peticion.key(), leer(invocacion.getArgument(1)));
            tipos.put(peticion.key(), peticion.contentType());
            return PutObjectResponse.builder().build();
        });
        when(cliente.getObjectAsBytes(any(GetObjectRequest.class))).thenAnswer(invocacion -> {
            String clave = ((GetObjectRequest) invocacion.getArgument(0)).key();
            if (!contenidos.containsKey(clave)) {
                throw NoSuchKeyException.builder().build();
            }
            return ResponseBytes.fromByteArray(
                    GetObjectResponse.builder().contentType(tipos.get(clave)).build(), contenidos.get(clave));
        });
        when(cliente.deleteObject(any(DeleteObjectRequest.class))).thenAnswer(invocacion -> {
            String clave = ((DeleteObjectRequest) invocacion.getArgument(0)).key();
            contenidos.remove(clave);
            tipos.remove(clave);
            return null;
        });
        return new AlmacenamientoFotosS3Adapter(cliente, mock(RegistroLogPort.class), "fotos-prueba");
    }

    private static byte[] leer(RequestBody cuerpo) {
        try (InputStream entrada = cuerpo.contentStreamProvider().newStream()) {
            return entrada.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
