package com.floral.opalina.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {
    private final AccesoInterceptor accesoInterceptor;
    private final String directorioImagenesProductos;

    public ConfiguracionWeb(AccesoInterceptor accesoInterceptor, AlmacenamientoImagenProductoProperties propiedadesImagenes) {
        this.accesoInterceptor = accesoInterceptor;
        this.directorioImagenesProductos = propiedadesImagenes.getDirectorio();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accesoInterceptor)
                .addPathPatterns("/cuenta/**", "/pedido", "/pedido/**", "/admin", "/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(directorioImagenesProductos).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/productos/**").addResourceLocations(location);
    }
}
