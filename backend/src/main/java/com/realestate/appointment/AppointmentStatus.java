package com.realestate.appointment;

public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    public boolean isFinal() {
        return this != SCHEDULED;
    }
}
