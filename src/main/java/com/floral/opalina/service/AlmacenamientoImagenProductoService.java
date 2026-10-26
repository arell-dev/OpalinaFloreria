package com.floral.opalina.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import com.floral.opalina.config.AlmacenamientoImagenProductoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AlmacenamientoImagenProductoService {
    public static final String URL_BASE = "/uploads/productos/";
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(AlmacenamientoImagenProductoService.class);

    private final Path directorio;

    public AlmacenamientoImagenProductoService(AlmacenamientoImagenProductoProperties propiedadesImagenes) {
        this.directorio = Path.of(propiedadesImagenes.getDirectorio()).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen para el producto.");
        }
        if (imagen.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar los 5 MB.");
        }

        byte[] contenido;
        try {
            contenido = imagen.getBytes();
        } catch (IOException error) {
            throw new IllegalArgumentException("No se pudo leer el archivo de imagen.", error);
        }
        if (contenido.length == 0 || contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar los 5 MB y debe contener datos.");
        }

        String extension = validarFormato(imagen.getOriginalFilename(), contenido);
        String nombreSeguro = UUID.randomUUID() + "." + extension;
        Path destino = directorio.resolve(nombreSeguro).normalize();
        if (!destino.startsWith(directorio)) {
            throw new IllegalArgumentException("El nombre de la imagen no es válido.");
        }

        try {
            Files.createDirectories(directorio);
            Files.write(destino, contenido, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo guardar la imagen del producto.", error);
        }

        return URL_BASE + nombreSeguro;
    }

    public void eliminarSiFueSubida(String imagenUrl) {
        Path archivo = resolverArchivoSubido(imagenUrl);
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException error) {
            LOGGER.warn("No se pudo eliminar la imagen de producto {}.", archivo, error);
        }
    }

    private String validarFormato(String nombreOriginal, byte[] contenido) {
        String extension = extension(nombreOriginal);
        boolean jpeg = contenido.length >= 3
                && (contenido[0] & 0xff) == 0xff
                && (contenido[1] & 0xff) == 0xd8
                && (contenido[2] & 0xff) == 0xff;
        boolean png = contenido.length >= 8
                && (contenido[0] & 0xff) == 0x89
                && contenido[1] == 0x50
                && contenido[2] == 0x4e
                && contenido[3] == 0x47
                && contenido[4] == 0x0d
                && contenido[5] == 0x0a
                && contenido[6] == 0x1a
                && contenido[7] == 0x0a;
        boolean webp = contenido.length >= 12
                && contenido[0] == 'R'
                && contenido[1] == 'I'
                && contenido[2] == 'F'
                && contenido[3] == 'F'
                && contenido[8] == 'W'
                && contenido[9] == 'E'
                && contenido[10] == 'B'
                && contenido[11] == 'P';

        if (jpeg && (extension.equals("jpg") || extension.equals("jpeg"))) {
            return "jpg";
        }

        if (png && extension.equals("png")) {
            return "png";
        }

        if (webp && extension.equals("webp")) {
            return "webp";
        }

        throw new IllegalArgumentException("Usa una imagen JPG, PNG o WebP válida.");
    }

    private String extension(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }

        int punto = nombreOriginal.lastIndexOf('.');
        if (punto < 0 || punto == nombreOriginal.length() - 1) {
            return "";
        }

        return nombreOriginal.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private Path resolverArchivoSubido(String imagenUrl) {
        if (imagenUrl == null || !imagenUrl.startsWith(URL_BASE)) {
            return null;
        }

        String nombre = imagenUrl.substring(URL_BASE.length());
        if (!nombre.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) {
            return null;
        }

        Path archivo = directorio.resolve(nombre).normalize();
        return archivo.startsWith(directorio) && archivo.getParent().equals(directorio) ? archivo : null;
    }
}
