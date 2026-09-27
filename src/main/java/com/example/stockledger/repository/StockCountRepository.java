package com.example.stockledger.repository;

import com.example.stockledger.entity.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountRepository extends JpaRepository<StockCount, Long> {
}
