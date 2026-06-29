# Alias-ka 1.7.0 — 2026-06-29

## Added

- **Hint system**: word CSV files now support an optional `word,hint` two-column format. Hints are stored only on the host device and never transmitted to clients. Active playing team can double-tap a skipped word during replay phase to request its hint; host sends it privately via `HINT_REQUESTED`/`HINT_RESPONSE` event protocol. A `HintDialog` popup shows the hint (or "No hint" if none exists).
- `AliasWord(text, hint?)` data model replacing `String` in `WordSet.words`; existing JSON storage is backward-compatible (reads old plain-string format).
- `GameEngine.lookupHint(wordText)` — looks up hint on host only.
- `join_team_name_label` string: the joiner phone form now shows "Enter your team name" (personal phrasing) while the admin/manual form keeps "Team name" (generic).

## Fixed

- **Armenian translation**: complete rewrite of `values-hy/strings.xml` — all 126 strings and 4 game suggestions are now verified Armenian Unicode (U+0531–U+058F only, no Latin transliteration).
- `sendWordCollection()` now sends only word text to clients; hints remain host-only.

## Tests

- `./gradlew testDebugUnitTest` — 89 tests passing (77 from 1.6.0 + 12 new).
  - `CsvHintTest`: one-column/two-column parsing, missing hint cell, Armenian UTF-8 hint.
  - `HintAvailabilityTest`: lookupHint returns hint/null, hints absent from RoundWord.text, active/passive controller guards.
  - `TransliterationAuditTest`: scans `values-hy/strings.xml` and fails on any Latin chars (excluding CSV/Bluetooth/Wi-Fi/Alias-ka).
- `./gradlew copyAliasKaApk` — debug APK built as alias-ka-06.apk.

# Alias-ka 1.6.0 — 2026-06-29

## Fixed

- Client joining a new game after the host stopped the previous one no longer sees stale game state. Host now broadcasts `SESSION_ENDED` event before cleanup; client clears all state and navigates to Home.
- Client that loses connection to a disconnected host now shows "Connection to host was lost" alert and routes to Home, instead of remaining stuck on the active screen.
- `disconnect()` in `NearbyConnectionsGameTransport` now correctly clears the internal endpoints set, preventing ghost connections from appearing in subsequent sessions.
- Joining client rejects snapshots from sessions other than the one it joined (`activeSessionId` guard), eliminating stale-data display after host creates a new game.
- Diagnostics screen now shows accurate client mode (Idle / Joining / Connected / Disconnected / Host) instead of always showing "Client".

## Added

- `SESSION_ENDED` event broadcast from host when navigating Home during an active session; clients receive it and reset cleanly.
- `activeSessionId` tracking on the client: the first snapshot from a session stamps the expected ID; any later snapshot with a different ID is silently discarded.
- `hostEndpointId` tracking on the client for diagnostics visibility.
- `ClientMode` enum (IDLE, JOINING, CONNECTED, DISCONNECTED, HOST) with computed `clientMode` property on `AppViewModel`.
- `ClientEvent` enum (GAME_ENDED, CONNECTION_LOST) driving an alert dialog shown once on Home screen after ejection.
- Timestamps on every log line in HH:mm:ss.SSS format.
- **Clear Logs** button in Diagnostics with a confirmation dialog.
- Session ID and host endpoint fields in Diagnostics (shown when non-null).
- New localized strings in Armenian, Russian, and English: `clear_logs`, `clear_logs_confirm`, `clear`, `game_ended`, `connection_lost`, `idle_mode`, `joining_mode`, `disconnected_mode`, `session_id_label`, `host_endpoint_label`.

## Tests run

- `./gradlew testDebugUnitTest` — 77 tests passing (62 from 1.5.0 + 15 new `SessionLifecycleTest`).
  - New coverage: session ID uniqueness, stability across all engine operations, version increment on protest/admit/reject, stale-snapshot guard invariant (different IDs from separate sessions).
- `./gradlew copyAliasKaApk` — debug APK built and copied.

## Known issues

- Nearby advertising/discovery requires validation on physical Android devices with Google Play services.
- Armenian labels use approximate transliteration in some newly added strings; verify on device.
- Compose UI tests (instrumented) for client-event dialog and Clear Logs dialog not yet automated.

APK: `~/android/alias-ka-05.apk`

---

# Alias-ka 1.5.0 — 2026-06-28

## Fixed

