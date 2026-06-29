# Manual Test: Armenian Translations & Hint Feature — Alias-ka 1.7.0

## 1. Language / Translation audit

### 1.1 Switch to Armenian

1. Open app → Settings → Language → select **Հայerern** (Armenian).
2. Restart app (force-close and reopen if needed).
3. Walk through every screen and verify: no visible Latin transliteration in any UI label.
   - Home screen: tagline, button labels
   - Create game: field labels
   - Join game: search status, field labels
   - Lobby: team list, buttons
   - Round screen: team name, score, time, word buttons
   - Round review: word status labels
   - Scoreboard: headers, team rows
   - Game over: winner label
   - Settings: all labels
   - Diagnostics: all labels and mode strings

**Pass**: every string displays in Armenian script (ա-ֆ range), no Latin letters visible.

### 1.2 Team name labels

1. (Admin phone) Create game → confirm the team name field label reads **Թimmi anoun** (generic: "Team name", not "Enter your …").
2. (Join phone) Join game → after selecting a room, confirm the team name field reads **Мutkagrek dzer timi anounn** (personal: "Enter your team name").

**Pass**: two distinct labels used in the correct screens.

---

## 2. Hint feature — single-device (host only)

### 2.1 CSV with hints

1. Prepare a CSV file:
   ```
   word,hint
   Արarат,Mountain in Armenia
   Sevan,Largest lake
   Yerevan,Capital city
   ```
2. Go to Settings → Import CSV → select the file.
3. Start a game (single-device / admin mode).
4. During the round, skip a word.
5. After timer ends, the board enters **UNGUESSED_REPLAY** phase — skipped words blink.
6. **Double-tap** a blinking skipped word.
7. A popup appears: title = **Hint**, text = the hint from the CSV (e.g. "Mountain in Armenia").
8. Tap **Close** — popup dismisses.

**Pass**: hint shows correctly for words that have one.

### 2.2 Word with no hint

1. Import a CSV with one-column format (no hints):
   ```
   word
   Apple
   Banana
   ```
2. Play a round, skip a word, double-tap in replay.
3. Popup appears: title = **Hint**, text = **No hint** (or equivalent in current language).

**Pass**: popup shows gracefully even when no hint is defined.

---

## 3. Hint feature — multi-device (privacy test)

### Setup

- Device A = host (creates and shares game).
- Device B = joins as a team.

### 3.1 Hint delivered only to active team

1. Import a CSV with hints on Device A.
2. Start game. Device B's team is active first.
3. On Device B (active team), double-tap a blinking skipped word during replay.
4. Hint popup appears on Device B.
5. Confirm: Device A (host) does **not** show a hint popup.
6. Confirm: any other observer device does **not** receive the hint.

**Pass**: hint is unicast to the requesting device only.

### 3.2 Hint denied for non-active team

1. Device A's team is active for a round.
2. Device B (passive/observer) tries double-tapping a word card (if the card is even interactive — it should not be, since `playerControls = false` for non-active device).
3. No hint request is sent; no popup appears on Device B.

**Pass**: double-tap has no effect for non-active team devices.

### 3.3 Hints not transmitted with word collection

1. On Device A, check Diagnostics → Info logs.
2. Confirm no `HINT_RESPONSE` or hint text appears in the word-transfer phase.
3. Confirm only `WORD_COLLECTION` events carry word text, not hints.

**Pass**: hint data never leaves the host except on-demand via `HINT_REQUESTED`.

---

## 4. Regression checks

- [ ] Non-hint CSV (plain one-column) still imports correctly.
- [ ] BOM-prefixed CSV still works.
- [ ] Duplicate detection still works with hint CSV.
- [ ] Round flow (correct, skip, replay, end, review) unaffected.
- [ ] Protest flow unaffected.
- [ ] Russian and English translations unaffected.
