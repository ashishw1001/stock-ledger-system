package com.example.stockledger.repository;

import com.example.stockledger.entity.Reservation;
import com.example.stockledger.entity.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("select coalesce(sum(r.quantity), 0) from Reservation r " +
           "where r.item.id = :itemId and r.warehouse.id = :warehouseId and r.status = 'PENDING'")
    BigDecimal sumPendingQuantity(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    java.util.Optional<Reservation> lockById(@Param("id") Long id);
}
