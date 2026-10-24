package com.floral.opalina.service;

import com.floral.opalina.domain.ConfiguracionTienda;
import com.floral.opalina.repository.ConfiguracionRepository;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracionService {
    private final ConfiguracionRepository repositorio;

    public ConfiguracionService(ConfiguracionRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ConfiguracionTienda obtener() {
        return repositorio.obtener();
    }

    public void guardar(ConfiguracionTienda configuracion) {
        boolean costoDeliveryInvalido = configuracion.getCostoDelivery() == null || configuracion.getCostoDelivery().signum() < 0;
        boolean montoMinimoInvalido = configuracion.getDeliveryGratisDesde() == null || configuracion.getDeliveryGratisDesde().signum() < 0;

        if (costoDeliveryInvalido || montoMinimoInvalido) {
            throw new IllegalArgumentException("Los importes de envío deben ser cero o mayores");
        }
        repositorio.guardar(configuracion);
    }
}
