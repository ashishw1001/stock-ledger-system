package com.example.stockledger.controller;

import com.example.stockledger.dto.request.CreateReservationRequest;
import com.example.stockledger.dto.response.ReservationResponse;
import com.example.stockledger.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservationService.create(request)));
    }

    @PostMapping("/{id}/release")
    public ReservationResponse release(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.release(id));
    }

    @PostMapping("/{id}/fulfill")
    public ReservationResponse fulfill(@PathVariable Long id, @RequestParam(required = false) String recordedBy) {
        return ReservationResponse.from(reservationService.fulfill(id, recordedBy));
    }
}
