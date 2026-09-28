package clinic.dashboard.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record OperationalSnapshot(
        String clinicId,
        Instant windowStart,
        int scheduled,
        int checkedIn,
        int waiting,
        int cancelled,
        int delayed,
        double checkInRate,
        String attention,
        String notification) {
    public Map<String, Object> asEventData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clinicId", clinicId);
        data.put("windowStart", windowStart.toString());
        data.put("scheduled", scheduled);
        data.put("checkedIn", checkedIn);
        data.put("waiting", waiting);
        data.put("cancelled", cancelled);
        data.put("delayed", delayed);
        data.put("checkInRate", checkInRate);
        data.put("attention", attention);
        data.put("notification", notification);
        return data;
    }
}
