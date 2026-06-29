# Alias-ka 1.2 round-board manual test

1. Open Settings, choose Armenian, and confirm Home and Settings contain no English action or empty-state labels.
2. Import Armenian CSV A, then CSV B. Confirm only CSV B remains as the custom collection and is active.
3. Try an empty CSV and confirm the previous collection remains active.
4. Create a game with exactly two teams and confirm Start Game is enabled.
5. Start a round and confirm exactly ten cards appear; cards 2–10 are masked and card 1 is revealed.
6. Confirm card 1 highlights exactly twice and remains bold, with `Ճիշտ` and `Բաց թողնել` enabled below the board.
7. Tap Skip. Confirm card 1 stays visible, regular, inactive, and unguessed; card 2 reveals, highlights twice, and becomes bold.
8. Tap Correct. Confirm +1, card 2 becomes inactive, and card 3 activates.
9. Complete the normal pass. Confirm correct cards remain visible and unguessed cards highlight after card 10.
10. Tap any unguessed card. Confirm only it becomes bold/active and Correct/Skip are enabled.
11. Tap Skip and confirm it stays unguessed and selectable without scoring. Select it again, tap Correct, and confirm exactly +1.
12. Confirm an already correct card cannot be activated or scored again.
13. Let the timer expire and confirm all controls/animations stop and Armenian review labels appear.
14. With a second phone connected, confirm matching blur/reveal/active states and that the observer cannot use Correct or Skip.
15. Confirm the launcher is a speech/word-card icon and `/Users/vahramdavoyan/android/alias-ka-01.apk` exists.
