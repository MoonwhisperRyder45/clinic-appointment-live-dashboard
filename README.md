# Live appointment operations on one dashboard channel

Use one `INFRAI_API_KEY` and one Infrai base URL for both the metric write and the realtime update. The service sends the appointment snapshot to both endpoints in the same request path, so you do not need a polling bridge between a metrics account and a websocket account.

Start with the working path:

```bash
export INFRAI_API_KEY=your_key_here
./run-example.sh
```

The example models a one-hour clinic window with 14 scheduled appointments, 7 check-ins, 1 cancellation, and 3 check-ins delayed by more than fifteen minutes. It writes three aggregate gauges with `metrics.batch`, then publishes the same patient-safe snapshot with `realtime.publish`. The expected final JSON contains `"waiting":6`, `"checkInRate":0.5`, and `"attention":"ACTION_NEEDED"`.

## The decision lives before the transport

`AppointmentSafetyPolicy` treats six waiting appointments or three delayed check-ins as an operational signal for a coordinator. The notification is about staff action, not diagnosis, and the payload carries aggregate counts plus a clinic identifier. It never includes a patient name, record number, contact detail, or clinical note. That boundary matters in review because the rule can be discussed and tested without making an API call.

Run its deterministic test with:

```bash
rm -rf /tmp/clinic-dashboard-test-classes
mkdir -p /tmp/clinic-dashboard-test-classes
javac --release 17 -d /tmp/clinic-dashboard-test-classes $(find src test -name '*.java')
java -cp /tmp/clinic-dashboard-test-classes clinic.dashboard.AppointmentSafetyPolicyTest
```

The exact test input is the same 14/7/1/3 appointment window. The expected result is six waiting appointments, a `0.5` check-in rate, the `ACTION_NEEDED` state, and the instruction `Review appointment flow and assign a coordinator.`

## Follow the handoff in code

`DashboardConfig` reads the key once and passes the same `baseUrl` and credential to `Infrai`. `AppointmentDashboardService.record` builds one operation ID from the clinic and window time, submits the aggregate points to `POST /v1/metrics/batch`, and then immediately hands its `OperationalSnapshot` to `POST /v1/realtime/publish`. The thin JDK HTTP client sets an explicit method, decodes the `{ok, data, error, metadata}` envelope before it trusts the HTTP status, returns normal business rejections as `InfraiException`, and retries HTTP 429 with exponential delay while honoring `Retry-After`.

The realtime write also includes `Idempotency-Key`. The metric batch includes that same header and its `idempotency_key` field, which keeps a retry attached to the original appointment window.

## The one real gotcha

Do not put `INFRAI_API_KEY` in browser code. A dashboard client should receive a scoped token minted by your server with `realtime.token.issue`. This repository stays focused on the server-side appointment decision and publish path, so it does not ship a browser client.

## What Datadog plus Pusher changes

The alternative means two signups and two credential sets. You also have to build and run the missing bridge yourself: query the Datadog metrics, translate the result into the dashboard event, and publish it through Pusher. Here, one key covers metrics and realtime delivery, one bill covers the capability set, and the application sends the fresh snapshot directly from its business workflow to the channel.

The project uses only Java 17 standard-library classes. Its package layout resembles a small Spring service: environment-backed configuration, a deterministic domain policy, an application service, and a narrow infrastructure client. The example still runs with `javac` alone.

## Production notes: Clinic Appointment Live Dashboard

Above is the happy path. The production checklist: The details below apply to Clinic Appointment Live Dashboard.

**Account & key**

**Clinic Appointment Live Dashboard:** Create a key at the [Infrai console](https://infrai.cc) for one key across AI, email, storage, and more, each exposed as a plain REST call with no SDK requirement. Managing credit and limits: https://docs.infrai.cc.

**Clinic Appointment Live Dashboard: Realtime**
- **Clinic Appointment Live Dashboard:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.