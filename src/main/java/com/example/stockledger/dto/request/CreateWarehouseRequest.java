package com.example.stockledger.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWarehouseRequest(
        @NotBlank(message = "code is required") String code,
        @NotBlank(message = "name is required") String name
) {}
