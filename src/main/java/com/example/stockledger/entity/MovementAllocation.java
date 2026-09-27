package com.example.stockledger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movement_allocations")
public class MovementAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_movement_id", nullable = false)
    private Movement outMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_movement_id", nullable = false)
    private Movement inMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_layer_id", nullable = false)
    private ArrivalLayer arrivalLayer;

    @Column(name = "quantity_allocated", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityAllocated;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Movement getOutMovement() { return outMovement; }
    public void setOutMovement(Movement outMovement) { this.outMovement = outMovement; }
    public Movement getInMovement() { return inMovement; }
    public void setInMovement(Movement inMovement) { this.inMovement = inMovement; }
    public ArrivalLayer getArrivalLayer() { return arrivalLayer; }
    public void setArrivalLayer(ArrivalLayer arrivalLayer) { this.arrivalLayer = arrivalLayer; }
    public BigDecimal getQuantityAllocated() { return quantityAllocated; }
    public void setQuantityAllocated(BigDecimal quantityAllocated) { this.quantityAllocated = quantityAllocated; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
