package com.example.stockledger.service;

import com.example.stockledger.dto.request.CreateItemRequest;
import com.example.stockledger.entity.Item;
import com.example.stockledger.entity.enums.ItemStatus;
import com.example.stockledger.exception.NotFoundException;
import com.example.stockledger.exception.ValidationException;
import com.example.stockledger.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Item create(CreateItemRequest request) {
        if (itemRepository.existsByCode(request.code())) {
            throw new ValidationException("Item code already exists: " + request.code());
        }
        Item item = new Item();
        item.setCode(request.code());
        item.setName(request.name());
        item.setUnit(request.unit());
        item.setStatus(ItemStatus.ACTIVE);
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<Item> list() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Item getOrThrow(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Item not found: " + id));
    }

    @Transactional
    public Item disable(Long id) {
        Item item = getOrThrow(id);
        item.setStatus(ItemStatus.DISABLED);
        return itemRepository.save(item);
    }
}
