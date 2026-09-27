package com.example.stockledger.service;

import com.example.stockledger.entity.Item;
import com.example.stockledger.entity.Movement;
import com.example.stockledger.entity.StockCount;
import com.example.stockledger.entity.Warehouse;
import com.example.stockledger.entity.enums.StockCountStatus;
import com.example.stockledger.exception.ConflictException;
import com.example.stockledger.exception.NotFoundException;
import com.example.stockledger.exception.ValidationException;
import com.example.stockledger.repository.ArrivalLayerRepository;
import com.example.stockledger.repository.ItemRepository;
import com.example.stockledger.repository.StockCountRepository;
import com.example.stockledger.repository.WarehouseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class StockCountService {

    private final StockCountRepository stockCountRepository;
    private final ArrivalLayerRepository arrivalLayerRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final MovementService movementService;

    public StockCountService(StockCountRepository stockCountRepository,
                              ArrivalLayerRepository arrivalLayerRepository,
                              ItemRepository itemRepository,
                              WarehouseRepository warehouseRepository,
                              MovementService movementService) {
        this.stockCountRepository = stockCountRepository;
        this.arrivalLayerRepository = arrivalLayerRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementService = movementService;
    }

    @Transactional
    public StockCount start(Long itemId, Long warehouseId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElseThrow(() -> new NotFoundException("Warehouse not found: " + warehouseId));

        BigDecimal systemQty = arrivalLayerRepository.sumRemainingQuantity(itemId, warehouseId);

        StockCount count = new StockCount();
        count.setItem(item);
        count.setWarehouse(warehouse);
        count.setStatus(StockCountStatus.OPEN);
        count.setSystemQuantityAtOpen(systemQty);
        count.setOpenedAt(Instant.now());

        try {
            return stockCountRepository.saveAndFlush(count);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An open stock count already exists for item " + itemId + " / warehouse " + warehouseId);
        }
    }

    @Transactional
    public StockCount complete(Long id, BigDecimal countedQuantity, String recordedBy) {
        StockCount count = stockCountRepository.findById(id).orElseThrow(() -> new NotFoundException("Stock count not found: " + id));
        if (count.getStatus() != StockCountStatus.OPEN) {
            throw new ConflictException("Stock count " + id + " is not open (status: " + count.getStatus() + ")");
        }
        if (countedQuantity == null || countedQuantity.signum() < 0) {
            throw new ValidationException("countedQuantity must be zero or positive");
        }

        count.setCountedQuantity(countedQuantity);
        BigDecimal discrepancy = countedQuantity.subtract(count.getSystemQuantityAtOpen());

        if (discrepancy.signum() != 0) {
            Movement adjustment = movementService.recordCountAdjustment(
                    count.getItem().getId(), count.getWarehouse().getId(), discrepancy,
                    "stock count adjustment", recordedBy);
            count.setAdjustmentMovementId(adjustment.getId());
        }

        count.setStatus(StockCountStatus.FINISHED);
        count.setClosedAt(Instant.now());
        return stockCountRepository.save(count);
    }

    @Transactional(readOnly = true)
    public StockCount getOrThrow(Long id) {
        return stockCountRepository.findById(id).orElseThrow(() -> new NotFoundException("Stock count not found: " + id));
    }
}
