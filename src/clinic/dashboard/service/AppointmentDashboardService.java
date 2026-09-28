package clinic.dashboard.service;

import clinic.dashboard.domain.AppointmentSafetyPolicy;
import clinic.dashboard.domain.AppointmentWindow;
import clinic.dashboard.domain.OperationalSnapshot;
import clinic.dashboard.infrai.Infrai;

import java.util.List;
import java.util.Map;

public final class AppointmentDashboardService {
    private final AppointmentSafetyPolicy policy;
    private final Infrai infrai;

    public AppointmentDashboardService(AppointmentSafetyPolicy policy, Infrai infrai) {
        this.policy = policy;
        this.infrai = infrai;
    }

    public OperationalSnapshot record(AppointmentWindow window) {
        OperationalSnapshot snapshot = policy.evaluate(window);
        String operationId = window.clinicId() + ":" + window.windowStart();
        Map<String, Object> tags = Map.of("clinic_id", window.clinicId());
        List<Map<String, Object>> points = List.of(
                point("appointments.waiting", snapshot.waiting(), tags),
                point("appointments.delayed", snapshot.delayed(), tags),
                point("appointments.check_in_rate", snapshot.checkInRate(), tags));

        // The one key records the numbers and immediately carries the same snapshot to the live channel.
        infrai.metricsBatch(points, operationId + ":metrics");
        infrai.realtimePublish("appointment.operations.updated", snapshot.asEventData(), operationId + ":realtime");
        return snapshot;
    }

    private Map<String, Object> point(String name, Number value, Map<String, Object> tags) {
        return Map.of("name", name, "value", value, "type", "gauge", "tags", tags);
    }
}
