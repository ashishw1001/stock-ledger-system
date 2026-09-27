package com.example.stockledger.entity;

import com.example.stockledger.entity.enums.MovementKind;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movements")
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovementKind kind;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "snapshot_item_name", nullable = false)
    private String snapshotItemName;

    @Column(name = "snapshot_item_unit", nullable = false, length = 20)
    private String snapshotItemUnit;

    @Column(length = 500)
    private String reason;

    @Column(name = "recorded_by", length = 128)
    private String recordedBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "cancelled_by_movement_id")
    private Long cancelledByMovementId;

    @Column(name = "cancels_movement_id")
    private Long cancelsMovementId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public MovementKind getKind() { return kind; }
    public void setKind(MovementKind kind) { this.kind = kind; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getSnapshotItemName() { return snapshotItemName; }
    public void setSnapshotItemName(String snapshotItemName) { this.snapshotItemName = snapshotItemName; }
    public String getSnapshotItemUnit() { return snapshotItemUnit; }
    public void setSnapshotItemUnit(String snapshotItemUnit) { this.snapshotItemUnit = snapshotItemUnit; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getCancelledByMovementId() { return cancelledByMovementId; }
    public Long getCancelsMovementId() { return cancelsMovementId; }
    public void setCancelsMovementId(Long cancelsMovementId) { this.cancelsMovementId = cancelsMovementId; }
}
