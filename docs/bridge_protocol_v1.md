# CM Bridge Protocol v1

**Status:** live, in production use by CM Companion, Guest Chat, Financing, and Davčna blagajna. This document describes exactly what those callers use — nothing here is aspirational or planned.

## What this is (and isn't)

Bridge Protocol v1 is a local, lateral HTTP API a satellite module or client app uses to read data from — and take a small number of pre-approved actions against — **one** CM Free/Plus/PRO installation it is explicitly paired or keyed with. It is not:

- **CM Relay / CM Connector** — a separate, still-unbuilt concept for connecting a CM installation *out* to a future central service (OTA sync, Community discovery). See `CM_Connectivity_Community_Contract_v0.2.md` in the main CM PRO repo if you're looking for that instead.
- **A second reservation database** — CM's own JSON files under `common/data/json/` remain the single source of truth. Every Bridge endpoint reads from them directly at request time; nothing is cached or duplicated server-side.
- **A general-purpose CM API** — the surface here is deliberately narrow: exactly what a handful of real, shipped clients need, curated field-by-field. There is no `/reservations` list endpoint, no bulk export, no admin CRUD. If your use case needs something not listed below, that's a real gap — open an issue rather than reading CM's JSON files directly off disk (which would work today but isn't guaranteed not to change shape).

CM Companion (this repo) is the reference implementation: it uses only what's documented here, nothing else. If a screen needs a capability the Bridge doesn't expose, the fix is a new documented scope on the server, never a private shortcut in the client.

## Base URL

Every endpoint lives under:

```
<installation's site root>/admin/api/bridge/v1/
```

For a CM PRO install this might be `https://example.com/app_pro/admin/api/bridge/v1/`; for CM Free, `https://example.com/app/admin/api/bridge/v1/`. There is no fixed path — the base URL is whatever the installation owner gives you (manually, or via the pairing flow below, which returns it as `bridge_base_url`).

## Authentication

Two credential types, checked in this order by every endpoint except `pairing/exchange.php` and `device_stats.php` (see their sections):

1. **Module key** — a long-lived key an installation owner manually generates and hands to a specific integration (Guest Chat, Financing, ...), with a fixed, admin-assigned scope list. Not the mechanism a generic third-party client should use.
2. **Device token** — minted per (device, installation) pair via the pairing flow below. This is what CM Companion and any comparable client uses.

Either way, present the credential as a header — **never as a query parameter**:

```
X-Bridge-Key: <your key or device token>
```

(`X-Bridge-Token` is accepted as an alias for the same header, for callers that prefer that name.) A query-string fallback was deliberately never implemented: a key in a URL ends up in Apache access logs, shell history, and any proxy in the path.

On failure, every authenticated endpoint returns:

```json
{ "ok": false, "error": "unauthorized" }
```
with HTTP 401 — regardless of whether the key was simply missing, malformed, or genuinely wrong. This is intentional; the failure reason is not distinguished so a brute-force attempt learns nothing extra.

If your credential is valid but lacks the scope an endpoint requires:

```json
{ "ok": false, "error": "scope_forbidden" }
```
with HTTP 403.

## Pairing (how a device gets a token)

1. **Installation owner generates a code**, from the CM admin panel ("CM Companion" pairing screen or equivalent). This returns (to the admin UI, not to your client):
   ```json
   {
     "code": "A1B2-C3D4",
     "installation_id": "<uuid>",
     "expires_at": "2026-09-11T12:34:56+00:00",
     "bridge_base_url": "https://example.com/app_pro/admin/api/bridge/v1",
     "deep_link": "cmcompanion://pair?url=...&installation_id=...&code=...",
     "qr_svg_data_uri": "data:image/png;base64,..."
   }
   ```
   The code is single-use and expires after 10 minutes. The `deep_link`/QR are conveniences for a mobile client — nothing prevents a client from just prompting the human to type `bridge_base_url`, `installation_id`, and `code` in by hand.

2. **Your client exchanges the code** for a permanent device token:

   ```
   POST {bridge_base_url}/pairing/exchange.php
   Content-Type: application/json

   { "installation_id": "<uuid>", "code": "A1B2-C3D4", "device_label": "My phone" }
   ```

   This endpoint is deliberately **not** behind `X-Bridge-Key` auth — the code itself is the one-shot credential for this single call, the same reasoning as any OAuth-style code exchange.

   Success (HTTP 200):
   ```json
   { "ok": true, "device_token": "<64 hex chars>", "scopes": ["dashboard.today", "dashboard.alerts", "dashboard.inquiries", "action.inquiry_respond"] }
   ```

   Failure (HTTP 401 or 400), one of:
   `missing_installation_id` · `missing_code` · `invalid_code` · `code_already_used` · `code_expired` · `installation_mismatch`

   Store `device_token` — that's what you send as `X-Bridge-Key` on every subsequent call. The scopes granted to a freshly-paired device are currently fixed (see above); there is no way for a client to request a narrower or broader set at exchange time.

3. **Self-service unpair**, any time, using the device's own token as proof of identity — no separate confirmation step, no scope check beyond "this token is currently valid":

   ```
   POST {bridge_base_url}/pairing/unpair.php
   X-Bridge-Key: <device_token>
   ```
   Response: `{ "ok": true }` or `{ "ok": false }` (token wasn't valid/already revoked). There is no way to unpair a *different* device by id through this endpoint — that's an admin-only action inside CM itself.

## Endpoints

All request bodies are JSON; all responses are JSON with `Content-Type: application/json`. Every response includes a top-level `"ok"` boolean.

### `GET ping.php`
**Scope:** none beyond a valid credential.
Liveness + auth + scope introspection — call this first when your client connects to a new installation, to confirm the credential works and see what it's actually allowed to do.

```json
{ "ok": true, "cm": { "tier": "pro", "version": "..." }, "module": "device:My phone", "scopes": ["dashboard.today", "..."] }
```

### `GET reservation_by_id.php?id=<reservation id>`
**Scope:** `reservation.basic`, `reservation.access`, `reservation.financial`, and/or `reservation.fiscal` — the response is projected down to whatever your scopes unlock, field by field (see [Field scopes](#field-scopes) below).

```json
{ "ok": true, "found": true, "reservation": { "id": "...", "unit": "A1", "from": "2026-09-05", "to": "2026-09-19", "status": "confirmed", "guest": { "name": "..." } } }
```
or `{ "ok": true, "found": false }` if no reservation with that id exists — this is not an error, a 200 either way.

### `POST reservation_by_pin.php`
Same field-scope projection as `reservation_by_id.php`, looked up by the guest-facing `door_pin` instead of the internal reservation id — POST, not GET, so the PIN never ends up in a server access log.

```json
{ "pin": "586224", "before_days": 1, "after_days": 1 }
```
`before_days`/`after_days` define how far outside the actual stay dates a PIN is still considered valid (the server enforces no PIN policy itself — that's entirely the caller's business rule). Response shape identical to `reservation_by_id.php`.

### `GET site_settings.php`
**Scope:** `settings.public`.
Minimal, non-sensitive settings projection:
```json
{ "ok": true, "settings": { "public_base_url": "https://example.com", "email": { "enabled": true, "from_email": "...", "from_name": "...", "admin_email": "..." } } }
```

### `GET dashboard/today.php`
**Scope:** `dashboard.today`.
Host/owner-context, aggregate-only — today's arrivals, departures, and currently-hosted stay per unit, with guest **counts** only (never a guest's name/phone/email). Also carries the installation's own product tier, so a client can adapt its UI without a separate round-trip.

```json
{
  "ok": true, "date": "2026-09-11", "tier": "pro",
  "totals": { "arrivals": 1, "arrival_guests": 3, "departures": 0, "departure_guests": 0, "hosting_now": 2, "hosting_now_guests": 5 },
  "units": [
    { "unit": "A1", "currently_hosting": { "id": "...", "guest_count": 3, "checkout": "2026-09-19" }, "arrivals": [{ "id": "...", "guest_count": 3 }], "departures": [] }
  ]
}
```

### `GET dashboard/alerts.php`
**Scope:** `dashboard.alerts`.
Open Time Engine / Price Engine alerts (currently: `price_opportunity_scan` output — see `Price_Engine_Workflow_Contract_v0.1.md` in the main repo). Advisory only; nothing here ever implies a price or reservation was changed.

```json
{ "ok": true, "alerts": [{ "id": "...", "unit": "A1", "type": "price_opportunity", "title": "...", "message": "...", "created_at": "..." }] }
```

### `GET dashboard/inquiries.php`
**Scope:** `dashboard.inquiries`.
Pending inquiries, summary only — dates, nights, guest-count breakdown, and a *derived* phone country (from the calling code only — the raw phone number is never returned).

```json
{ "ok": true, "count": 1, "inquiries": [{ "id": "...", "unit": "A1", "from": "...", "to": "...", "nights": 3, "created": "...", "adults": 2, "kids06": 0, "kids712": 0, "guest_phone_country": "Germany" }] }
```

### `POST action/inquiry_respond.php`
**Scope:** `action.inquiry_respond`. **The only endpoint in this protocol that can change anything.**

```json
{ "id": "<pending inquiry id>", "decision": "accept", "reason": null }
```
`decision` is `"accept"` or `"reject"`; `reason` is optional (used for reject, e.g. shown to the guest). This never writes directly — it calls the exact same in-process logic the admin panel's own Accept/Reject buttons call (`accept_inquiry.php`/`reject_inquiry.php`'s core functions), so every existing safety gate (ICS conflict check, local occupancy conflict check) applies identically. On failure the response's own `ok:false` still returns HTTP 422, not a generic 200 — check `ok`, not just the status code, but a non-2xx also reliably means "the action did not happen."

### `GET device_stats.php`
**No auth required** — deliberately public, mirroring `admin/api/counter.php`'s existing precedent. Two numbers only, safe to expose without a credential:
```json
{ "ok": true, "total_ever": 11, "active": 9 }
```
`total_ever` counts every pairing exchange that has ever succeeded (never decreases); `active` counts devices currently enabled (drops on unpair/revoke). No per-device detail (labels, tokens, timestamps) is exposed here — that stays admin-only inside CM itself.

## Field scopes

`reservation.basic`, `reservation.access`, `reservation.financial`, and `reservation.fiscal` control what `reservation_by_id.php`/`reservation_by_pin.php` return, additively — a key/token can hold more than one:

| Scope | Adds |
|---|---|
| `reservation.basic` | `id`, `unit`, `from`, `to`, `status`, `guest.name` (never phone/email/note), `door_pin` |
| `reservation.access` | `id`, `unit`, `from`, `to` (a narrower subset, for callers that only need to confirm a stay window) |
| `reservation.financial` | `payment_summary` — a curated `{status, amount_due, amount_paid, currency, paid_at, breakdown}`, never the raw `payment`/`calc` objects or any `*_token` field |
| `reservation.fiscal` | `fiscal_summary` — guest-count/keycard/payment-method fields a fiscal-voucher integration needs, nothing else |

No scope ever exposes `cancel_token`, `secure_token`, or `pdf_token` — those are credentials embedded in the reservation record, not data, and are excluded unconditionally regardless of what scopes a key holds.

## Errors you may see anywhere

| HTTP | `error` | Meaning |
|---|---|---|
| 401 | `unauthorized` | Missing/invalid credential |
| 403 | `scope_forbidden` | Valid credential, missing scope for this endpoint |
| 405 | `method_not_allowed` | Wrong HTTP method (check GET vs POST above) |
| 400 | `invalid_json_body` / `missing_<field>` | Malformed or incomplete request body |

## What will never be added to v1

- Guest PII beyond a name (phone/email/notes) — no scope exposes these, on purpose.
- A bulk/list reservations endpoint — every lookup is by a specific id or PIN.
- Any endpoint that writes without going through an existing, already-safety-gated admin action (see `action/inquiry_respond.php`'s design above — this is the pattern any future write action must follow).
