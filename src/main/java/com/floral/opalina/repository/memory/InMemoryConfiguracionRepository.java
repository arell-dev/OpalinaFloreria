package com.floral.opalina.repository.memory;

import com.floral.opalina.model.ConfiguracionTienda;
import com.floral.opalina.repository.ConfiguracionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class InMemoryConfiguracionRepository implements ConfiguracionRepository {
    private ConfiguracionTienda configuracion = new ConfiguracionTienda();

    public synchronized ConfiguracionTienda obtener() {
        return configuracion;
    }

    public synchronized void guardar(ConfiguracionTienda config) {
        configuracion = config;
    }
}
