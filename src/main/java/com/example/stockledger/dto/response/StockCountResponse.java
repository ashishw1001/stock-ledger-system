package com.example.stockledger.dto.response;

import com.example.stockledger.entity.StockCount;
import com.example.stockledger.entity.enums.StockCountStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record StockCountResponse(
        Long id, Long itemId, Long warehouseId, StockCountStatus status,
        BigDecimal systemQuantityAtOpen, BigDecimal countedQuantity, BigDecimal discrepancy,
        Long adjustmentMovementId, Instant openedAt, Instant closedAt
) {
    public static StockCountResponse from(StockCount sc) {
        BigDecimal discrepancy = sc.getCountedQuantity() != null
                ? sc.getCountedQuantity().subtract(sc.getSystemQuantityAtOpen())
                : null;
        return new StockCountResponse(sc.getId(), sc.getItem().getId(), sc.getWarehouse().getId(), sc.getStatus(),
                sc.getSystemQuantityAtOpen(), sc.getCountedQuantity(), discrepancy,
                sc.getAdjustmentMovementId(), sc.getOpenedAt(), sc.getClosedAt());
    }
}
