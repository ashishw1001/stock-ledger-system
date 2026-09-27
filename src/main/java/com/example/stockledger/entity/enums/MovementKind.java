package com.example.stockledger.entity.enums;

public enum MovementKind {
    IN(true),
    OUT(false),
    TRANSFER_IN(true),
    TRANSFER_OUT(false),
    COUNT_ADJUSTMENT(true),
    CANCEL_IN(false),
    CANCEL_OUT(true),
    CANCEL_TRANSFER_IN(false),
    CANCEL_TRANSFER_OUT(true);

    private final boolean increasesStock;

    MovementKind(boolean increasesStock) {
        this.increasesStock = increasesStock;
    }

    public boolean increasesStock() {
        return increasesStock;
    }

    public boolean isCancellation() {
        return this == CANCEL_IN || this == CANCEL_OUT || this == CANCEL_TRANSFER_IN || this == CANCEL_TRANSFER_OUT;
    }
}
