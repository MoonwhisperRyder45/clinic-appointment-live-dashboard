package clinic.dashboard;

import clinic.dashboard.config.DashboardConfig;
import clinic.dashboard.domain.AppointmentSafetyPolicy;
import clinic.dashboard.domain.AppointmentWindow;
import clinic.dashboard.domain.OperationalSnapshot;
import clinic.dashboard.infrai.Infrai;
import clinic.dashboard.infrai.Json;
import clinic.dashboard.service.AppointmentDashboardService;

import java.time.Instant;

public final class AppointmentDashboardExample {
    public static void main(String[] args) {
        DashboardConfig config = DashboardConfig.fromEnvironment();
        AppointmentDashboardService service = new AppointmentDashboardService(
                new AppointmentSafetyPolicy(), new Infrai(config));
        AppointmentWindow input = new AppointmentWindow(
                "clinic-learning-01", Instant.parse("2026-09-27T09:00:00Z"), 14, 7, 1, 3);
        OperationalSnapshot result = service.record(input);
        System.out.println(Json.encode(result.asEventData()));
    }
}
