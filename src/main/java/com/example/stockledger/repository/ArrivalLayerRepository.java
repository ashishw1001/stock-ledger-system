package com.example.stockledger.repository;

import com.example.stockledger.entity.ArrivalLayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ArrivalLayerRepository extends JpaRepository<ArrivalLayer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select al from ArrivalLayer al " +
           "where al.item.id = :itemId and al.warehouse.id = :warehouseId and al.remainingQuantity > 0 " +
           "order by al.occurredAt asc, al.id asc")
    List<ArrivalLayer> lockAvailableLayersForConsumption(@Param("itemId") Long itemId,
                                                          @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select al from ArrivalLayer al where al.movement.id = :movementId")
    Optional<ArrivalLayer> lockByMovementId(@Param("movementId") Long movementId);

    Optional<ArrivalLayer> findByMovementId(Long movementId);

    @Query("select coalesce(sum(al.remainingQuantity), 0) from ArrivalLayer al " +
           "where al.item.id = :itemId and al.warehouse.id = :warehouseId")
    BigDecimal sumRemainingQuantity(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

    @Query("select coalesce(sum(al.remainingQuantity * al.unitCost), 0) from ArrivalLayer al " +
           "where al.item.id = :itemId and al.warehouse.id = :warehouseId")
    BigDecimal sumRemainingValue(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

    @Query("select al from ArrivalLayer al where al.item.id = :itemId and al.warehouse.id = :warehouseId")
    List<ArrivalLayer> findAllForItemWarehouse(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);
}