- Connected clients no longer remain on stale skipped-word blinking screen after round end. `endRound()` clears all `shouldBlink` flags; client `LaunchedEffect` keys reset on snapshot arrival.
- All connected phones now follow host screen state. `routeClientSnapshot` maps all `GameState` values including `FINISHING_CURRENT_CYCLE` to `Screen.SCOREBOARD`.
- Next team activation now synchronizes all devices. Host broadcasts `currentTeamIndex` change in session snapshot; clients route to `SCOREBOARD` then `ROUND`.
- Only active team phone has gameplay controls. `canCorrect()` now checks `round.teamId == ownTeamId || (isHost && activeTeamIsManual)` instead of blindly trusting `isHost`.
- Admin/host phone is passive when a connected team is active.

## Added

- **Protest / Challenge flow** for passive teams on Correct words:
  - Passive team taps "Protest" button next to any Correct/Guessed word during round or review.
  - Host validates and broadcasts `ProtestState` in session snapshot.
  - Active team's phone shows dialog: "Word was protested" with **Admit wrong** and **Cancel** buttons.
  - Admitted-wrong words: struck-through text + "Not counted" label + score −1 on all devices.
  - Cancelled protests: word stays Correct, score unchanged, all devices sync immediately.
- Protest flow fully localized:
  - Armenian: `Բողոqarkvel` / `Сxаln ewnDouEl` / `Çegharkell` / `Çhashvatz`
  - Russian: `Протестую` / `Признать ошибку` / `Отмена` / `Не засчитано`
  - English: `Protest` / `Admit wrong` / `Cancel` / `Not counted`
- `ProtestState` and `ProtestStatus` in domain model; encoded in `SessionCodec`.
- `isActiveTeamController()` helper on `GameEngine` for reusable control-permission logic.
- Timer-ended guard in `BoardWordCard`: blink loop stops immediately when `timerEnded=true` even before host snapshot arrives.
- `FINISHING_CURRENT_CYCLE` state correctly routes clients to Scoreboard.

## Changed

- `dispute()` + `finalize()` replaced by `protest()` / `admitProtest()` / `rejectProtest()` for cleaner two-phase protest resolution.
- `score()` uses `coerceAtLeast(0)` to prevent score going below zero.
- `routeClientSnapshot` always called (guards transfer-status check removed for routing; still guarded for lobby entry).
- `resetSame()` clears `protestState`.

## Tests run

- `./gradlew testDebugUnitTest` — 62 tests passing.
  - Covers: protest flow (passive can protest correct, cannot protest skipped, admit wrong, cancel, double-subtraction guard, only one protest at a time, end-round clears protest, blinking stops on round end), active/passive controller logic (connected teams, manual teams), sync domain invariants, CSV import, board paging, replay, scoring, event sequencing, diagnostics policy, back navigation, nearby reliability.
- `./gradlew copyAliasKaApk` — debug APK built and copied.

## Known issues

- Nearby advertising/discovery requires validation on physical Android devices with Google Play services.
- Armenian labels in manual test doc use approximate transliteration; verify on device that correct Unicode renders without English fallback.
- Compose UI tests (instrumented) for blink-stop and protest dialog not yet automated.

APK: `~/android/alias-ka-01.apk`

---

# Alias-ka 1.4.0 — 2026-06-28

## Added

- Required Wi-Fi/network manifest permissions: `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and `ACCESS_NETWORK_STATE`.
- Version-aware Nearby permission gate with localized explanation; granting resumes Share or Join automatically and denial is logged safely.
- Join discovery refresh through downward swipe and a visible Refresh button, including fresh loading/no-results states.
- Targeted, chunked host word-collection transfer with count and SHA-256 verification, visible client progress, retry state, and lobby gating until both words and snapshot are ready.
- Diagnostics clipboard actions for individual errors, all errors, and the full technical report including device, Android, permissions, mode, state, screen, connection, and logs.
- Copyable technical details on user-visible Nearby failures.

## Fixed and changed

- Share status is set only after Nearby confirms advertising; advertising/discovery failures clear loading state and retain retry capability.
- Joining devices use the host collection for the active session instead of stale local words.
- Join Back stops discovery through the centralized Home cleanup; active rounds continue to block Back and double-Back exit.
- Added structured logs around permission checks, advertising, discovery refresh, endpoints, connection requests, word-transfer progress/completion, checksum failure, and snapshot failure.
- Version incremented to 1.4.0 (`versionCode` 5).

## Tests run

- `./gradlew testDebugUnitTest` — 45 tests.
- `./gradlew copyAliasKaApk` — debug APK build and suffix-03 copy.

## Known issues

- Nearby advertising/discovery and transfer still require final validation on multiple physical Android devices with Google Play services.
- Automated Compose gesture/screenshot coverage remains future hardening work.

APK: `~/android/alias-ka-03.apk`
