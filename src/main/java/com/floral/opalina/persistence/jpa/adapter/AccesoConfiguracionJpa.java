package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.persistence.jpa.entity.ConfiguracionTiendaJpaEntity;
import com.floral.opalina.persistence.jpa.springdata.ConfiguracionJpaRepository;

import com.floral.opalina.domain.ConfiguracionTienda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AccesoConfiguracionJpa {
    private final EntityManager entityManager;
    private final ConfiguracionJpaRepository configuracionJpa;

    public AccesoConfiguracionJpa(EntityManager entityManager, ConfiguracionJpaRepository configuracionJpa) {
        this.entityManager = entityManager;
        this.configuracionJpa = configuracionJpa;
    }

    public ConfiguracionTiendaJpaEntity obtenerParaActualizar() {
        ConfiguracionTiendaJpaEntity entity = configuracionJpa.buscarUnicaParaActualizar().orElse(null);
        if (entity != null) {
            return entity;
        }

        ConfiguracionTienda defaults = new ConfiguracionTienda();
        entity = new ConfiguracionTiendaJpaEntity();
        entity.setId((byte) 1);
        entity.setNombre(defaults.getNombre());
        entity.setTelefono(defaults.getTelefono());
        entity.setEmail(defaults.getEmail());
        entity.setDireccion(defaults.getDireccion());
        entity.setWhatsapp(defaults.getWhatsapp());
        entity.setZonaDelivery(defaults.getZonaDelivery());
        entity.setHorarioGeneral(defaults.getHorarioGeneral());
        entity.setInstagram(defaults.getInstagram());
        entity.setFacebook(defaults.getFacebook());
        entity.setTiktok(defaults.getTiktok());
        entity.setCostoDelivery(defaults.getCostoDelivery());
        entity.setDeliveryGratisDesde(defaults.getDeliveryGratisDesde());
        entity.setUltimoNumeroPedido(100);
        entity = configuracionJpa.save(entity);
        entityManager.flush();
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return entity;
    }
}
