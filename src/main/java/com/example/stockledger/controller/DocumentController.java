package com.example.stockledger.controller;

import com.example.stockledger.dto.request.CreateDocumentRequest;
import com.example.stockledger.dto.response.DocumentResponse;
import com.example.stockledger.dto.response.MovementResponse;
import com.example.stockledger.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        DocumentService.DocumentExecutionResult result = documentService.execute(request);
        var movements = result.movements().stream().map(m -> MovementResponse.from(m, false)).toList();
        DocumentResponse response = new DocumentResponse(
                result.document().getId(),
                result.document().getDocumentType(),
                result.document().getReferenceNo(),
                result.document().getCreatedAt(),
                movements);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
