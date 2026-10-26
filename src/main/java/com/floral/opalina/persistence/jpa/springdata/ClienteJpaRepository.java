package com.floral.opalina.persistence.jpa.springdata;

import com.floral.opalina.persistence.jpa.entity.ClienteJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, Long> {
    List<ClienteJpaEntity> findAllByOrderByNombreAsc();

    Optional<ClienteJpaEntity> findByEmailIgnoreCase(String email);
}
