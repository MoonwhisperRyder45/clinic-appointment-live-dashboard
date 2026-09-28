package clinic.dashboard;

import clinic.dashboard.domain.AppointmentSafetyPolicy;
import clinic.dashboard.domain.AppointmentWindow;
import clinic.dashboard.domain.OperationalSnapshot;

import java.time.Instant;

public final class AppointmentSafetyPolicyTest {
    public static void main(String[] args) {
        AppointmentWindow input = new AppointmentWindow(
                "teaching-clinic-a", Instant.parse("2026-09-27T09:00:00Z"), 14, 7, 1, 3);
        OperationalSnapshot result = new AppointmentSafetyPolicy().evaluate(input);

        equal(6, result.waiting(), "waiting");
        equal(0.5, result.checkInRate(), "checkInRate");
        equal("ACTION_NEEDED", result.attention(), "attention");
        equal("Review appointment flow and assign a coordinator.", result.notification(), "notification");
        System.out.println("AppointmentSafetyPolicyTest passed");
    }

    private static void equal(Object expected, Object actual, String field) {
        if (!expected.equals(actual)) {
            throw new AssertionError(field + " expected=" + expected + " actual=" + actual);
        }
    }
}
