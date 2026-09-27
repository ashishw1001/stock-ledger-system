package com.example.stockledger.controller;

import com.example.stockledger.dto.response.CurrentStockResponse;
import com.example.stockledger.dto.response.HistoricalStockResponse;
import com.example.stockledger.service.StockService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/current")
    public CurrentStockResponse current(@RequestParam Long itemId, @RequestParam Long warehouseId) {
        return stockService.getCurrentStock(itemId, warehouseId);
    }

    @GetMapping("/historical")
    public HistoricalStockResponse historical(@RequestParam Long itemId, @RequestParam Long warehouseId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return stockService.getHistoricalStock(itemId, warehouseId, at);
    }
}
