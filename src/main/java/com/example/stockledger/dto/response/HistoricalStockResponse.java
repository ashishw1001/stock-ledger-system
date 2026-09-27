package com.example.stockledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalStockResponse(
        Long itemId,
        Long warehouseId,
        Instant asOf,
        BigDecimal quantity,
        BigDecimal valuation
) {}
