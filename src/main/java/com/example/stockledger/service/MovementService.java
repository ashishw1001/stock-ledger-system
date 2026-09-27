package com.example.stockledger.service;

import com.example.stockledger.entity.*;
import com.example.stockledger.entity.enums.MovementKind;
import com.example.stockledger.exception.ConflictException;
import com.example.stockledger.exception.InsufficientStockException;
import com.example.stockledger.exception.NotFoundException;
import com.example.stockledger.exception.ValidationException;
import com.example.stockledger.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final ArrivalLayerRepository arrivalLayerRepository;
    private final MovementAllocationRepository allocationRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;

    public MovementService(MovementRepository movementRepository,
                            ArrivalLayerRepository arrivalLayerRepository,
                            MovementAllocationRepository allocationRepository,
                            ItemRepository itemRepository,
                            WarehouseRepository warehouseRepository) {
        this.movementRepository = movementRepository;
        this.arrivalLayerRepository = arrivalLayerRepository;
        this.allocationRepository = allocationRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public Movement recordIn(Long itemId, Long warehouseId, BigDecimal quantity, BigDecimal unitCost,
                              Instant occurredAt, String reason, String recordedBy, Document document) {
        Item item = requireActiveItem(itemId);
        Warehouse warehouse = requireActiveWarehouse(warehouseId);
        requirePositive(quantity, "quantity");
        if (unitCost == null || unitCost.signum() < 0) {
            throw new ValidationException("unitCost must be zero or positive");
        }

        Movement movement = newMovement(item, warehouse, MovementKind.IN, quantity, unitCost,
                occurredAt, reason, recordedBy, document);
        movement = movementRepository.save(movement);

        ArrivalLayer layer = new ArrivalLayer();
        layer.setMovement(movement);
        layer.setItem(item);
        layer.setWarehouse(warehouse);
        layer.setInitialQuantity(quantity);
        layer.setRemainingQuantity(quantity);
        layer.setUnitCost(unitCost);
        layer.setOccurredAt(movement.getOccurredAt());
        arrivalLayerRepository.save(layer);

        return movement;
    }

    @Transactional
    public Movement recordOut(Long itemId, Long warehouseId, BigDecimal quantity,
                               Instant occurredAt, String reason, String recordedBy, Document document) {
        return recordOut(itemId, warehouseId, quantity, occurredAt, reason, recordedBy, document, MovementKind.OUT);
    }

    @Transactional
    public Movement recordOut(Long itemId, Long warehouseId, BigDecimal quantity, Instant occurredAt,
                               String reason, String recordedBy, Document document, MovementKind kind) {
        Item item = requireActiveItem(itemId);
        Warehouse warehouse = requireActiveWarehouse(warehouseId);
        requirePositive(quantity, "quantity");

        Movement movement = newMovement(item, warehouse, kind, quantity, null,
                occurredAt, reason, recordedBy, document);
        movement = movementRepository.save(movement);

        allocateFifo(movement, item, warehouse, quantity);
        return movement;
    }

    private void allocateFifo(Movement consumingMovement, Item item, Warehouse warehouse, BigDecimal quantity) {
        List<ArrivalLayer> layers = arrivalLayerRepository.lockAvailableLayersForConsumption(item.getId(), warehouse.getId());

        BigDecimal totalAvailable = layers.stream().map(ArrivalLayer::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAvailable.compareTo(quantity) < 0) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for item %d in warehouse %d: requested %s, available %s",
                    item.getId(), warehouse.getId(), quantity, totalAvailable));
        }

        BigDecimal remainingToAllocate = quantity;
        for (ArrivalLayer layer : layers) {
            if (remainingToAllocate.signum() <= 0) break;

            BigDecimal takeFromLayer = layer.getRemainingQuantity().min(remainingToAllocate);
            layer.setRemainingQuantity(layer.getRemainingQuantity().subtract(takeFromLayer));
            arrivalLayerRepository.save(layer);

            MovementAllocation allocation = new MovementAllocation();
            allocation.setOutMovement(consumingMovement);
            allocation.setInMovement(layer.getMovement());
            allocation.setArrivalLayer(layer);
            allocation.setQuantityAllocated(takeFromLayer);
            allocation.setUnitCost(layer.getUnitCost());
            allocationRepository.save(allocation);

            remainingToAllocate = remainingToAllocate.subtract(takeFromLayer);
        }
    }

    @Transactional
    public List<Movement> recordTransfer(Long itemId, Long sourceWarehouseId, Long destinationWarehouseId,
                                          BigDecimal quantity, Instant occurredAt, String reason,
                                          String recordedBy, Document document) {
        if (sourceWarehouseId.equals(destinationWarehouseId)) {
            throw new ValidationException("source and destination warehouse must differ");
        }
        Item item = requireActiveItem(itemId);
        Warehouse source = requireActiveWarehouse(sourceWarehouseId);
        Warehouse destination = requireActiveWarehouse(destinationWarehouseId);
        requirePositive(quantity, "quantity");

        Movement transferOut = newMovement(item, source, MovementKind.TRANSFER_OUT, quantity, null,
                occurredAt, reason, recordedBy, document);
        transferOut = movementRepository.save(transferOut);
        allocateFifo(transferOut, item, source, quantity);

        List<MovementAllocation> allocations = allocationRepository.findByOutMovement_Id(transferOut.getId());

        List<Movement> results = new ArrayList<>();
        results.add(transferOut);

        for (MovementAllocation allocation : allocations) {
            Movement transferIn = newMovement(item, destination, MovementKind.TRANSFER_IN,
                    allocation.getQuantityAllocated(), allocation.getUnitCost(),
                    occurredAt, reason, recordedBy, document);
            transferIn = movementRepository.save(transferIn);

            ArrivalLayer destLayer = new ArrivalLayer();
            destLayer.setMovement(transferIn);
            destLayer.setItem(item);
            destLayer.setWarehouse(destination);
            destLayer.setInitialQuantity(allocation.getQuantityAllocated());
            destLayer.setRemainingQuantity(allocation.getQuantityAllocated());
            destLayer.setUnitCost(allocation.getUnitCost());
            destLayer.setOccurredAt(transferIn.getOccurredAt());
            arrivalLayerRepository.save(destLayer);

            results.add(transferIn);
        }
        return results;
    }

    @Transactional
    public Movement recordCountAdjustment(Long itemId, Long warehouseId, BigDecimal discrepancy,
                                           String reason, String recordedBy) {
        Item item = requireActiveItem(itemId);
        Warehouse warehouse = requireActiveWarehouse(warehouseId);
        if (discrepancy == null || discrepancy.signum() == 0) {
            throw new ValidationException("discrepancy must be non-zero");
        }

        Instant occurredAt = Instant.now();

        if (discrepancy.signum() > 0) {
            BigDecimal avgCost = weightedAverageCost(itemId, warehouseId);
            Movement movement = newMovement(item, warehouse, MovementKind.COUNT_ADJUSTMENT, discrepancy, avgCost,
                    occurredAt, reason, recordedBy, null);
            movement = movementRepository.save(movement);

            ArrivalLayer layer = new ArrivalLayer();
            layer.setMovement(movement);
            layer.setItem(item);
            layer.setWarehouse(warehouse);
            layer.setInitialQuantity(discrepancy);
            layer.setRemainingQuantity(discrepancy);
            layer.setUnitCost(avgCost);
            layer.setOccurredAt(occurredAt);
            arrivalLayerRepository.save(layer);
            return movement;
        } else {
            BigDecimal shortfall = discrepancy.negate();
            Movement movement = newMovement(item, warehouse, MovementKind.COUNT_ADJUSTMENT, discrepancy, null,
                    occurredAt, reason, recordedBy, null);
            movement.setQuantity(discrepancy.setScale(4, RoundingMode.HALF_UP));
            movement = movementRepository.save(movement);
            allocateFifo(movement, item, warehouse, shortfall);
            return movement;
        }
    }

    private BigDecimal weightedAverageCost(Long itemId, Long warehouseId) {
        List<ArrivalLayer> layers = arrivalLayerRepository.findAllForItemWarehouse(itemId, warehouseId);
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (ArrivalLayer l : layers) {
            totalQty = totalQty.add(l.getRemainingQuantity());
            totalValue = totalValue.add(l.getRemainingQuantity().multiply(l.getUnitCost()));
        }
        if (totalQty.signum() == 0) return BigDecimal.ZERO;
        return totalValue.divide(totalQty, 4, RoundingMode.HALF_UP);
    }

    @Transactional
    public Movement cancelMovement(Long movementId, String reason, String recordedBy) {
        Movement original = movementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Movement not found: " + movementId));

        if (original.getKind().isCancellation()) {
            throw new ValidationException("Cannot cancel a cancellation movement (id=" + movementId + ")");
        }
        if (original.getKind() == MovementKind.COUNT_ADJUSTMENT) {
            throw new ValidationException("Stock count adjustments cannot be cancelled directly");
        }
        if (movementRepository.isCancelled(movementId)) {
            throw new ConflictException("Movement " + movementId + " has already been cancelled");
        }

        Movement cancellation = switch (original.getKind()) {
            case IN -> cancelIn(original, reason, recordedBy, MovementKind.CANCEL_IN);
            case OUT -> cancelOut(original, reason, recordedBy, MovementKind.CANCEL_OUT);
            case TRANSFER_OUT -> cancelOut(original, reason, recordedBy, MovementKind.CANCEL_TRANSFER_OUT);
            case TRANSFER_IN -> cancelIn(original, reason, recordedBy, MovementKind.CANCEL_TRANSFER_IN);
            default -> throw new ValidationException("Movement kind " + original.getKind() + " is not cancellable");
        };

        return movementRepository.save(cancellation);
    }

    private Movement cancelIn(Movement original, String reason, String recordedBy, MovementKind cancelKind) {
        ArrivalLayer layer = arrivalLayerRepository.lockByMovementId(original.getId())
                .orElseThrow(() -> new ConflictException("Arrival layer missing for movement " + original.getId()));
        if (layer.getRemainingQuantity().compareTo(layer.getInitialQuantity()) != 0) {
            throw new ConflictException("Cannot cancel movement " + original.getId() +
                    ": some of its stock has already been consumed by later issues");
        }
        layer.setRemainingQuantity(BigDecimal.ZERO);
        arrivalLayerRepository.save(layer);

        return buildCancellationMovement(original, cancelKind, reason, recordedBy);
    }

    private Movement cancelOut(Movement original, String reason, String recordedBy, MovementKind cancelKind) {
        List<MovementAllocation> allocations = allocationRepository.findByOutMovement_Id(original.getId());
        if (allocations.isEmpty()) {
            throw new ConflictException("No allocations found for movement " + original.getId());
        }
        for (MovementAllocation allocation : allocations) {
            ArrivalLayer layer = arrivalLayerRepository.lockByMovementId(allocation.getInMovement().getId())
                    .orElseThrow(() -> new ConflictException("Arrival layer missing for movement " + allocation.getInMovement().getId()));
            BigDecimal restored = layer.getRemainingQuantity().add(allocation.getQuantityAllocated());
            if (restored.compareTo(layer.getInitialQuantity()) > 0) {
                throw new ConflictException("Restoring allocation would exceed original layer quantity");
            }
            layer.setRemainingQuantity(restored);
            arrivalLayerRepository.save(layer);
        }
        return buildCancellationMovement(original, cancelKind, reason, recordedBy);
    }

    private Movement buildCancellationMovement(Movement original, MovementKind cancelKind, String reason, String recordedBy) {
        Movement cancellation = new Movement();
        cancellation.setItem(original.getItem());
        cancellation.setWarehouse(original.getWarehouse());
        cancellation.setDocument(original.getDocument());
        cancellation.setKind(cancelKind);
        cancellation.setQuantity(original.getQuantity());
        cancellation.setUnitPrice(original.getUnitPrice());
        cancellation.setSnapshotItemName(original.getSnapshotItemName());
        cancellation.setSnapshotItemUnit(original.getSnapshotItemUnit());
        cancellation.setReason(reason != null ? reason : "Cancellation of movement " + original.getId());
        cancellation.setRecordedBy(recordedBy);
        cancellation.setOccurredAt(Instant.now());
        cancellation.setCancelsMovementId(original.getId());
        return cancellation;
    }

    @Transactional(readOnly = true)
    public List<MovementAllocation> getAllocations(Long movementId) {
        return allocationRepository.findByOutMovement_Id(movementId);
    }

    @Transactional(readOnly = true)
    public boolean isCancelled(Long movementId) {
        return movementRepository.isCancelled(movementId);
    }

    @Transactional(readOnly = true)
    public Movement getOrThrow(Long id) {
        return movementRepository.findById(id).orElseThrow(() -> new NotFoundException("Movement not found: " + id));
    }

    private Movement newMovement(Item item, Warehouse warehouse, MovementKind kind, BigDecimal quantity,
                                  BigDecimal unitPrice, Instant occurredAt, String reason, String recordedBy,
                                  Document document) {
        Movement m = new Movement();
        m.setItem(item);
        m.setWarehouse(warehouse);
        m.setKind(kind);
        m.setQuantity(quantity.setScale(4, RoundingMode.HALF_UP));
        m.setUnitPrice(unitPrice != null ? unitPrice.setScale(4, RoundingMode.HALF_UP) : null);
        m.setSnapshotItemName(item.getName());
        m.setSnapshotItemUnit(item.getUnit().name());
        m.setReason(reason);
        m.setRecordedBy(recordedBy);
        m.setOccurredAt(occurredAt != null ? occurredAt : Instant.now());
        m.setDocument(document);
        return m;
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException(field + " must be positive");
        }
    }

    private Item requireActiveItem(Long id) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Item not found: " + id));
        if (item.getStatus() != com.example.stockledger.entity.enums.ItemStatus.ACTIVE) {
            throw new ValidationException("Item " + id + " is disabled");
        }
        return item;
    }

    private Warehouse requireActiveWarehouse(Long id) {
        Warehouse w = warehouseRepository.findById(id).orElseThrow(() -> new NotFoundException("Warehouse not found: " + id));
        if (w.getStatus() != com.example.stockledger.entity.enums.WarehouseStatus.ACTIVE) {
            throw new ValidationException("Warehouse " + id + " is disabled");
        }
        return w;
    }
}
