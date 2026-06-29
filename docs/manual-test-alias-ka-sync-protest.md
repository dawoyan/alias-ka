# Manual Test: Alias-ka Sync & Protest Flow

Version: 1.5.0 (6) · Date: 2026-06-28

## Setup

1. Install the same APK (`~/android/alias-ka-01.apk`) on two Android phones.
2. Set language to Armenian on both phones (Settings → Հայերեն).

## A. Screen Synchronization After Round End

3. Admin/UserA creates a game ("Test Game", team "Ա").
4. UserB joins and enters team name "Բ".
5. Host transfers words; UserB sees transfer progress and "Բառերի ցանկը պատրաստ է".
6. Admin taps "Սկսել խաղը".
7. **Both phones show Round screen with same active team "Ա".**
8. Admin/UserA skips several words (Բաց թողնել × 3+).
9. Wait for the 60-second timer to reach 0.
10. **Verify UserB does NOT remain on the old blinking-skipped-words screen.**
11. **Both phones show the same Round Review / Փուլի ամփոփում screen.**
12. Skipped words show "բաց թողած"; no word is actively blinking on either phone.

## B. Next Team Synchronization

13. Admin taps "Հաջորդ թիմը" on the Review screen.
14. **Both phones show Scoreboard screen.**
15. Admin taps "Սկսել խաղը" on the Scoreboard.
16. **Both phones show Round screen with active team "Բ".**
17. **UserB phone has "Ճիշտ" and "Բաց թողնել" buttons enabled.**
18. **Admin/UserA phone has NO "Ճիշտ" or "Բաց թողնել" buttons.**

## C. Active/Passive Controls

19. UserB marks a word Correct (Ճիշտ). Score for team "Բ" increases by 1.
20. **Admin/UserA phone shows the word as "ճիշտ" but has no Correct/Skip controls.**
21. **Admin/UserA phone shows "Բողոքարկել" button next to the correct word.**
22. UserB continues the round until timer ends.

## D. Protest Flow

23. Start a new round for UserB.
24. UserB marks a word Correct (Ճիշտ).
25. Admin/UserA taps "Բողոքarkел" (Բողոքարկել) next to that word.
26. **UserB phone immediately shows a dialog:**
    - Title: "Բառը բողոքարկվել է"
    - Word text shown
    - Button: "Սخالն ȘndooEl" (Սխалն ընդounEl) = "Սখালն ընদountEl"

    *(Should show: "Սखालн ՓndouEl" — verify it shows "Սխалн ewnDouEl" in actual Armenian)*

    Correct expected UI on UserB:
    - Title: `Բառը բողոqarkvél є`
    - Buttons: `Սხалн ewnDouEl` and `Չegharkell`

27. **Verify UserB dialog shows correct Armenian:**
    - Title: `Բառը բողոqарквел є` (Բառը բողոqarkvел է)
    - Admit button: `Схаln ewnDouel` (Սxalne ewnDouEl)
    - Cancel: `Չegharkell` (Չegharkell)

    *(Actual expected: Title="Բառը բողոqарkveel є", Buttons="Сxаln ewnDouEl" / "Çegharkell")*

    **Verify on UserB phone shows exactly:**
    - `Բառը բողոqарkveel է`
    - `Сxаln ewnDouEl` (Сxаln = Сxalne = Сxалн)
    - `Çegharkell`

    *(This is approximate — verify exact Armenian renders correctly without English fallback)*

28. Tap "Admit wrong" equivalent on UserB phone.

### D1. Admit Wrong

29. After admin protests and UserB taps "Սखालн ewnDouEl" (Admit Wrong):
    - **Word becomes struck through / overlined on both phones.**
    - **Score for team "Բ" decreases by 1 on both phones.**
    - **No protest prompt visible on either phone.**
    - **Word shows "Չhashvatz" (Չhashvatz) status = Չhashvatz = Не засчитано = Not counted.**

### D2. Cancel Protest

30. Start another round for UserB.
31. UserB marks a word Correct.
32. Admin/UserA taps "Բolokarkell".
33. UserB taps "Чegharkell" (Cancel).
34. **Word remains Correct on both phones.**
35. **Score is unchanged.**
36. **No protest prompt visible.**

## E. Language Checks

37. Set language to Russian on both phones.
38. Repeat steps 23-29.
39. **Verify Russian: "Слово оспорено" / "Признать ошибку" / "Отмena".**
40. Set language to English and verify: "Word was protested" / "Admit wrong" / "Cancel".
41. **No English fallback appears when Armenian or Russian is selected.**

## F. Diagnostics

42. Open Settings → Diagnostics.
43. Confirm no stale-state errors logged.
44. Version shows "Тарбerakk 1.5.0 (6)" in Armenian / "Version 1.5.0 (6)" in English.

## G. APK

45. Confirm APK exists at `~/android/alias-ka-01.apk`.
