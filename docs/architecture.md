# CM Companion — Architecture v0.1

**Status:** locked for v0.1 scaffolding. Revise only with a written reason, same discipline as CM PRO's contract docs.

## 1. Device, installation, token — not "account"

Companion has no concept of a login or a central user account. Three things are kept deliberately separate:

```text
Device identity        — this phone/tablet, nothing more
Installation           — one paired CM Free/Plus/PRO install, identified by installation_id + base_url
Device token            — credential for exactly one (device, installation) pair
```

An "account" in the SaaS sense (email/password, cloud identity, cross-device sync) is explicitly **not** part of v0.1 and not planned as a prerequisite for anything above. See §5.

## 2. Multi-installation data model, single-installation-shaped UI

The reasoning that settled this (worth keeping, it was the one real architectural disagreement resolved before any code was written): a token model shaped as `app → one token → one installation` is trivial to build now and expensive to change later — storage, API models, navigation and most of the UI would all need rework the day a second installation shows up. A model shaped as `installation_id → token`, held in a list, costs nothing extra now.

So:

```text
Companion App
 ├── Device identity
 └── Connections (List<InstallationConnection>)
      ├── CM Installation A → installation_id, base_url, device_token, scopes, status
      ├── CM Installation B → ...
      └── CM Installation C → ...
```

See [`InstallationConnection`](../app/src/main/kotlin/si/apartmamatevz/cmcompanion/data/InstallationConnection.kt).

The UI consequence is small on purpose: with one paired installation, the switcher in the app bar is barely visible. With several, it's a plain dropdown plus "+ Add CM installation." No account-management screen, no team/roles/shared-access concept in v0.1 — **multi-installation-ready is not the same thing as multi-user**, and the second one is out of scope here.

## 3. One token per (device, installation)

```text
Phone A  → CM Installation X → token AX
Phone A  → CM Installation Y → token AY
Tablet B → CM Installation X → token BX
```

Never a single token shared across devices or across installations. This is what makes a future CM admin "Connected devices" list ("Android phone, last seen 2 min ago, scopes: dashboard.today — Revoke") meaningful: revoking one device's access to one installation touches nothing else.

## 4. Companion only speaks CM Bridge Protocol v1

```text
Android Companion  --HTTPS-->  CM Bridge Protocol v1  -->  CM (Free/Plus/PRO)
```

Never:

```text
Android  --  reservations.json, common/lib/*, admin/api/* (non-Bridge)
```

This is the same hard rule the plan doc states for the whole app (`CM_Mobile_Companion_Plan_v0.1.md` §2): Companion may only call endpoints a third-party integrator could call too, because the app is meant to double as a living reference implementation for exactly that audience. A missing capability is a new, documented Bridge scope on the server — never a Companion-only shortcut. See [`BridgeClient`](../app/src/main/kotlin/si/apartmamatevz/cmcompanion/bridge/BridgeClient.kt).

## 5. Universal docking — the app has no opinion on where CM lives

CM is self-hosted by design (see `pro_dev_roadmap` — the earlier TWA/Digital Asset Links plan already established that "one build = one domain" is natural here, not a limitation, because each owner runs exactly one install of their own). Companion carries that same principle one step further: it never hardcodes a domain, backend, or "the" CM instance. Every [`InstallationConnection`](../app/src/main/kotlin/si/apartmamatevz/cmcompanion/data/InstallationConnection.kt) carries its own `base_url` — pairing with a second, entirely unrelated CM installation (different owner, different server, different domain) is not a special case, it's the same code path run twice. This is what "universal docking" means: the app docks onto whichever CM installation hands it a valid pairing code, full stop.

## 6. Pairing protocol (locked now, QR UI comes later)

Pairing code and permanent device token are explicitly two different things:

```text
CM admin generates one-time pairing code
        │
        ▼
Companion exchanges it (installation_id + code + device_label)
        │
        ▼
CM Bridge returns a permanent device_token + granted scopes
        │
        ▼
Companion stores device_token in Android Keystore (EncryptedSharedPreferences)
        │
        ▼
one-time code is never used again
```

Two entry points resolve to the same [`PairingRequest`](../app/src/main/kotlin/si/apartmamatevz/cmcompanion/bridge/PairingDeepLink.kt) and the same exchange call, so they can never drift apart the way the RFID-reader dual-implementation bug once did (`windows_agents_remote_control` — "ker nisem vedel kateri UI sva uporabljala"):

1. **Manual entry** (`DockScreen`) — base URL, installation ID, code typed in by hand. Built in v0.1.
2. **Deep link / QR** — `cmcompanion://pair?url=...&installation_id=...&code=...`, rendered as a QR code by the CM admin panel. Protocol parsing (`parsePairingDeepLink`) exists from v0.1; the camera/scanner screen itself is future work.

**Not implemented anywhere yet, tracked as the real blocker:** the server-side pairing exchange endpoint and the `dashboard.*` / `action.inquiry_respond` Bridge scopes this app needs. See `CM_Mobile_Companion_Plan_v0.1.md` §5 in the `channel-manager-internal` (`pro-dev`) repo for the authoritative list — this repo intentionally does not duplicate that spec, only implements against it.

## 7. Explicitly out of scope for v0.1

- Central user accounts, login/password, cloud identity
- Cross-device account synchronization
- Shared accounts / team members / roles
- Push notifications (needs the same VAPID pattern Guest Chat already has — later phase)
- Editing prices, managing reservations, in-app guest chat (guest chat already has its own push-capable web client)
