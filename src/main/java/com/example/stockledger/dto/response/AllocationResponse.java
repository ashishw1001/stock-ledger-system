package com.example.stockledger.dto.response;

import java.math.BigDecimal;

public record AllocationResponse(
        Long arrivalLayerId,
        Long sourceInMovementId,
        BigDecimal quantityAllocated,
        BigDecimal unitCost,
        BigDecimal lineCost
) {}
