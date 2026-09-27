package com.example.stockledger.dto.request;

import jakarta.validation.constraints.NotNull;

public record StartStockCountRequest(
        @NotNull Long itemId,
        @NotNull Long warehouseId
) {}
