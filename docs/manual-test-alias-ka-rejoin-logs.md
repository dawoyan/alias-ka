# Manual Test: Rejoin, Session Lifecycle, and Diagnostics Logs — Alias-ka 1.6.0

## Setup

- Two Android devices with Google Play Services (API 26+)
- Install `~/android/alias-ka-05.apk` on both
- Enable Bluetooth and Wi-Fi on both devices (no internet required)

---

## Test 1 — Host stops game, client joins new game cleanly

**Steps:**
1. Device A: Create game, add two teams, start sharing.
2. Device B: Join game, enter team name, confirm words received.
3. Device A: Start game, play one round.
4. Device A: Tap Back → "Back to Home" (or navigate home during round).
5. Observe Device B.

**Expected (Device B):**
- Alert dialog appears: "Game ended"
- Tapping OK navigates to Home screen.
- No stale round/scoreboard content visible.

6. Device A: Create a new game, start sharing again.
7. Device B: Join the new game successfully.

**Expected (Device B):**
- Sees new lobby, no leftover state from the previous session.
- Diagnostics (Device B) → Session ID changes to the new session's ID.

---

## Test 2 — Host disconnects unexpectedly (force-stop app)

**Steps:**
1. Device A: Create + share game. Device B: Join and confirm connected.
2. Device A: Force-stop the app (via Android task manager or `adb shell am force-stop`).
3. Observe Device B within ~5 seconds.

**Expected (Device B):**
- Alert dialog: "Connection to host was lost"
- Tapping OK navigates to Home.
- Mode shown in Diagnostics switches from "Connected" → (reset to Idle after dismissal).

---

## Test 3 — Stale snapshot rejected after host restart

**Steps:**
1. Device A: Create game (ID = A1). Device B: Join, receive first snapshot.
2. Device A: Go Home (broadcasts SESSION_ENDED).
3. Device B: Should be on Home now (from Test 1 flow).
4. Device A: Create new game (ID = A2), start sharing.
5. Device B: Join new game — receives snapshots from session A2.

**Expected (Device B):**
- Diagnostics shows Session ID = A2 (not A1).
- No state from A1 leaks into the A2 game.

---

## Test 4 — Diagnostics shows correct client mode

**Steps:**
1. Device B: Open app, go to Settings → Diagnostics.
2. Check Mode field.

**Expected:** Mode = "Idle"

3. Device B: Tap "Join Game" and start discovery.
4. Device B: Open Diagnostics (during discovery).

**Expected:** Mode = "Joining"

5. Device A: Share game. Device B: Join and confirm connected (round not started yet).
6. Device B: Open Diagnostics.

**Expected:** Mode = "Client" (Connected)

7. Device A: Go Home (SESSION_ENDED). Device B: Dismiss alert. Open Diagnostics.

**Expected:** Mode = "Idle" (state was cleared).

---

## Test 5 — Clear Logs button

**Steps:**
1. Play at least one round to populate logs.
2. Device A: Settings → Diagnostics.
3. Tap "Clear Logs".
4. Confirm dialog appears: "Are you sure you want to clear logs?"
5. Tap "Clear".

**Expected:**
- Log list is now empty (or shows only the LOGS_CLEARED entry with timestamp).
- Timestamp of the clear entry follows HH:mm:ss.SSS format.

**Steps (cancel path):**
6. Tap "Clear Logs" again.
7. Tap "Cancel".

**Expected:** Logs unchanged.

---

## Test 6 — Timestamps on log entries

**Steps:**
1. Device A: Create game, start sharing, then go Home.
2. Device A: Settings → Diagnostics.
3. Inspect each log entry.

**Expected:**
- Every line starts with a timestamp in `HH:mm:ss.SSS` format (e.g., `14:32:07.423 INFO SESSION_CREATED ...`).
- `SESSION_ENDED` log entry visible with correct timestamp.

---

## Test 7 — Host mode (Device A) Diagnostics

**Steps:**
1. Device A: Create game, start sharing.
2. Device A: Settings → Diagnostics.

**Expected:**
- Mode = "Host / manual"
- Session ID visible.
- Host endpoint = (none / not shown since host has no upstream endpoint).

---

## Regression — Protest flow still works across sessions

**Steps:**
1. Device A: Create game with two teams (one manual, one connected from Device B).
2. Play a round. Device B: tap Protest on a Correct word.
3. Device A: Admit wrong / Cancel.

**Expected:**
- Protest dialog appears on Device A.
- Admitted word shows struck-through + "Not counted"; score −1.
- Cancelled protest: word stays Correct, score unchanged.

---

## Notes

- If Device B shows no alert after host stops: confirm Wi-Fi is on and SESSION_ENDED appears in Device A logs.
- Diagnostics session ID and host endpoint fields only appear when non-null (they are hidden in Host mode or after disconnect+clear).
