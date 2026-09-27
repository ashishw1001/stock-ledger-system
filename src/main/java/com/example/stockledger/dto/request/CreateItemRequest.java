package com.example.stockledger.dto.request;

import com.example.stockledger.entity.enums.ItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateItemRequest(
        @NotBlank(message = "code is required") String code,
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "unit is required") ItemUnit unit
) {}
