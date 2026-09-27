package com.example.stockledger.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompleteStockCountRequest(
        @NotNull BigDecimal countedQuantity,
        String recordedBy
) {}
