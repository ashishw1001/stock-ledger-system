package com.example.stockledger.service;

import com.example.stockledger.dto.request.CreateDocumentRequest;
import com.example.stockledger.dto.request.DocumentLineRequest;
import com.example.stockledger.entity.Document;
import com.example.stockledger.entity.Movement;
import com.example.stockledger.entity.enums.MovementKind;
import com.example.stockledger.exception.ValidationException;
import com.example.stockledger.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MovementService movementService;

    public DocumentService(DocumentRepository documentRepository, MovementService movementService) {
        this.documentRepository = documentRepository;
        this.movementService = movementService;
    }

    @Transactional
    public DocumentExecutionResult execute(CreateDocumentRequest request) {
        Document document = new Document();
        document.setDocumentType(request.documentType());
        document.setReferenceNo(request.referenceNo());
        document = documentRepository.save(document);

        List<Movement> movements = new ArrayList<>();
        for (DocumentLineRequest line : request.lines()) {
            Movement movement = switch (line.kind()) {
                case IN -> movementService.recordIn(line.itemId(), line.warehouseId(), line.quantity(),
                        line.unitCost(), request.occurredAt(), line.reason(), request.recordedBy(), document);
                case OUT -> movementService.recordOut(line.itemId(), line.warehouseId(), line.quantity(),
                        request.occurredAt(), line.reason(), request.recordedBy(), document, MovementKind.OUT);
                default -> throw new ValidationException(
                        "Document lines only support IN or OUT (use /movements/transfer for transfers): got " + line.kind());
            };
            movements.add(movement);
        }
        return new DocumentExecutionResult(document, movements);
    }

    public record DocumentExecutionResult(Document document, List<Movement> movements) {}
}
