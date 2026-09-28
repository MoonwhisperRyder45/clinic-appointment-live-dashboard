package clinic.dashboard.domain;

import java.time.Instant;

public record AppointmentWindow(
        String clinicId,
        Instant windowStart,
        int scheduled,
        int checkedIn,
        int cancelled,
        int delayedOverFifteenMinutes) {
    public AppointmentWindow {
        if (clinicId == null || clinicId.isBlank()) throw new IllegalArgumentException("clinicId is required");
        if (windowStart == null) throw new IllegalArgumentException("windowStart is required");
        if (scheduled < 0 || checkedIn < 0 || cancelled < 0 || delayedOverFifteenMinutes < 0) {
            throw new IllegalArgumentException("appointment counts cannot be negative");
        }
        if (checkedIn + cancelled > scheduled || delayedOverFifteenMinutes > checkedIn) {
            throw new IllegalArgumentException("appointment counts are inconsistent");
        }
    }
}
