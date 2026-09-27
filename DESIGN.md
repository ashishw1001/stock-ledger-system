# Stock Ledger Design

## Tables and calculations

`items` and `warehouses` hold the names, codes, units, and active status used by the API. `documents` holds a header for a group of related movements.

`movements` is the event history. It stores arrivals, issues, transfers, stock-count adjustments, and cancellation events. The database trigger rejects updates and deletes to this ledger. A transfer has an outgoing movement at its source and one or more incoming movements at its destination.

`arrival_layers` tracks the quantity and cost of each batch that added stock. Its remaining quantity is updated as stock is issued or restored. `movement_allocations` records which batches an issue used and their costs. These tables let the system answer current-stock and cost questions without replaying every movement.

`reservations` stores pending holds and their status. `stock_counts` stores the opening system quantity and the physical count. Stock, value, and whether a movement was cancelled are calculated from these records rather than written back to old movement rows.

## Past stock and value

For a requested date, the historical query considers arrivals recorded by that date, then subtracts allocations from issues that had happened by that date and had not yet been cancelled. The remaining quantity in each batch is multiplied by that batch's unit cost. The results are summed for the requested item and warehouse.

This answers “what does the ledger now say stock was on that date?” A backdated arrival can change a later historical answer. The system does not change the FIFO allocations of issues that were already recorded.

## The 150-unit FIFO example

Assume one item and warehouse receive 100 units at 10.00, then 100 at 12.00. An issue of 150 takes all 100 units from the first batch and 50 from the second. The issue cost is 1,600.00. There are three movement rows (two IN, one OUT), two arrival-layer rows, and two allocation rows. The layers show 0 and 50 units remaining, so current stock is 50 units worth 600.00.

## Cancelling the issue

The service restores 100 units to the first layer and 50 to the second, using the original allocation rows. It adds a `CANCEL_OUT` movement that points to the issue. It does not change or delete the issue or its allocations. Stock returns to 200 units worth 2,200.00, at the same batch costs. A database unique index allows only one cancellation per movement.

## Concurrent requests

Before allocating an issue, the service selects available arrival layers in FIFO order using PostgreSQL `SELECT ... FOR UPDATE`. A second request for the same last unit waits for the first transaction. After the first commits, the second sees the updated quantity and fails for insufficient stock. This protects the same item and warehouse while allowing unrelated stock to proceed. The cost is that requests for a busy item and warehouse queue behind its row locks.

The database also has a partial unique index allowing only one open count per item and warehouse. Competing count requests cannot both insert an open count.

## A document that fails on line three

The document and all its lines run inside one database transaction. If line three fails, PostgreSQL rolls back the document header and the work from lines one and two, including movements, allocations, and layer changes. No partial document remains.

## Backdated arrivals and scale

FIFO uses arrival time and then row ID to order batches available when an issue is recorded. A late-entered, backdated arrival affects future issues, but does not rewrite old issue costs. This preserves the recorded history, though the historical picture can change when late information is added.

At ten million movements, historical reports and indexes need measurement and likely tuning. Partitioning old ledger data, adding monthly stock snapshots, and moving reporting to a separate read model are possible next steps. Hot items can also queue on FIFO row locks.

## Before production

This is a working sample, not a finished warehouse product. It has no authentication, authorization, request idempotency, closed accounting periods, currency handling, or production monitoring. Reservations are not enforced against direct issue requests. Transfer cancellation is performed one leg at a time.

For production, I would run the packaged application behind a load balancer and connect it to a private, backed-up PostgreSQL service. Credentials would come from a secret store, and logs, database health, and failed requests would be monitored. Before deployment I would add identity and permissions, idempotency keys, period controls, atomic transfer reversal, stronger reservation checks, a tested backup restore, and load testing against expected warehouse traffic. Schema changes would be reviewed and applied as controlled SQL during deployment.
