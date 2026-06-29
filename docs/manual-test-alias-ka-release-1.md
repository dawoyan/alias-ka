# Alias-ka Release 1 manual test

Build with `./gradlew copyAliasKaApk`, then install `~/android/alias-ka-01.apk` on three Android phones with Google Play services.

1. Disable mobile data and disconnect internet Wi-Fi where possible.
2. On phone A create a game, edit its suggested name, and enter the host team name.
3. Add two manual teams; rename, reorder, and remove/re-add one. Start and complete a round from phone A.
4. Confirm Correct adds one, no Skip/Pass/Wrong action exists, the timer opens review, and the next team rotates.
5. Return home and create another game. Tap Share Game and grant nearby permissions.
6. On phones B and C tap Join Game, grant permissions, select phone A's room, and enter unique team names.
7. Confirm both connected teams appear on A. Start the game.
8. Mark a word correct. Confirm observers show the word and highlight it. Dispute it and confirm the point is removed.
9. In review, finalize the dispute both ways in separate rounds and verify scoring.
10. Disconnect the host and confirm clients do not continue authoritative gameplay.
11. Play until a team reaches 50. Confirm every team completes the same number of rounds before a unique leader wins.
12. Create a tie at or above 50 and confirm complete sudden-death cycles continue.
13. Confirm Armenian, Russian, and English device locales load translated primary navigation.
14. Confirm `~/android/alias-ka-01.apk` exists.

Release 2: import one-word-per-row CSV files from Settings, persist/select them locally, and have the host share the active wordset with clients.
