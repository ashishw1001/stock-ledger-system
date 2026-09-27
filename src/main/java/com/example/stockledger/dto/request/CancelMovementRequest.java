package com.example.stockledger.dto.request;

public record CancelMovementRequest(
        String reason,
        String recordedBy
) {}
