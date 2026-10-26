package com.floral.opalina.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opalina.almacenamiento.productos")
public class AlmacenamientoImagenProductoProperties {
    /** Directorio local donde se guardan las imágenes subidas desde administración. */
    private String directorio = "./uploads/productos";

    public String getDirectorio() {
        return directorio;
    }

    public void setDirectorio(String directorio) {
        this.directorio = directorio;
    }
}
