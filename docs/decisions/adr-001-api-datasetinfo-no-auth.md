# Security Decision: /api/datasetinfo Authentication

**Story:** US-SEC-008
**Date:** 2026-03-03
**Decision:** Risk-Accept (see rationale below)

## Endpoint

`GET /api/datasetinfo?id={datasetId}`

Returns: dataset title, last update timestamp, variable list, status.

## Risk Assessment

| Aspect | Assessment |
|--------|-----------|
| Data sensitivity | Low — metadata only, no actual data values |
| Information disclosed | Dataset IDs, variable names, last-update time |
| Attack surface | Enumerable dataset IDs (already visible in WMS GetCapabilities) |
| CVSS estimate | 3.1 (Low) — AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N |

## Decision: Risk-Accept

The `/api/datasetinfo` endpoint is intentionally **public** because:

1. **WMS semantics require public metadata** — WMS 1.1/1.3 GetCapabilities already exposes all dataset IDs and variable names without authentication. Protecting `/api/datasetinfo` while leaving `GetCapabilities` open provides no real security benefit.

2. **Godiva3 dependency** — The Godiva3 web client calls this endpoint for display purposes (status, variable list). Requiring auth would break the unauthenticated Godiva3 UI.

3. **No sensitive data** — The response contains only: `id`, `title`, `lastUpdate`, `status`, `variables[id, title]`. No file paths, credentials, or user data.

## Mitigations Applied

- Null-guard for unknown `datasetId` → 404 (not 500), prevents enumeration side-channel (US-SEC-006 ✅)
- Response Content-Type: `application/json;charset=UTF-8` (US-SEC-006 ✅)
- No stack traces in error responses (US-SEC-005 ✅)

## Condition to Revisit

This decision should be revisited if:
- Multi-tenancy is introduced (datasets belonging to specific users/orgs)
- Variable names or dataset titles are considered proprietary
- A GDPR assessment requires access control on metadata

## Tracked By

- US-SEC-008 in `.claude/user-stories.md`
- CISO finding: CISO-009 (Low severity)
