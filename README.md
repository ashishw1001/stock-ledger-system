# Stock Ledger

A Java 21 and Spring Boot REST API for warehouse stock. PostgreSQL stores the ledger and FIFO cost layers. The API records arrivals, issues, transfers, reservations, stock counts, and documents.

See [DESIGN.md](DESIGN.md) for the design in plain language. See [API_TEST_DATA.txt](API_TEST_DATA.txt) for a longer copy-and-paste request walkthrough.

## Requirements

- JDK 21
- Maven 3.9 or newer
- PostgreSQL 16 or compatible

## Start from a clean machine

Install JDK 21, Maven, and PostgreSQL 16 using their standard installers. Start the PostgreSQL service. Open `psql` as a PostgreSQL administrator and create separate application and test databases:

```sql
CREATE ROLE stock_ledger LOGIN PASSWORD 'stock_ledger';
CREATE DATABASE stock_ledger OWNER stock_ledger;
CREATE DATABASE stock_ledger_test OWNER stock_ledger;
```

Apply the schema to the application database:

```text
psql -h localhost -p 5432 -U stock_ledger -d stock_ledger -f src/main/resources/db/schema.sql
```

Apply the same schema to the separate test database. The test suite truncates
tables there before each test, so this must not be your application database:

```text
psql -h localhost -p 5432 -U stock_ledger -d stock_ledger_test -f src/main/resources/db/schema.sql
```

The schema is SQL for PostgreSQL. The application does not create or migrate tables automatically. Hibernate checks that the tables exist when the application starts.

Build and start the API from the project directory:

```text
mvn clean package
java -jar target/stock-ledger-1.0.0.jar
```

The API listens on `http://localhost:8080`. The default database connection is in `src/main/resources/application.yml`. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to use different connection details. Set `SERVER_PORT` to change the HTTP port.

## Add sample data

Run this once against an empty database:

```text
curl.exe -X POST http://localhost:8080/api/v1/seed
```

It creates two items (`WIDGET-A`, `STEEL-COIL`), two warehouses (`MAIN`, `NORTH`), and arrival stock. Each call adds new arrival movements, so do not repeat it unless you want more stock. Find generated IDs with `GET /api/v1/items` and `GET /api/v1/warehouses`.

## Endpoints

All routes start with `http://localhost:8080/api/v1`. For JSON requests, send `Content-Type: application/json`. IDs below are examples; use IDs returned by your API.

| Method and path | Example request |
|---|---|
| `POST /seed` | No body. Creates sample data. |
| `POST /items` | `{"code":"BOLT-1","name":"Bolt","unit":"PIECES"}` |
| `GET /items` | No body. |
| `PATCH /items/1/disable` | No body. |
| `POST /warehouses` | `{"code":"NORTH-2","name":"North Depot"}` |
| `GET /warehouses` | No body. |
| `PATCH /warehouses/1/disable` | No body. |
| `POST /movements/in` | `{"itemId":1,"warehouseId":1,"quantity":25,"unitCost":11.50,"reason":"Purchase","recordedBy":"sam"}` |
| `POST /movements/out` | `{"itemId":1,"warehouseId":1,"quantity":5,"reason":"Sale","recordedBy":"sam"}` |
| `POST /movements/transfer` | `{"itemId":1,"sourceWarehouseId":1,"destinationWarehouseId":2,"quantity":3,"reason":"Move stock","recordedBy":"sam"}` |
| `POST /movements/1/cancel` | `{"reason":"Entered in error","recordedBy":"sam"}` |
| `GET /movements?itemId=1&page=0&size=20` | No body. |
| `GET /movements/1/cost` | No body. Use an OUT movement ID. |
| `GET /stock/current?itemId=1&warehouseId=1` | No body. |
| `GET /stock/historical?itemId=1&warehouseId=1&at=2026-09-28T10:00:00Z` | No body. |
| `POST /documents` | `{"documentType":"DISPATCH_NOTE","referenceNo":"D-1","recordedBy":"sam","lines":[{"itemId":1,"warehouseId":1,"kind":"OUT","quantity":2,"reason":"Dispatch"}]}` |
| `POST /stock-counts/start` | `{"itemId":1,"warehouseId":1}` |
| `GET /stock-counts/1` | No body. Use the count ID. |
| `POST /stock-counts/1/complete` | `{"countedQuantity":95,"recordedBy":"sam"}` |
| `POST /reservations` | `{"itemId":1,"warehouseId":1,"quantity":5,"referenceNo":"ORDER-1"}` |
| `POST /reservations/1/release` | No body. Use a pending reservation ID. |
| `POST /reservations/1/fulfill?recordedBy=sam` | No body. Use a pending reservation ID instead of releasing it. |

For valid JSON examples and a suggested test order, open `API_TEST_DATA.txt`. A reservation can be released or fulfilled, but not both. Disable operations and cancellation are one-time state changes.

## Real PostgreSQL tests

Tests use a real PostgreSQL database. They do not use H2 or another in-memory database. Create `stock_ledger_test` and apply the schema as described above.

The tests connect to `stock_ledger_test` by default. Configure `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` to change the test connection. Run the full suite, including simultaneous issue and stock-count requests, with:

```text
mvn test
```

**Warning:** tests clear tables in the configured test database before each test.

The integration tests cover historical quantity and value, FIFO issue cost, issue cancellation and restored layers, rollback of a document that fails on line three, two issues racing for the last unit, and two requests racing to open the same count.


