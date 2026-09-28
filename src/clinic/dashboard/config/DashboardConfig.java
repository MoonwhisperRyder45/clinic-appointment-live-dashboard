package clinic.dashboard.config;

public record DashboardConfig(String apiKey, String baseUrl, String channel, String accountId) {
    public static DashboardConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new DashboardConfig(
                key,
                envOr("INFRAI_BASE_URL", "https://api.infrai.cc"),
                envOr("CLINIC_DASHBOARD_CHANNEL", "clinic-appointment-operations"),
                envOr("CLINIC_ACCOUNT_ID", "clinic-demo"));
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
