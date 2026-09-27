package com.example.stockledger.service;

import com.example.stockledger.dto.request.CreateReservationRequest;
import com.example.stockledger.entity.Item;
import com.example.stockledger.entity.Movement;
import com.example.stockledger.entity.Reservation;
import com.example.stockledger.entity.Warehouse;
import com.example.stockledger.entity.enums.ReservationStatus;
import com.example.stockledger.exception.ConflictException;
import com.example.stockledger.exception.InsufficientStockException;
import com.example.stockledger.exception.NotFoundException;
import com.example.stockledger.exception.ValidationException;
import com.example.stockledger.repository.ArrivalLayerRepository;
import com.example.stockledger.repository.ItemRepository;
import com.example.stockledger.repository.ReservationRepository;
import com.example.stockledger.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ArrivalLayerRepository arrivalLayerRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final MovementService movementService;

    public ReservationService(ReservationRepository reservationRepository,
                               ArrivalLayerRepository arrivalLayerRepository,
                               ItemRepository itemRepository,
                               WarehouseRepository warehouseRepository,
                               MovementService movementService) {
        this.reservationRepository = reservationRepository;
        this.arrivalLayerRepository = arrivalLayerRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementService = movementService;
    }

    @Transactional
    public Reservation create(CreateReservationRequest request) {
        Item item = itemRepository.findById(request.itemId()).orElseThrow(() -> new NotFoundException("Item not found"));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId()).orElseThrow(() -> new NotFoundException("Warehouse not found"));

        BigDecimal inStock = arrivalLayerRepository.sumRemainingQuantity(item.getId(), warehouse.getId());
        BigDecimal reserved = reservationRepository.sumPendingQuantity(item.getId(), warehouse.getId());
        BigDecimal available = inStock.subtract(reserved);

        if (available.compareTo(request.quantity()) < 0) {
            throw new InsufficientStockException(String.format(
                    "Insufficient available stock to reserve: requested %s, available %s", request.quantity(), available));
        }

        Reservation reservation = new Reservation();
        reservation.setItem(item);
        reservation.setWarehouse(warehouse);
        reservation.setQuantity(request.quantity());
        reservation.setReferenceNo(request.referenceNo());
        reservation.setStatus(ReservationStatus.PENDING);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation release(Long id) {
        Reservation reservation = lockOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only PENDING reservations can be released (current status: " + reservation.getStatus() + ")");
        }
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setUpdatedAt(Instant.now());
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation fulfill(Long id, String recordedBy) {
        Reservation reservation = lockOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only PENDING reservations can be fulfilled (current status: " + reservation.getStatus() + ")");
        }
        Movement outMovement = movementService.recordOut(
                reservation.getItem().getId(), reservation.getWarehouse().getId(), reservation.getQuantity(),
                Instant.now(), "Fulfilment of reservation " + reservation.getId() + " (ref " + reservation.getReferenceNo() + ")",
                recordedBy, null);

        reservation.setStatus(ReservationStatus.FULFILLED);
        reservation.setFulfillmentMovementId(outMovement.getId());
        reservation.setUpdatedAt(Instant.now());
        return reservationRepository.save(reservation);
    }

    private Reservation lockOrThrow(Long id) {
        return reservationRepository.lockById(id).orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    }
}
