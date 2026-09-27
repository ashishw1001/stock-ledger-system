package com.example.stockledger.service;

import com.example.stockledger.entity.Item;
import com.example.stockledger.entity.Warehouse;
import com.example.stockledger.entity.enums.ItemUnit;
import com.example.stockledger.repository.ItemRepository;
import com.example.stockledger.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SeedService {

    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final MovementService movementService;

    public SeedService(ItemRepository itemRepository, WarehouseRepository warehouseRepository, MovementService movementService) {
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementService = movementService;
    }

    @Transactional
    public Map<String, Object> seed() {
        Map<String, Object> summary = new LinkedHashMap<>();

        Item widget = itemRepository.findByCode("WIDGET-A").orElseGet(() -> {
            Item i = new Item();
            i.setCode("WIDGET-A");
            i.setName("Widget A");
            i.setUnit(ItemUnit.PIECES);
            return itemRepository.save(i);
        });

        Item steel = itemRepository.findByCode("STEEL-COIL").orElseGet(() -> {
            Item i = new Item();
            i.setCode("STEEL-COIL");
            i.setName("Steel Coil");
            i.setUnit(ItemUnit.KG);
            return itemRepository.save(i);
        });

        Warehouse main = warehouseRepository.findByCode("MAIN").orElseGet(() -> {
            Warehouse w = new Warehouse();
            w.setCode("MAIN");
            w.setName("Main Warehouse");
            return warehouseRepository.save(w);
        });

        Warehouse north = warehouseRepository.findByCode("NORTH").orElseGet(() -> {
            Warehouse w = new Warehouse();
            w.setCode("NORTH");
            w.setName("North Depot");
            return warehouseRepository.save(w);
        });

        Instant now = Instant.now();
        movementService.recordIn(widget.getId(), main.getId(), new BigDecimal("100"), new BigDecimal("10.00"),
                now.minus(30, ChronoUnit.DAYS), "seed arrival", "seed-script", null);
        movementService.recordIn(widget.getId(), main.getId(), new BigDecimal("100"), new BigDecimal("12.00"),
                now.minus(20, ChronoUnit.DAYS), "seed arrival", "seed-script", null);
        movementService.recordIn(steel.getId(), main.getId(), new BigDecimal("500"), new BigDecimal("3.50"),
                now.minus(10, ChronoUnit.DAYS), "seed arrival", "seed-script", null);

        summary.put("items", itemRepository.count());
        summary.put("warehouses", warehouseRepository.count());
        summary.put("message", "Seed data loaded: WIDGET-A and STEEL-COIL in MAIN warehouse, with two arrival batches for WIDGET-A (100 @ 10.00, 100 @ 12.00)");
        return summary;
    }
}
