package com.floral.opalina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import com.floral.opalina.config.AlmacenamientoImagenProductoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AlmacenamientoImagenProductoServiceTest {
    private static final byte[] PNG_VALIDO = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/7ugAAAAASUVORK5CYII=");

    @TempDir
    Path directorio;

    private AlmacenamientoImagenProductoService almacenamiento;

    @BeforeEach
    void prepararAlmacenamiento() {
        AlmacenamientoImagenProductoProperties propiedades = new AlmacenamientoImagenProductoProperties();
        propiedades.setDirectorio(directorio.toString());
        almacenamiento = new AlmacenamientoImagenProductoService(propiedades);
    }

    @Test
    void guardaImagenConNombreSeguroYLaEliminaCuandoSeReemplaza() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile("imagen", "ramo.png", "image/png", PNG_VALIDO);

        String url = almacenamiento.guardar(archivo);

        assertThat(url).matches("/uploads/productos/[0-9a-f-]{36}\\.png");
        Path imagenGuardada = directorio.resolve(url.substring(AlmacenamientoImagenProductoService.URL_BASE.length()));
        assertThat(Files.readAllBytes(imagenGuardada)).containsExactly(PNG_VALIDO);

        almacenamiento.eliminarSiFueSubida(url);
        assertThat(imagenGuardada).doesNotExist();
    }

    @Test
    void rechazaContenidoQueNoCorrespondeConLaExtension() {
        MockMultipartFile archivo = new MockMultipartFile("imagen", "foto.png", "image/png", "no es una imagen".getBytes());

        assertThatThrownBy(() -> almacenamiento.guardar(archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usa una imagen JPG, PNG o WebP válida.");
    }

    @Test
    void rechazaArchivosDeMasDeCincoMegabytes() {
        byte[] contenidoGrande = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile archivo = new MockMultipartFile("imagen", "foto.png", "image/png", contenidoGrande);

        assertThatThrownBy(() -> almacenamiento.guardar(archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La imagen no puede superar los 5 MB.");
    }

    @Test
    void noEliminaRutasQueNoPertenecenAlDirectorioDeSubidas() throws IOException {
        Path archivoConservado = Files.write(directorio.resolve("fijo.png"), PNG_VALIDO);

        almacenamiento.eliminarSiFueSubida("/assets/imagenes/productos/fijo.png");

        assertThat(archivoConservado).exists();
    }
}
