package com.example.stockledger.dto.request;

import com.example.stockledger.entity.enums.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateDocumentRequest(
        @NotNull DocumentType documentType,
        String referenceNo,
        Instant occurredAt,
        String recordedBy,
        @NotEmpty(message = "a document needs at least one line") @Valid List<DocumentLineRequest> lines
) {}
