package com.example.stockledger.repository;

import com.example.stockledger.entity.MovementAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementAllocationRepository extends JpaRepository<MovementAllocation, Long> {
    List<MovementAllocation> findByOutMovement_Id(Long outMovementId);
}
