package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
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
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlmacenamientoFotosS3AdapterTest {

    private static final String BUCKET = "fotos-prueba";
    private static final String CLAVE = "11111111-2222-3333-4444-555555555555";

    private S3Client s3;
    private RegistroLogPort registroLog;
    private AlmacenamientoFotosS3Adapter adaptador;

    @BeforeEach
    void preparar() {
        s3 = mock(S3Client.class);
        registroLog = mock(RegistroLogPort.class);
        adaptador = new AlmacenamientoFotosS3Adapter(s3, registroLog, BUCKET);
    }

    private void verificarWarnDeIntegracionSinDatosSensibles() {
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(registroLog).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), anyString(), eq(null), mensaje.capture());
        assertFalse(mensaje.getValue().contains(BUCKET));
        assertFalse(mensaje.getValue().contains(CLAVE));
        assertFalse(mensaje.getValue().contains("http"));
    }

    @Test
    void guardaElObjetoEnElBucketConSuTipoDeContenido() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().build());

        adaptador.guardar(CLAVE, new byte[]{1, 2}, "image/png");

        ArgumentCaptor<PutObjectRequest> peticion = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(peticion.capture(), any(RequestBody.class));
        assertEquals(BUCKET, peticion.getValue().bucket());
        assertEquals(CLAVE, peticion.getValue().key());
        assertEquals("image/png", peticion.getValue().contentType());
    }

    @Test
    void siElBucketNoExisteLoCreaYReintentaUnaVez() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(NoSuchBucketException.builder().build())
                .thenReturn(PutObjectResponse.builder().build());
        when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());

        adaptador.guardar(CLAVE, new byte[]{1}, "image/jpeg");

        verify(s3).createBucket(any(CreateBucketRequest.class));
        verify(s3, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void siElAlmacenamientoNoRespondeAlGuardarLanza503ConWarnDeIntegracion() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("connection refused http://almacenamiento:9000"));

        AlmacenamientoNoDisponibleException ex = assertThrows(AlmacenamientoNoDisponibleException.class,
                () -> adaptador.guardar(CLAVE, new byte[]{1}, "image/png"));

        assertFalse(ex.getMessage().contains("almacenamiento:9000"));
        verificarWarnDeIntegracionSinDatosSensibles();
    }

    @Test
    void obtieneBytesYTipoDeContenido() {
        GetObjectResponse respuesta = GetObjectResponse.builder().contentType("image/webp").build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(respuesta, new byte[]{7, 8}));

        FotoAlmacenada foto = adaptador.obtener(CLAVE).orElseThrow();

        assertArrayEquals(new byte[]{7, 8}, foto.contenido());
        assertEquals("image/webp", foto.tipoContenido());
        ArgumentCaptor<GetObjectRequest> peticion = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObjectAsBytes(peticion.capture());
        assertEquals(BUCKET, peticion.getValue().bucket());
        assertEquals(CLAVE, peticion.getValue().key());
    }

    @Test
    void unaClaveInexistenteDevuelveVacioSinRegistrarWarn() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertEquals(Optional.empty(), adaptador.obtener(CLAVE));
        verify(registroLog, never()).registrar(any(), any(), anyString(), any(), anyString());
    }

    @Test
    void siElAlmacenamientoNoRespondeAlObtenerLanza503() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(SdkClientException.create("timeout"));

        assertThrows(AlmacenamientoNoDisponibleException.class, () -> adaptador.obtener(CLAVE));
        verificarWarnDeIntegracionSinDatosSensibles();
    }

    @Test
    void eliminaElObjeto() {
        adaptador.eliminar(CLAVE);

        ArgumentCaptor<DeleteObjectRequest> peticion = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(peticion.capture());
        assertEquals(BUCKET, peticion.getValue().bucket());
        assertEquals(CLAVE, peticion.getValue().key());
    }

    @Test
    void siElAlmacenamientoNoRespondeAlEliminarLanza503() {
        doThrow(SdkClientException.create("timeout")).when(s3).deleteObject(any(DeleteObjectRequest.class));

        assertThrows(AlmacenamientoNoDisponibleException.class, () -> adaptador.eliminar(CLAVE));
        verificarWarnDeIntegracionSinDatosSensibles();
    }

    @Test
    void asegurarBucketNoCreaNadaSiYaExiste() {
        adaptador.asegurarBucket();

        verify(s3, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void asegurarBucketLoCreaCuandoNoExiste() {
        when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());

        adaptador.asegurarBucket();

        ArgumentCaptor<CreateBucketRequest> peticion = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3).createBucket(peticion.capture());
        assertEquals(BUCKET, peticion.getValue().bucket());
    }

    @Test
    void asegurarBucketTambienLoCreaConUnaRespuesta404Generica() {
        when(s3.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        adaptador.asegurarBucket();

        verify(s3).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void asegurarBucketNoLanzaSiElAlmacenamientoFallaYSoloDejaWarn() {
        when(s3.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("Error interno").build());

        assertDoesNotThrow(() -> adaptador.asegurarBucket());

        verify(s3, never()).createBucket(any(CreateBucketRequest.class));
        verificarWarnDeIntegracionSinDatosSensibles();
    }

    @Test
    void asegurarBucketNoLanzaSiNoHayConexion() {
        when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(SdkClientException.create("sin red"));

        assertDoesNotThrow(() -> adaptador.asegurarBucket());

        verificarWarnDeIntegracionSinDatosSensibles();
    }
}
