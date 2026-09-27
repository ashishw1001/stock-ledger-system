package com.example.stockledger.controller;

import com.example.stockledger.dto.request.CreateWarehouseRequest;
import com.example.stockledger.dto.response.WarehouseResponse;
import com.example.stockledger.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseResponse.from(warehouseService.create(request)));
    }

    @GetMapping
    public List<WarehouseResponse> list() {
        return warehouseService.list().stream().map(WarehouseResponse::from).toList();
    }

    @PatchMapping("/{id}/disable")
    public WarehouseResponse disable(@PathVariable Long id) {
        return WarehouseResponse.from(warehouseService.disable(id));
    }
}
