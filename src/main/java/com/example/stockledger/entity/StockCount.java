package com.example.stockledger.entity;

import com.example.stockledger.entity.enums.StockCountStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_counts")
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCountStatus status = StockCountStatus.OPEN;

    @Column(name = "system_quantity_at_open", nullable = false, precision = 15, scale = 4)
    private BigDecimal systemQuantityAtOpen;

    @Column(name = "counted_quantity", precision = 15, scale = 4)
    private BigDecimal countedQuantity;

    @Column(name = "adjustment_movement_id")
    private Long adjustmentMovementId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public StockCountStatus getStatus() { return status; }
    public void setStatus(StockCountStatus status) { this.status = status; }
    public BigDecimal getSystemQuantityAtOpen() { return systemQuantityAtOpen; }
    public void setSystemQuantityAtOpen(BigDecimal systemQuantityAtOpen) { this.systemQuantityAtOpen = systemQuantityAtOpen; }
    public BigDecimal getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(BigDecimal countedQuantity) { this.countedQuantity = countedQuantity; }
    public Long getAdjustmentMovementId() { return adjustmentMovementId; }
    public void setAdjustmentMovementId(Long adjustmentMovementId) { this.adjustmentMovementId = adjustmentMovementId; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
