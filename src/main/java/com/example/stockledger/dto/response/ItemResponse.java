package com.example.stockledger.dto.response;

import com.example.stockledger.entity.Item;
import com.example.stockledger.entity.enums.ItemStatus;
import com.example.stockledger.entity.enums.ItemUnit;

import java.time.Instant;

public record ItemResponse(Long id, String code, String name, ItemUnit unit, ItemStatus status, Instant createdAt) {
    public static ItemResponse from(Item i) {
        return new ItemResponse(i.getId(), i.getCode(), i.getName(), i.getUnit(), i.getStatus(), i.getCreatedAt());
    }
}
