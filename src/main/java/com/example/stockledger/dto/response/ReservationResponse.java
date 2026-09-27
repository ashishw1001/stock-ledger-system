package com.example.stockledger.dto.response;

import com.example.stockledger.entity.Reservation;
import com.example.stockledger.entity.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(
        Long id, Long itemId, Long warehouseId, BigDecimal quantity,
        String referenceNo, ReservationStatus status, Instant createdAt, Instant updatedAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getItem().getId(), r.getWarehouse().getId(), r.getQuantity(),
                r.getReferenceNo(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
