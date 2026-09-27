package com.example.stockledger.repository;

import com.example.stockledger.entity.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    Page<Movement> findByItem_IdOrderByOccurredAtDescIdDesc(Long itemId, Pageable pageable);

    Page<Movement> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    Optional<Movement> findByCancelsMovementId(Long originalMovementId);

    @Query("select (count(m) > 0) from Movement m where m.cancelsMovementId = :movementId")
    boolean isCancelled(@Param("movementId") Long movementId);
}
