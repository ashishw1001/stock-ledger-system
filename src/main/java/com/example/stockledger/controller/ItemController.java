package com.example.stockledger.controller;

import com.example.stockledger.dto.request.CreateItemRequest;
import com.example.stockledger.dto.response.ItemResponse;
import com.example.stockledger.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(itemService.create(request)));
    }

    @GetMapping
    public List<ItemResponse> list() {
        return itemService.list().stream().map(ItemResponse::from).toList();
    }

    @PatchMapping("/{id}/disable")
    public ItemResponse disable(@PathVariable Long id) {
        return ItemResponse.from(itemService.disable(id));
    }
}
