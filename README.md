# CM Companion

CM Companion is the official Android app for [CM Free / Plus / PRO](https://github.com/viljemd-hub/channel-manager) — a self-hosted reservation and channel-manager system for small accommodation providers.

Pair your phone with your own CM installation in under a minute — no account to create, no cloud middleman — and get today's arrivals/departures, open alerts, and guest inquiries on your phone instead of having to open the admin panel on a laptop. Works with any CM tier reachable over HTTPS: CM Free (self-hosted, free to run), Plus, or PRO.

It is an open-source reference client: everything it does, it does through CM's public **Bridge Protocol v1**, the same API surface any third-party integrator would use. There is no private shortcut into CM's internals anywhere in this app. If a screen needs a capability the Bridge doesn't expose, that's a documented Bridge scope added on the server side — never a special case here.

**Status: working, live-tested.** Pairing (manual entry + deep link), the four v0.1 tabs, and all `dashboard.*` / `action.inquiry_respond` Bridge scopes are built and tested end-to-end against real CM Free, Plus, and PRO installations, including multi-installation docking (two+ installations paired on the same phone at once). CM Free ships this pairing flow out of the box, and a signed APK is published from CM Free's own download page — anyone running CM Free can hand a guest-free download link to their own phone with no build step. See [Status](#status) below for what's still open.

## Why this exists

CM has grown from a single admin panel into a small ecosystem of independent connector modules (Guest Chat, Financing, Davčna blagajna, Market Lab, CM Connector), all talking to CM core over Bridge Protocol v1. None of them are mobile-first, and CM's existing mobile access was built for guests, not for the property owner checking today's arrivals from their phone.

Rather than wait for an outside developer to build a companion app against these connectors, this app is being built in the open from day one. Two goals at once:

1. **A real tool** — a phone-first dashboard for CM owners: today's arrivals/departures, open Time Engine / Price Engine alerts, pending inquiries, connector status.
2. **A living reference implementation** — proof that the Bridge Protocol is sufficient on its own, and a real, buildable example for anyone who wants to build their own client against it.

Being public and open-source from the start is also a deliberate choice to put CM in front of an audience — Play Store listings and GitHub contributor traffic — that a self-hosted admin panel alone doesn't reach.

## Universal docking

CM is self-hosted: every owner runs their own installation, on their own domain, with their own data. Companion never hardcodes which CM installation it talks to. Instead, each paired installation is a **connection** the app "docks" onto:

```text
CM admin panel generates a one-time pairing code
        │
        ▼
Companion exchanges it for a permanent, per-device token
        │
        ▼
token is stored in Android Keystore, scoped to exactly this
(device, installation) pair — nothing shared, nothing global
```

Pairing works by typing the code and URL in by hand, or — once the scanner screen is built — by scanning a QR code the CM admin panel renders. Both resolve to the exact same exchange call, so they can never quietly drift apart.

The app can hold **more than one paired installation at once** (one owner with two properties, or two entirely unrelated CM installs on the same phone) without that being a special case — it's the same connection model used once or many times. See [`docs/architecture.md`](docs/architecture.md) for the full reasoning, including why this was deliberately built into the data model from v0.1 even though the UI stays effectively single-installation until a second one is actually paired.

## CM Community

CM Community is the wider idea CM Companion is a first step toward: a voluntary network of independent, self-hosted CM installations that can optionally share selected public data with each other or with a public discovery layer — think shared availability search, referrals when a property is fully booked, a public discovery portal, or AI-assisted natural-language search over real (never invented) CM data.

Community membership is deliberately **separate from OTA connectivity** (Booking.com/Airbnb sync via CM Relay) — an installation can join Community without ever connecting to an OTA API, and vice versa. Nothing here is implemented yet beyond the opt-in switches already present in CM Connector; this repo doesn't try to build Community itself. What it *does* commit to is the same principle Community depends on: an app or service outside CM's own codebase should never need more than a well-scoped, documented API to be useful. Companion proves that for the "owner's own phone" case; Community, later, proves it for the "network of installations" case.

## Architecture

See [`docs/architecture.md`](docs/architecture.md) for the full breakdown: device vs. installation vs. token, why multi-installation is a v0.1 data-model decision rather than a v2 migration, and the exact pairing protocol.

Short version:

```text
Android Companion  --HTTPS-->  CM Bridge Protocol v1  -->  CM (Free/Plus/PRO)
```

Kotlin, Jetpack Compose, OkHttp, no Room/server-side database — a thin client, on purpose.

## Status

Built and live-tested against real CM installations:

- Multi-installation connection model + Keystore-backed token storage
- Bridge Protocol v1 HTTP client (GET/POST, header-only auth)
- Pairing protocol (manual entry + `cmcompanion://pair` deep link parsing), including QR pairing from the CM admin panel
- Server-side pairing exchange, device tokens, and self-service unpair
- Server-side `dashboard.today`, `dashboard.alerts`, `dashboard.inquiries`, and `action.inquiry_respond` Bridge scopes, on both CM Free and CM PRO
- The four v0.1 tab screens (Today / Alerts / Inquiries / Connection), all live, not placeholders
- Multi-installation docking tested for real: the same phone paired to a CM Free install and a CM PRO install at once

Not yet built:

- Plus-tier availability query (cross-installation sweep) — architected for, not implemented
- Explicit pairing test against a CM Plus installation (only Free and PRO have been paired so far)
- App launcher icon (deliberately deferred until Free/Plus/PRO all reach parity)
- Play Store listing / Play App Signing enrollment

The server-side plan for the Bridge scopes above lives in `CM_Mobile_Companion_Plan_v0.1.md` in the main CM PRO working repo — this app is built against that plan, not duplicating it.

## Building

Requires Android Studio (Jellyfish or newer) or a local JDK 17 + Gradle 8.7 install. This repo does not commit the Gradle wrapper jar — open the project in Android Studio and let it regenerate `gradlew`, or run `gradle wrapper` once before using `./gradlew`.

```
git clone https://github.com/viljemd-hub/cm-companion.git
cd cm-companion
# open in Android Studio, or:
gradle wrapper && ./gradlew assembleDebug
```

## Contributing

Contributions are welcome — this app exists partly *because* one person can't be the only client of the Bridge Protocol and still call it a public API. Good first areas: the four dashboard tab screens, the QR pairing scanner, or trying the Bridge client against your own CM Free/Plus install and reporting where the protocol falls short.

Please open an issue before a large PR — several pieces here (the pairing protocol, the multi-installation data model) were deliberate architectural decisions with reasoning recorded in `docs/architecture.md`; changes to those should start as a discussion, not a surprise diff.

## License

AGPL-3.0, matching [CM Free/Plus](https://github.com/viljemd-hub/channel-manager). See [LICENSE](LICENSE).
