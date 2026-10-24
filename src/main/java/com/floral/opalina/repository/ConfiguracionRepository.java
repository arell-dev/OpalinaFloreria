package com.floral.opalina.repository;

import com.floral.opalina.domain.ConfiguracionTienda;

public interface ConfiguracionRepository {
    ConfiguracionTienda obtener();

    void guardar(ConfiguracionTienda configuracion);
}
