#!/bin/sh
set -eu
OUT="${TMPDIR:-/tmp}/clinic-dashboard-classes"
mkdir -p "$OUT"
javac --release 17 -d "$OUT" $(find src -name '*.java')
java -cp "$OUT" clinic.dashboard.AppointmentDashboardExample
