package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.persistence.jpa.entity.ConfiguracionTiendaJpaEntity;

import com.floral.opalina.domain.ConfiguracionTienda;
import com.floral.opalina.repository.ConfiguracionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ConfiguracionPersistenceAdapter implements ConfiguracionRepository {
    private final AccesoConfiguracionJpa configuracionJpaAcceso;

    public ConfiguracionPersistenceAdapter(AccesoConfiguracionJpa configuracionJpaAcceso) {
        this.configuracionJpaAcceso = configuracionJpaAcceso;
    }

    @Override
    public ConfiguracionTienda obtener() {
        return toDomain(configuracionJpaAcceso.obtenerParaActualizar());
    }

    @Override
    public void guardar(ConfiguracionTienda configuracion) {
        ConfiguracionTiendaJpaEntity entity = configuracionJpaAcceso.obtenerParaActualizar();
        entity.setNombre(configuracion.getNombre());
        entity.setTelefono(configuracion.getTelefono());
        entity.setEmail(configuracion.getEmail());
        entity.setDireccion(configuracion.getDireccion());
        entity.setWhatsapp(configuracion.getWhatsapp());
        entity.setZonaDelivery(configuracion.getZonaDelivery());
        entity.setHorarioGeneral(configuracion.getHorarioGeneral());
        entity.setInstagram(configuracion.getInstagram());
        entity.setFacebook(configuracion.getFacebook());
        entity.setTiktok(configuracion.getTiktok());
        entity.setCostoDelivery(configuracion.getCostoDelivery());
        entity.setDeliveryGratisDesde(configuracion.getDeliveryGratisDesde());
    }

    private ConfiguracionTienda toDomain(ConfiguracionTiendaJpaEntity entity) {
        ConfiguracionTienda config = new ConfiguracionTienda();
        config.setNombre(entity.getNombre());
        config.setTelefono(entity.getTelefono());
        config.setEmail(entity.getEmail());
        config.setDireccion(entity.getDireccion());
        config.setWhatsapp(entity.getWhatsapp());
        config.setZonaDelivery(entity.getZonaDelivery());
        config.setHorarioGeneral(entity.getHorarioGeneral());
        config.setInstagram(entity.getInstagram());
        config.setFacebook(entity.getFacebook());
        config.setTiktok(entity.getTiktok());
        config.setCostoDelivery(entity.getCostoDelivery());
        config.setDeliveryGratisDesde(entity.getDeliveryGratisDesde());
        return config;
    }
}
