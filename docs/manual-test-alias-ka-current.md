# Alias-ka current revision manual test

1. Install `~/android/alias-ka-01.apk` and confirm the launcher uses overlapping speech/word cards rather than a Latin letter.
2. Open Settings, select Armenian, and confirm Home shows `Ստեղծել խաղ`, `Միանալ խաղին`, and `Կարգավորումներ`.
3. Tap CSV import and choose a UTF-8 file containing `word`, Armenian words, duplicates, and empty rows. Confirm imported, duplicate, and empty counts.
4. Rename the imported wordset, restart the app, confirm it remains available, and select it as active.
5. Create a game, enter the admin team, add exactly one opponent, and confirm Start Game is enabled with two teams.
6. Start and confirm words come from the selected imported wordset.
7. Tap `Ճիշտ`; confirm the score increases by one and the list shows `ճիշտ` without pending terminology.
8. Tap `Բաց թողնել`; confirm score is unchanged and the word remains in the skipped list.
9. Process ten normal words. Confirm skipped cards highlight at the bottom.
10. Select a skipped card, confirm it becomes the large active card and `Գուշակված` appears, then tap it and confirm +1.
11. Let another skipped word remain unresolved until timeout and confirm review shows it as not counted.
12. Complete rounds until 50; confirm every team receives equal rounds and tied teams enter another full cycle.
13. On a second phone, tap Join Game and connect in-app. Confirm the team appears on the host with only two total teams needed.
14. Confirm the observer sees all correct/skipped words, correct rows highlight, and Dispute removes the point for review.
15. Confirm `/Users/vahramdavoyan/android/alias-ka-01.apk` exists after `./gradlew copyAliasKaApk`.
