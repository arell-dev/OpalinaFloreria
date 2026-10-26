package com.floral.opalina.persistence.jpa.springdata;

import com.floral.opalina.persistence.jpa.entity.CredencialJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CredencialJpaRepository extends JpaRepository<CredencialJpaEntity, Long> {
    @Query("select c from CredencialJpaEntity c join fetch c.cliente where lower(c.cliente.email) = lower(:email)")
    Optional<CredencialJpaEntity> buscarPorCorreo(@Param("email") String email);
}
