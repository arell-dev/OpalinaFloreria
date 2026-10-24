package com.floral.opalina.persistence.jpa.springdata;

import com.floral.opalina.persistence.jpa.entity.ConfiguracionTiendaJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ConfiguracionJpaRepository extends JpaRepository<ConfiguracionTiendaJpaEntity, Byte> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracionTiendaJpaEntity c where c.id = 1")
    Optional<ConfiguracionTiendaJpaEntity> buscarUnicaParaActualizar();
}
