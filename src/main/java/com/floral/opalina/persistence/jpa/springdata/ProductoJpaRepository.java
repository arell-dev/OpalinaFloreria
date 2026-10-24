package com.floral.opalina.persistence.jpa.springdata;

import com.floral.opalina.persistence.jpa.entity.ProductoJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoJpaRepository extends JpaRepository<ProductoJpaEntity, String> {
    List<ProductoJpaEntity> findAllByOrderByNombreAsc();

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoJpaEntity p where p.id = :id")
    Optional<ProductoJpaEntity> buscarPorIdParaActualizar(@Param("id") String id);

    @Modifying(flushAutomatically = true)
    @Query("update ProductoJpaEntity p set p.stock = p.stock + :cantidad where p.id = :id")
    int incrementarStock(@Param("id") String id, @Param("cantidad") int cantidad);

    @Modifying(flushAutomatically = true)
    @Query("update ProductoJpaEntity p set p.stock = p.stock - :cantidad "
         + "where p.id = :id and p.disponible = true and p.stock >= :cantidad")
    int descontarStockDisponible(@Param("id") String id, @Param("cantidad") int cantidad);
}
