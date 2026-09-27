package com.example.stockledger.dto.response;

import java.math.BigDecimal;

public record CurrentStockResponse(
        Long itemId,
        Long warehouseId,
        BigDecimal inStock,
        BigDecimal reserved,
        BigDecimal available,
        BigDecimal valuation
) {}
