package com.example.stockledger.service;

import com.example.stockledger.dto.request.CreateWarehouseRequest;
import com.example.stockledger.entity.Warehouse;
import com.example.stockledger.entity.enums.WarehouseStatus;
import com.example.stockledger.exception.NotFoundException;
import com.example.stockledger.exception.ValidationException;
import com.example.stockledger.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public Warehouse create(CreateWarehouseRequest request) {
        if (warehouseRepository.existsByCode(request.code())) {
            throw new ValidationException("Warehouse code already exists: " + request.code());
        }
        Warehouse w = new Warehouse();
        w.setCode(request.code());
        w.setName(request.name());
        w.setStatus(WarehouseStatus.ACTIVE);
        return warehouseRepository.save(w);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> list() {
        return warehouseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Warehouse getOrThrow(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new NotFoundException("Warehouse not found: " + id));
    }

    @Transactional
    public Warehouse disable(Long id) {
        Warehouse w = getOrThrow(id);
        w.setStatus(WarehouseStatus.DISABLED);
        return warehouseRepository.save(w);
    }
}
