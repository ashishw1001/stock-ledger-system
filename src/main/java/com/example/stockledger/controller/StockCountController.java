package com.example.stockledger.controller;

import com.example.stockledger.dto.request.CompleteStockCountRequest;
import com.example.stockledger.dto.request.StartStockCountRequest;
import com.example.stockledger.dto.response.StockCountResponse;
import com.example.stockledger.service.StockCountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-counts")
public class StockCountController {

    private final StockCountService stockCountService;

    public StockCountController(StockCountService stockCountService) {
        this.stockCountService = stockCountService;
    }

    @PostMapping("/start")
    public ResponseEntity<StockCountResponse> start(@Valid @RequestBody StartStockCountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StockCountResponse.from(stockCountService.start(request.itemId(), request.warehouseId())));
    }

    @PostMapping("/{id}/complete")
    public StockCountResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteStockCountRequest request) {
        return StockCountResponse.from(stockCountService.complete(id, request.countedQuantity(), request.recordedBy()));
    }

    @GetMapping("/{id}")
    public StockCountResponse get(@PathVariable Long id) {
        return StockCountResponse.from(stockCountService.getOrThrow(id));
    }
}
