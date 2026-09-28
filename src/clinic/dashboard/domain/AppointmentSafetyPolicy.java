package clinic.dashboard.domain;

public final class AppointmentSafetyPolicy {
    public OperationalSnapshot evaluate(AppointmentWindow window) {
        int waiting = window.scheduled() - window.checkedIn() - window.cancelled();
        double checkInRate = window.scheduled() == 0
                ? 0.0
                : Math.round(window.checkedIn() * 100.0 / window.scheduled()) / 100.0;

        String attention;
        String notification;
        if (window.delayedOverFifteenMinutes() >= 3 || waiting >= 6) {
            attention = "ACTION_NEEDED";
            notification = "Review appointment flow and assign a coordinator.";
        } else if (window.delayedOverFifteenMinutes() > 0 || waiting >= 3) {
            attention = "WATCH";
            notification = "Monitor the waiting area for the next appointment update.";
        } else {
            attention = "NORMAL";
            notification = "Appointment flow is within the operating threshold.";
        }

        return new OperationalSnapshot(window.clinicId(), window.windowStart(), window.scheduled(),
                window.checkedIn(), waiting, window.cancelled(), window.delayedOverFifteenMinutes(),
                checkInRate, attention, notification);
    }
}
