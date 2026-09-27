package com.example.stockledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordOutRequest(
        @NotNull Long itemId,
        @NotNull Long warehouseId,
        @NotNull @DecimalMin(value = "0.0001", message = "quantity must be positive") BigDecimal quantity,
        Instant occurredAt,
        String reason,
        String recordedBy,
        Long documentId
) {}
