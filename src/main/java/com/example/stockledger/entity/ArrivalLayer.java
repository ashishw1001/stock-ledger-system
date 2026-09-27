package com.example.stockledger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "arrival_layers")
public class ArrivalLayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movement_id", nullable = false, unique = true)
    private Movement movement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "initial_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal initialQuantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal remainingQuantity;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Movement getMovement() { return movement; }
    public void setMovement(Movement movement) { this.movement = movement; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public BigDecimal getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(BigDecimal initialQuantity) { this.initialQuantity = initialQuantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
