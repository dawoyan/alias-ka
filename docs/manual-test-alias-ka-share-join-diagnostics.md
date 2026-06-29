# Alias-ka 1.4 share, join, transfer, and diagnostics test

1. Install `/Users/vahramdavoyan/android/alias-ka-03.apk` on two Android phones with Google Play services.
2. Open Diagnostics and confirm Copy Diagnostics is present. If errors exist, confirm per-error Copy and Copy All Errors buttons.
3. Copy a report and paste it elsewhere; verify version/build/device/Android/mode/state/screen/permissions/connection and full technical logs.
4. On phone A create a game and tap Share Game. Confirm a localized permission explanation appears before the Android dialog.
5. Grant permissions and confirm advertising starts automatically with the available-room status. Confirm no `ACCESS_WIFI_STATE missing` failure.
6. Deny once on a repeat installation if practical; confirm a localized failure, retry capability, ERROR log, and copyable technical detail.
7. On phone B tap Join Game. Grant permissions, pull downward, and confirm discovery clears/restarts. Repeat with the visible Refresh button.
8. Select A's room, enter a team name, and confirm Receiving Words plus `received / total` progress appears.
9. Confirm B does not enter the lobby before the transfer completes and the session snapshot is available.
10. Confirm Word Collection Ready appears and B then enters the lobby. Verify B's received count equals A's active imported/bundled collection.
11. With a host-imported CSV, start the game and confirm B observes host words rather than B's previous local collection.
12. Interrupt a transfer if possible; confirm failure/retry instead of a broken lobby.
13. Start a round and press Back once or twice on either device. Confirm the active round remains running and the localized safety message appears.
14. From Join press Back and confirm discovery stops and Home appears. Double Back exits only from safe Home.
15. Confirm `/Users/vahramdavoyan/android/alias-ka-03.apk` exists.
