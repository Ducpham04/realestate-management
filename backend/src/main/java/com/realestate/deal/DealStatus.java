package com.realestate.deal;

import java.util.EnumSet;
import java.util.Set;

public enum DealStatus {
    NEGOTIATING,
    DEPOSITED,
    CLOSED,
    CANCELLED;

    public static final Set<DealStatus> ACTIVE = EnumSet.of(NEGOTIATING, DEPOSITED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public boolean canTransitionTo(DealStatus target) {
        return switch (this) {
            case NEGOTIATING -> target == DEPOSITED || target == CLOSED || target == CANCELLED;
            case DEPOSITED -> target == CLOSED || target == CANCELLED;
            case CLOSED, CANCELLED -> false;
        };
    }
}
