package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.ImagenInvalidaException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorImagenFotoTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] conTamanio(byte[] cabecera, int tamanio) {
        byte[] bytes = new byte[tamanio];
        System.arraycopy(cabecera, 0, bytes, 0, cabecera.length);
        return bytes;
    }

    private static String mensaje(String entrada) {
        return assertThrows(ImagenInvalidaException.class, () -> ValidadorImagenFoto.validar(entrada)).getMessage();
    }

    @Test
    void aceptaJpegPngYWebpConPrefijoDataUri() {
        assertEquals(TipoImagen.JPEG, ValidadorImagenFoto.validar("data:image/jpeg;base64," + b64(JPEG)).tipo());
        assertEquals(TipoImagen.PNG, ValidadorImagenFoto.validar("data:image/png;base64," + b64(PNG)).tipo());
        assertEquals(TipoImagen.WEBP, ValidadorImagenFoto.validar("data:image/webp;base64," + b64(WEBP)).tipo());
    }

    @Test
    void aceptaBase64SinPrefijoYDetectaElTipoPorLosBytes() {
        ImagenValidada imagen = ValidadorImagenFoto.validar(b64(PNG));

        assertEquals(TipoImagen.PNG, imagen.tipo());
        assertEquals("image/png", imagen.tipo().tipoContenido());
        assertArrayEquals(PNG, imagen.contenido());
    }

    @Test
    void elTipoDeclaradoNoDistingueMayusculas() {
        assertEquals(TipoImagen.JPEG, ValidadorImagenFoto.validar("data:IMAGE/JPEG;base64," + b64(JPEG)).tipo());
    }

    @Test
    void aceptaExactamenteUnMegabyteYRechazaUnByteMas() {
        assertEquals(ValidadorImagenFoto.TAMANIO_MAXIMO_BYTES,
                ValidadorImagenFoto.validar(b64(conTamanio(JPEG, ValidadorImagenFoto.TAMANIO_MAXIMO_BYTES))).contenido().length);

        assertEquals("La imagen supera el tamaño máximo de 1 MB",
                mensaje(b64(conTamanio(JPEG, ValidadorImagenFoto.TAMANIO_MAXIMO_BYTES + 1))));
    }

    @Test
    void rechazaSinDecodificarUnaCargaDemasiadoLarga() {
        char[] enorme = new char[2_000_000];
        Arrays.fill(enorme, 'A');

        assertEquals("La imagen supera el tamaño máximo de 1 MB", mensaje(new String(enorme)));
    }

    @Test
    void rechazaEntradaNulaOVacia() {
        assertEquals("La imagen es obligatoria", mensaje(null));
        assertEquals("La imagen es obligatoria", mensaje("   "));
    }

    @Test
    void rechazaBase64Invalido() {
        assertEquals("La imagen no es un base64 válido", mensaje("esto no es base64!!"));
        assertEquals("La imagen no es un base64 válido", mensaje("data:image/png;base64,@@@@"));
    }

    @Test
    void rechazaBase64ConSaltosDeLineaOEspacios() {
        String conSalto = b64(PNG).substring(0, 8) + "\n" + b64(PNG).substring(8);

        assertEquals("La imagen no es un base64 válido", mensaje(conSalto));
        assertEquals("La imagen no es un base64 válido", mensaje(" " + b64(PNG)));
    }

    @Test
    void rechazaBase64ConRellenoIncorrecto() {
        assertEquals("La imagen no es un base64 válido", mensaje(b64(PNG).replace("=", "")  + "="));
    }

    @Test
    void rechazaUnaCargaVaciaTrasElPrefijo() {
        assertEquals("La imagen está vacía", mensaje("data:image/png;base64,"));
    }

    @Test
    void rechazaSvgDeclarado() {
        String svg = b64("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));

        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje("data:image/svg+xml;base64," + svg));
    }

    @Test
    void rechazaSvgDisfrazadoDePngPorqueLosBytesNoSonPng() {
        String svg = b64("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));

        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje("data:image/png;base64," + svg));
        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje(svg));
    }

    @Test
    void rechazaUnTipoDeclaradoQueNoCoincideConLaCabeceraReal() {
        String mensaje = "El tipo declarado no coincide con el contenido de la imagen";

        assertEquals(mensaje, mensaje("data:image/jpeg;base64," + b64(PNG)));
        assertEquals(mensaje, mensaje("data:image/png;base64," + b64(WEBP)));
    }

    @Test
    void rechazaGifYOtrosFormatos() {
        byte[] gif = "GIF89a....".getBytes(StandardCharsets.US_ASCII);

        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje(b64(gif)));
        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje("data:image/gif;base64," + b64(gif)));
    }

    @Test
    void rechazaRiffQueNoEsWebpYArchivosMasCortosQueLaCabecera() {
        byte[] wav = {'R', 'I', 'F', 'F', 24, 0, 0, 0, 'W', 'A', 'V', 'E'};
        byte[] riffCorto = {'R', 'I', 'F', 'F', 24, 0, 0, 0, 'W', 'E'};

        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje(b64(wav)));
        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje(b64(riffCorto)));
        assertEquals("Formato no permitido: solo JPEG, PNG o WebP", mensaje(b64(new byte[]{(byte) 0xFF, (byte) 0xD8})));
    }

    @Test
    void rechazaUnDataUriSinMarcadorBase64OSinComa() {
        assertEquals("La imagen debe venir en base64", mensaje("data:image/png," + b64(PNG)));
        assertEquals("La imagen debe venir en base64", mensaje("data:image/png;base64" + b64(PNG)));
    }

    @Test
    void laImagenValidadaYLaFotoAlmacenadaNoExponenSuArregloInterno() {
        byte[] original = PNG.clone();
        ImagenValidada imagen = new ImagenValidada(original, TipoImagen.PNG);
        original[0] = 0;
        imagen.contenido()[1] = 0;

        assertArrayEquals(PNG, imagen.contenido());

        FotoAlmacenada foto = new FotoAlmacenada(original, "image/png");
        foto.contenido()[2] = 0;
        assertArrayEquals(original, foto.contenido());
    }
}
