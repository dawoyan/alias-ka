# Alias-ka 1.3 navigation, import, and round test

1. Select Armenian and open Settings. Confirm `Տարբերակ 1.3.0 (4)` is visible at the bottom.
2. Press Android Back once; confirm Home appears and the app stays open.
3. On Home press Back once; confirm `Ելքի համար կրկին սեղմեք`. Press again within two seconds and confirm exit.
4. Reopen, enter a lobby, and confirm Back returns Home. During an active round confirm Back shows the localized safety message without ending the round.
5. Import CSV A and start a game; confirm only A words appear. Import CSV B and confirm A and bundled words no longer appear.
6. Import an invalid/empty file and confirm the prior working collection remains active. Explicitly delete the custom collection and confirm bundled fallback returns.
7. Create exactly two teams and start a round. Confirm ten compact rows, timer, score, and both controls fit without scrolling.
8. Confirm `Բաց թողնել` stays on one line.
9. Mark all ten cards Correct before timeout. Confirm a fresh masked page of ten loads, the first card activates, score continues, and timer does not reset.
10. Complete several cards on page two, let time expire, and confirm review includes words from both pages.
11. In another round skip at least two cards. After card ten, confirm both blink.
12. Select one skipped card and confirm every other skipped card immediately stops blinking.
13. Press Skip and confirm remaining skipped cards blink again. Select one and press Correct; confirm +1 and only remaining unguessed cards resume blinking.
14. Open Diagnostics. Confirm version/build/language/word count/source/state/screen/team count/mode/connection appear with localized headings.
15. Confirm Errors appear first when error logs exist; otherwise Information logs appear and the page is not empty.
16. Confirm `/Users/vahramdavoyan/android/alias-ka-02.apk` exists.
