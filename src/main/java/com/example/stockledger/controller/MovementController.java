package com.example.stockledger.controller;

import com.example.stockledger.dto.request.CancelMovementRequest;
import com.example.stockledger.dto.request.RecordInRequest;
import com.example.stockledger.dto.request.RecordOutRequest;
import com.example.stockledger.dto.request.RecordTransferRequest;
import com.example.stockledger.dto.response.AllocationResponse;
import com.example.stockledger.dto.response.MovementCostResponse;
import com.example.stockledger.dto.response.MovementResponse;
import com.example.stockledger.entity.Movement;
import com.example.stockledger.entity.MovementAllocation;
import com.example.stockledger.service.MovementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movements")
public class MovementController {

    private final MovementService movementService;
    private final com.example.stockledger.repository.MovementRepository movementRepository;

    public MovementController(MovementService movementService, com.example.stockledger.repository.MovementRepository movementRepository) {
        this.movementService = movementService;
        this.movementRepository = movementRepository;
    }

    @PostMapping("/in")
    public ResponseEntity<MovementResponse> recordIn(@Valid @RequestBody RecordInRequest request) {
        Movement m = movementService.recordIn(request.itemId(), request.warehouseId(), request.quantity(),
                request.unitCost(), request.occurredAt(), request.reason(), request.recordedBy(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(MovementResponse.from(m, false));
    }

    @PostMapping("/out")
    public ResponseEntity<MovementResponse> recordOut(@Valid @RequestBody RecordOutRequest request) {
        Movement m = movementService.recordOut(request.itemId(), request.warehouseId(), request.quantity(),
                request.occurredAt(), request.reason(), request.recordedBy(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(MovementResponse.from(m, false));
    }

    @PostMapping("/transfer")
    public ResponseEntity<List<MovementResponse>> recordTransfer(@Valid @RequestBody RecordTransferRequest request) {
        List<Movement> movements = movementService.recordTransfer(request.itemId(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.quantity(), request.occurredAt(), request.reason(),
                request.recordedBy(), null);
        List<MovementResponse> responses = movements.stream().map(m -> MovementResponse.from(m, false)).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MovementResponse> cancel(@PathVariable Long id, @RequestBody(required = false) CancelMovementRequest request) {
        String reason = request != null ? request.reason() : null;
        String recordedBy = request != null ? request.recordedBy() : null;
        Movement cancellation = movementService.cancelMovement(id, reason, recordedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(MovementResponse.from(cancellation, false));
    }

    @GetMapping("/{id}/cost")
    public MovementCostResponse cost(@PathVariable Long id) {
        List<MovementAllocation> allocations = movementService.getAllocations(id);
        List<AllocationResponse> allocationResponses = allocations.stream()
                .map(a -> new AllocationResponse(
                        a.getArrivalLayer().getId(),
                        a.getInMovement().getId(),
                        a.getQuantityAllocated(),
                        a.getUnitCost(),
                        a.getQuantityAllocated().multiply(a.getUnitCost())))
                .toList();
        BigDecimal totalQty = allocationResponses.stream().map(AllocationResponse::quantityAllocated).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = allocationResponses.stream().map(AllocationResponse::lineCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MovementCostResponse(id, totalQty, totalCost, allocationResponses);
    }

    @GetMapping
    public Page<MovementResponse> list(@RequestParam(required = false) Long itemId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Movement> movements = itemId != null
                ? movementRepository.findByItem_IdOrderByOccurredAtDescIdDesc(itemId, pageable)
                : movementRepository.findAllByOrderByOccurredAtDescIdDesc(pageable);
        return movements.map(m -> MovementResponse.from(m, movementService.isCancelled(m.getId())));
    }
}
