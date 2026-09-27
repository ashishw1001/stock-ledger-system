package com.example.stockledger.integration;

import com.example.stockledger.dto.request.CreateDocumentRequest;
import com.example.stockledger.dto.request.DocumentLineRequest;
import com.example.stockledger.dto.response.CurrentStockResponse;
import com.example.stockledger.dto.response.HistoricalStockResponse;
import com.example.stockledger.entity.Item;
import com.example.stockledger.entity.Movement;
import com.example.stockledger.entity.Warehouse;
import com.example.stockledger.entity.enums.DocumentType;
import com.example.stockledger.entity.enums.ItemUnit;
import com.example.stockledger.entity.enums.MovementKind;
import com.example.stockledger.exception.ConflictException;
import com.example.stockledger.exception.InsufficientStockException;
import com.example.stockledger.service.DocumentService;
import com.example.stockledger.service.ItemService;
import com.example.stockledger.service.MovementService;
import com.example.stockledger.service.StockCountService;
import com.example.stockledger.service.StockService;
import com.example.stockledger.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StockLedgerIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ItemService itemService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private MovementService movementService;
    @Autowired private StockService stockService;
    @Autowired private DocumentService documentService;
    @Autowired private StockCountService stockCountService;

    private Item item;
    private Warehouse warehouse;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE TABLE stock_counts, reservations, movement_allocations, arrival_layers, " +
                "movements, documents, warehouses, items RESTART IDENTITY CASCADE");
        item = itemService.create(new com.example.stockledger.dto.request.CreateItemRequest(
                "TEST-ITEM", "Test Item", ItemUnit.PIECES));
        warehouse = warehouseService.create(new com.example.stockledger.dto.request.CreateWarehouseRequest(
                "TEST-WH", "Test Warehouse"));
    }

    @Test
    void historicalQuantityAndValueAndFifoIssueCancellationAreCorrect() {
        Instant firstArrivalAt = Instant.parse("2024-03-01T00:00:00Z");
        Instant secondArrivalAt = Instant.parse("2024-03-05T00:00:00Z");
        Instant issueTime = Instant.parse("2024-03-08T00:00:00Z");
        Movement firstArrival = movementService.recordIn(item.getId(), warehouse.getId(), bd("100"), bd("10"),
                firstArrivalAt, "first batch", "test", null);
        Movement secondArrival = movementService.recordIn(item.getId(), warehouse.getId(), bd("100"), bd("12"),
                secondArrivalAt, "second batch", "test", null);

        Movement issue = movementService.recordOut(item.getId(), warehouse.getId(), bd("150"),
                issueTime, "FIFO issue", "test", null);

        List<com.example.stockledger.entity.MovementAllocation> allocations =
                movementService.getAllocations(issue.getId());
        assertEquals(2, allocations.size());
        allocations = allocations.stream()
                .sorted(Comparator.comparing(a -> a.getUnitCost()))
                .toList();
        assertEquals(0, bd("100").compareTo(allocations.get(0).getQuantityAllocated()));
        assertEquals(0, bd("10").compareTo(allocations.get(0).getUnitCost()));
        assertEquals(0, bd("50").compareTo(allocations.get(1).getQuantityAllocated()));
        assertEquals(0, bd("12").compareTo(allocations.get(1).getUnitCost()));
        BigDecimal issueCost = allocations.stream()
                .map(a -> a.getQuantityAllocated().multiply(a.getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, bd("1600").compareTo(issueCost));

        HistoricalStockResponse beforeIssue = stockService.getHistoricalStock(
                item.getId(), warehouse.getId(), Instant.parse("2024-03-06T00:00:00Z"));
        assertAmount("200", beforeIssue.quantity());
        assertAmount("2200", beforeIssue.valuation());

        HistoricalStockResponse afterIssue = stockService.getHistoricalStock(
                item.getId(), warehouse.getId(), issueTime.plusSeconds(1));
        assertAmount("50", afterIssue.quantity());
        assertAmount("600", afterIssue.valuation());
        assertAmount("0", layerRemaining(firstArrival.getId()));
        assertAmount("50", layerRemaining(secondArrival.getId()));

        Movement cancellation = movementService.cancelMovement(issue.getId(), "test reversal", "test");
        assertEquals("CANCEL_OUT", cancellation.getKind().name());
        assertEquals(issue.getId(), cancellation.getCancelsMovementId());
        assertAmount("100", layerRemaining(firstArrival.getId()));
        assertAmount("100", layerRemaining(secondArrival.getId()));
        assertEquals("OUT", jdbc.queryForObject(
                "SELECT kind FROM movements WHERE id = ?", String.class, issue.getId()));
        assertAmount("150", jdbc.queryForObject(
                "SELECT quantity FROM movements WHERE id = ?", BigDecimal.class, issue.getId()));
        CurrentStockResponse current = stockService.getCurrentStock(item.getId(), warehouse.getId());
        assertAmount("200", current.inStock());
        assertAmount("2200", current.valuation());

        HistoricalStockResponse afterCancellation = stockService.getHistoricalStock(
                item.getId(), warehouse.getId(), cancellation.getOccurredAt().plusSeconds(1));
        assertAmount("200", afterCancellation.quantity());
        assertAmount("2200", afterCancellation.valuation());
        assertEquals(4, countRows("movements"));
        assertEquals(2, countRows("arrival_layers"));
        assertEquals(2, countRows("movement_allocations"));
    }

    @Test
    void failedThirdDocumentLineLeavesNoRowsOrStockChanges() {
        movementService.recordIn(item.getId(), warehouse.getId(), bd("10"), bd("4"),
                Instant.parse("2024-01-01T00:00:00Z"), "opening", "test", null);
        long movementsBefore = countRows("movements");
        long documentsBefore = countRows("documents");

        CreateDocumentRequest request = new CreateDocumentRequest(
                DocumentType.DISPATCH_NOTE, "DOC-FAIL", Instant.now(), "test", List.of(
                new DocumentLineRequest(item.getId(), warehouse.getId(), MovementKind.OUT,
                        bd("1"), null, "line one"),
                new DocumentLineRequest(item.getId(), warehouse.getId(), MovementKind.OUT,
                        bd("1"), null, "line two"),
                new DocumentLineRequest(item.getId(), warehouse.getId(), MovementKind.OUT,
                        bd("100"), null, "line three fails")));

        assertThrows(InsufficientStockException.class, () -> documentService.execute(request));
        assertEquals(movementsBefore, countRows("movements"));
        assertEquals(documentsBefore, countRows("documents"));
        assertAmount("10", stockService.getCurrentStock(item.getId(), warehouse.getId()).inStock());
        assertEquals(0, countRows("movement_allocations"));
    }

    @Test
    void concurrentIssuesCannotTakeTheSameLastUnit() throws Exception {
        movementService.recordIn(item.getId(), warehouse.getId(), bd("1"), bd("5"),
                Instant.now().minusSeconds(10), "one last unit", "test", null);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = pool.submit(() -> attemptIssue(ready, start));
            Future<Boolean> second = pool.submit(() -> attemptIssue(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
        }
        assertAmount("0", stockService.getCurrentStock(item.getId(), warehouse.getId()).inStock());
        assertEquals(1, countRows("movements", "kind = 'OUT'"));
    }

    @Test
    void concurrentStockCountRequestsCreateOnlyOneOpenCount() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = pool.submit(() -> attemptCountStart(ready, start));
            Future<Boolean> second = pool.submit(() -> attemptCountStart(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
        }
        assertEquals(1, countRows("stock_counts", "status = 'OPEN'"));
    }

    private boolean attemptIssue(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            movementService.recordOut(item.getId(), warehouse.getId(), bd("1"), Instant.now(),
                    "concurrent issue", "test", null);
            return true;
        } catch (InsufficientStockException expected) {
            return false;
        }
    }

    private boolean attemptCountStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            stockCountService.start(item.getId(), warehouse.getId());
            return true;
        } catch (ConflictException expected) {
            return false;
        }
    }

    private long countRows(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private long countRows(String table, String condition) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + condition, Long.class);
    }

    private BigDecimal layerRemaining(Long movementId) {
        return jdbc.queryForObject(
                "SELECT remaining_quantity FROM arrival_layers WHERE movement_id = ?",
                BigDecimal.class, movementId);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual), "Expected " + expected + " but was " + actual);
    }
}
