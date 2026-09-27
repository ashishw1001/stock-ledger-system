package com.example.stockledger.dto.response;

import com.example.stockledger.entity.enums.DocumentType;

import java.time.Instant;
import java.util.List;

public record DocumentResponse(
        Long id, DocumentType documentType, String referenceNo, Instant createdAt, List<MovementResponse> movements
) {}
