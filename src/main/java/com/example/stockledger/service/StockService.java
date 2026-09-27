package com.example.stockledger.service;

import com.example.stockledger.dto.response.CurrentStockResponse;
import com.example.stockledger.dto.response.HistoricalStockResponse;
import com.example.stockledger.repository.ArrivalLayerRepository;
import com.example.stockledger.repository.ReservationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class StockService {

    private final ArrivalLayerRepository arrivalLayerRepository;
    private final ReservationRepository reservationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public StockService(ArrivalLayerRepository arrivalLayerRepository, ReservationRepository reservationRepository) {
        this.arrivalLayerRepository = arrivalLayerRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public CurrentStockResponse getCurrentStock(Long itemId, Long warehouseId) {
        BigDecimal inStock = arrivalLayerRepository.sumRemainingQuantity(itemId, warehouseId);
        BigDecimal valuation = arrivalLayerRepository.sumRemainingValue(itemId, warehouseId);
        BigDecimal reserved = reservationRepository.sumPendingQuantity(itemId, warehouseId);
        BigDecimal available = inStock.subtract(reserved);
        return new CurrentStockResponse(itemId, warehouseId, inStock, reserved, available, valuation);
    }

    @Transactional(readOnly = true)
    public HistoricalStockResponse getHistoricalStock(Long itemId, Long warehouseId, Instant at) {
        String sql = """
                SELECT
                    al.id AS layer_id,
                    al.unit_cost AS unit_cost,
                    CASE
                        WHEN in_m.occurred_at > :at THEN 0
                        WHEN cancel_in.occurred_at IS NOT NULL AND cancel_in.occurred_at <= :at THEN 0
                        ELSE al.initial_quantity - COALESCE(consumed.qty, 0)
                    END AS remaining_at
                FROM arrival_layers al
                JOIN movements in_m ON in_m.id = al.movement_id
                LEFT JOIN movements cancel_in
                       ON cancel_in.cancels_movement_id = in_m.id
                LEFT JOIN (
                    SELECT ma.in_movement_id AS in_movement_id, SUM(ma.quantity_allocated) AS qty
                    FROM movement_allocations ma
                    JOIN movements out_m ON out_m.id = ma.out_movement_id
                    WHERE out_m.occurred_at <= :at
                      AND NOT EXISTS (
                          SELECT 1 FROM movements cancel_out
                          WHERE cancel_out.cancels_movement_id = out_m.id
                            AND cancel_out.occurred_at <= :at
                      )
                    GROUP BY ma.in_movement_id
                ) consumed ON consumed.in_movement_id = in_m.id
                WHERE al.item_id = :itemId AND al.warehouse_id = :warehouseId
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("at", java.sql.Timestamp.from(at));
        query.setParameter("itemId", itemId);
        query.setParameter("warehouseId", warehouseId);

        @SuppressWarnings("unchecked")
        var rows = query.getResultList();

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            BigDecimal unitCost = (BigDecimal) row[1];
            BigDecimal remainingAt = (BigDecimal) row[2];
            if (remainingAt.signum() < 0) remainingAt = BigDecimal.ZERO;
            totalQty = totalQty.add(remainingAt);
            totalValue = totalValue.add(remainingAt.multiply(unitCost));
        }

        return new HistoricalStockResponse(itemId, warehouseId, at, totalQty, totalValue);
    }
}
