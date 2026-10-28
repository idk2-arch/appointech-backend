package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.ImagenInvalidaException;

import java.util.Base64;
import java.util.Map;

/**
 * Valida con rigor la imagen que llega en base64: decodificación estricta, tamaño máximo ya decodificado y formato
 * real (JPEG, PNG o WebP) según los bytes de cabecera, sin fiarse del tipo declarado.
 */
public final class ValidadorImagenFoto {

    public static final int TAMANIO_MAXIMO_BYTES = 1024 * 1024;

    private static final String PREFIJO_DATA_URI = "data:";
    private static final String MARCADOR_BASE64 = ";base64";
    private static final int LONGITUD_MAXIMA_BASE64 = (TAMANIO_MAXIMO_BYTES / 3 + 1) * 4;
    private static final Map<String, TipoImagen> TIPOS_DECLARABLES = Map.of(
            TipoImagen.JPEG.tipoContenido(), TipoImagen.JPEG,
            TipoImagen.PNG.tipoContenido(), TipoImagen.PNG,
            TipoImagen.WEBP.tipoContenido(), TipoImagen.WEBP);
    private static final byte[] CABECERA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] CABECERA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};
    private static final int DESPLAZAMIENTO_WEBP = 8;

    private ValidadorImagenFoto() {
    }

    public static ImagenValidada validar(String imagenBase64) {
        if (imagenBase64 == null || imagenBase64.isBlank()) {
            throw new ImagenInvalidaException("La imagen es obligatoria");
        }
        TipoImagen declarado = null;
        String carga = imagenBase64;
        if (imagenBase64.startsWith(PREFIJO_DATA_URI)) {
            int coma = imagenBase64.indexOf(',');
            declarado = tipoDeclarado(imagenBase64, coma);
            carga = imagenBase64.substring(coma + 1);
        }
        byte[] bytes = decodificar(carga);
        TipoImagen real = detectarTipo(bytes);
        if (declarado != null && declarado != real) {
            throw new ImagenInvalidaException("El tipo declarado no coincide con el contenido de la imagen");
        }
        return new ImagenValidada(bytes, real);
    }

    private static TipoImagen tipoDeclarado(String entrada, int coma) {
        if (coma < 0 || !entrada.substring(0, coma).endsWith(MARCADOR_BASE64)) {
            throw new ImagenInvalidaException("La imagen debe venir en base64");
        }
        String tipo = entrada.substring(PREFIJO_DATA_URI.length(), coma - MARCADOR_BASE64.length());
        TipoImagen declarado = TIPOS_DECLARABLES.get(tipo.toLowerCase());
        if (declarado == null) {
            throw new ImagenInvalidaException("Formato no permitido: solo JPEG, PNG o WebP");
        }
        return declarado;
    }

    private static byte[] decodificar(String carga) {
        if (carga.length() > LONGITUD_MAXIMA_BASE64) {
            throw new ImagenInvalidaException("La imagen supera el tamaño máximo de 1 MB");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(carga);
        } catch (IllegalArgumentException ex) {
            throw new ImagenInvalidaException("La imagen no es un base64 válido", ex);
        }
        if (bytes.length == 0) {
            throw new ImagenInvalidaException("La imagen está vacía");
        }
        if (bytes.length > TAMANIO_MAXIMO_BYTES) {
            throw new ImagenInvalidaException("La imagen supera el tamaño máximo de 1 MB");
        }
        return bytes;
    }

    private static TipoImagen detectarTipo(byte[] bytes) {
        if (empiezaCon(bytes, CABECERA_JPEG, 0)) {
            return TipoImagen.JPEG;
        }
        if (empiezaCon(bytes, CABECERA_PNG, 0)) {
            return TipoImagen.PNG;
        }
        if (empiezaCon(bytes, RIFF, 0) && empiezaCon(bytes, WEBP, DESPLAZAMIENTO_WEBP)) {
            return TipoImagen.WEBP;
        }
        throw new ImagenInvalidaException("Formato no permitido: solo JPEG, PNG o WebP");
    }

    private static boolean empiezaCon(byte[] bytes, byte[] cabecera, int desplazamiento) {
        if (bytes.length < desplazamiento + cabecera.length) {
            return false;
        }
        for (int i = 0; i < cabecera.length; i++) {
            if (bytes[desplazamiento + i] != cabecera[i]) {
                return false;
            }
        }
        return true;
    }
}
