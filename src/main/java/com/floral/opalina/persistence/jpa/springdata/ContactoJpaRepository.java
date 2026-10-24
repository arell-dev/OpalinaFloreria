package com.floral.opalina.persistence.jpa.springdata;

import com.floral.opalina.persistence.jpa.entity.ConsultaContactoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoJpaRepository extends JpaRepository<ConsultaContactoJpaEntity, Long> {
}
