package com.example.stockledger.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MovementCostResponse(
        Long movementId,
        BigDecimal totalQuantity,
        BigDecimal totalCost,
        List<AllocationResponse> allocations
) {}
