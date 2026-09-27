package com.example.stockledger.dto.request;

import com.example.stockledger.entity.enums.MovementKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DocumentLineRequest(
        @NotNull Long itemId,
        @NotNull Long warehouseId,
        @NotNull MovementKind kind,
        @NotNull @DecimalMin(value = "0.0001", message = "quantity must be positive") BigDecimal quantity,
        BigDecimal unitCost,
        String reason
) {}
