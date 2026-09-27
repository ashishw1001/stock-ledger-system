package com.example.stockledger.dto.response;

import com.example.stockledger.entity.Movement;
import com.example.stockledger.entity.enums.MovementKind;

import java.math.BigDecimal;
import java.time.Instant;

public record MovementResponse(
        Long id,
        Long documentId,
        Long itemId,
        String itemName,
        Long warehouseId,
        MovementKind kind,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String reason,
        String recordedBy,
        Instant occurredAt,
        Instant createdAt,
        Long cancelsMovementId,
        boolean cancelled
) {
    public static MovementResponse from(Movement m, boolean cancelled) {
        return new MovementResponse(
                m.getId(),
                m.getDocument() != null ? m.getDocument().getId() : null,
                m.getItem().getId(),
                m.getSnapshotItemName(),
                m.getWarehouse().getId(),
                m.getKind(),
                m.getQuantity(),
                m.getUnitPrice(),
                m.getReason(),
                m.getRecordedBy(),
                m.getOccurredAt(),
                m.getCreatedAt(),
                m.getCancelsMovementId(),
                cancelled
        );
    }
}
