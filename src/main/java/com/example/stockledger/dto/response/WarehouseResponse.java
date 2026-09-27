package com.example.stockledger.dto.response;

import com.example.stockledger.entity.Warehouse;
import com.example.stockledger.entity.enums.WarehouseStatus;

import java.time.Instant;

public record WarehouseResponse(Long id, String code, String name, WarehouseStatus status, Instant createdAt) {
    public static WarehouseResponse from(Warehouse w) {
        return new WarehouseResponse(w.getId(), w.getCode(), w.getName(), w.getStatus(), w.getCreatedAt());
    }
}
