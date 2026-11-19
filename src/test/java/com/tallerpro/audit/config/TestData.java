package com.tallerpro.audit.config;

import com.tallerpro.audit.dto.AuditEventMessage;
import java.time.Instant;

final class TestData {
    static AuditEventMessage dto() {
        return new AuditEventMessage("ev-1", "ORDER_STATUS_CHANGED", Instant.parse("2026-11-18T10:00:00Z"),
                null, null, "ORD-1", "T-1", null, "u-1", "Ana", null, null);
    }
}
