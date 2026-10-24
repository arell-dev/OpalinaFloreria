package com.floral.opalina.persistence.jpa.springdata;

import com.floral.opalina.persistence.jpa.entity.PedidoJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, Long> {
    List<PedidoJpaEntity> findAllByOrderByCreadoEnDesc();

    Optional<PedidoJpaEntity> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoJpaEntity p where p.codigo = :codigo")
    Optional<PedidoJpaEntity> buscarPorCodigoParaActualizar(@Param("codigo") String codigo);
}
