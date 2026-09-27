
CREATE TABLE items (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    unit            VARCHAR(20) NOT NULL CHECK (unit IN ('PIECES', 'KG', 'LITRES')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE warehouses (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE documents (
    id              BIGSERIAL PRIMARY KEY,
    document_type   VARCHAR(30) NOT NULL CHECK (document_type IN ('DELIVERY_NOTE', 'DISPATCH_NOTE', 'TRANSFER_NOTE')),
    reference_no    VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE movements (
    id                      BIGSERIAL PRIMARY KEY,
    document_id             BIGINT REFERENCES documents(id),
    item_id                 BIGINT NOT NULL REFERENCES items(id),
    warehouse_id            BIGINT NOT NULL REFERENCES warehouses(id),
    kind                    VARCHAR(30) NOT NULL CHECK (kind IN (
                                'IN', 'OUT', 'TRANSFER_IN', 'TRANSFER_OUT',
                                'COUNT_ADJUSTMENT',
                                'CANCEL_IN', 'CANCEL_OUT',
                                'CANCEL_TRANSFER_IN', 'CANCEL_TRANSFER_OUT'
                             )),
    quantity                NUMERIC(15,4) NOT NULL,
    unit_price              NUMERIC(15,4),
    snapshot_item_name      VARCHAR(255) NOT NULL,
    snapshot_item_unit      VARCHAR(20) NOT NULL,
    reason                  VARCHAR(500),
    recorded_by             VARCHAR(128),
    occurred_at             TIMESTAMPTZ NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_by_movement_id BIGINT,
    cancels_movement_id     BIGINT REFERENCES movements(id)
);

CREATE INDEX idx_movements_item_wh ON movements(item_id, warehouse_id);
CREATE INDEX idx_movements_occurred_at ON movements(occurred_at);
CREATE INDEX idx_movements_document ON movements(document_id);
CREATE INDEX idx_movements_cancels ON movements(cancels_movement_id);

CREATE UNIQUE INDEX unique_cancels_movement ON movements(cancels_movement_id) WHERE cancels_movement_id IS NOT NULL;

CREATE OR REPLACE FUNCTION prevent_movement_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'movements is an append-only ledger: % is not permitted (id=%)', TG_OP, OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_movement_update
    BEFORE UPDATE ON movements
    FOR EACH ROW EXECUTE FUNCTION prevent_movement_mutation();

CREATE TRIGGER trg_prevent_movement_delete
    BEFORE DELETE ON movements
    FOR EACH ROW EXECUTE FUNCTION prevent_movement_mutation();

CREATE TABLE arrival_layers (
    id                  BIGSERIAL PRIMARY KEY,
    movement_id         BIGINT NOT NULL UNIQUE REFERENCES movements(id),
    item_id             BIGINT NOT NULL REFERENCES items(id),
    warehouse_id        BIGINT NOT NULL REFERENCES warehouses(id),
    initial_quantity    NUMERIC(15,4) NOT NULL,
    remaining_quantity  NUMERIC(15,4) NOT NULL,
    unit_cost           NUMERIC(15,4) NOT NULL,
    occurred_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_remaining_bounds CHECK (remaining_quantity >= 0 AND remaining_quantity <= initial_quantity)
);

CREATE INDEX idx_arrival_layers_fifo ON arrival_layers(item_id, warehouse_id, occurred_at, id);

CREATE TABLE movement_allocations (
    id                  BIGSERIAL PRIMARY KEY,
    out_movement_id     BIGINT NOT NULL REFERENCES movements(id),
    in_movement_id      BIGINT NOT NULL REFERENCES movements(id),
    arrival_layer_id    BIGINT NOT NULL REFERENCES arrival_layers(id),
    quantity_allocated  NUMERIC(15,4) NOT NULL CHECK (quantity_allocated > 0),
    unit_cost           NUMERIC(15,4) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_allocations_out ON movement_allocations(out_movement_id);
CREATE INDEX idx_allocations_in ON movement_allocations(in_movement_id);
CREATE INDEX idx_allocations_layer ON movement_allocations(arrival_layer_id);

CREATE TABLE reservations (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL REFERENCES items(id),
    warehouse_id    BIGINT NOT NULL REFERENCES warehouses(id),
    quantity        NUMERIC(15,4) NOT NULL CHECK (quantity > 0),
    reference_no    VARCHAR(128),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'FULFILLED', 'RELEASED')),
    fulfillment_movement_id BIGINT REFERENCES movements(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservations_item_wh_status ON reservations(item_id, warehouse_id, status);

CREATE TABLE stock_counts (
    id                          BIGSERIAL PRIMARY KEY,
    item_id                     BIGINT NOT NULL REFERENCES items(id),
    warehouse_id                BIGINT NOT NULL REFERENCES warehouses(id),
    status                      VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'FINISHED')),
    system_quantity_at_open     NUMERIC(15,4) NOT NULL,
    counted_quantity            NUMERIC(15,4),
    adjustment_movement_id      BIGINT REFERENCES movements(id),
    opened_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at                   TIMESTAMPTZ
);

CREATE UNIQUE INDEX unique_open_stock_count ON stock_counts(item_id, warehouse_id) WHERE status = 'OPEN';
