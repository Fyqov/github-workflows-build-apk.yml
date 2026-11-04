# Cluck & Load - progress notes

## ROADMAP v2.x (user asked for ALL 9 ideas, built on top of v1.9; v1.9 was the version the user liked best)
Stage 1 = v2.0  DONE: mod-menu extras (Kamehameha button ON by default but ONLY after the admin code; start wave slider,
  slow motion slider, boss fight picker, add coins), run modes (6 mutators, coin bonus), records screen (top 10),
  wardrobe (7 hats, 6 colours, 5 trails), drone chick helper (shop upgrade, 3 levels), menu second icon row.
Stage 2 = v2.1  DONE: flamethrower / crossbow / grenade launcher (perk unlocks, crates, keys 6-8) + Mk II upgrade cards (5 new perks).
Stage 3 = v2.2  DONE: biome mechanics (ice slide, swamp puddles/fog, autumn wind), 9 bosses (new: autumn, swamp, snow, FINAL at wave 50),
  boss every 5 waves from 10, ending screen at wave 50 + KEEP FIGHTING (endless) / FINISH RUN, achievements 'final' and 'endless'.
Stage 4 = v2.3  DONE: Russian localization (Settings -> LANGUAGE EN/RU, auto-detect on first launch, embedded Cyrillic font, tr() layer over canvas text),
  final regression, README, zip. ALL 4 STAGES (v2.0-v2.3) ARE DONE.

ROADMAP v3.x (user asked for 6 more things after v2.3)
Stage 5 = v3.0  DONE: hen active abilities (7 abilities, button + F/Shift) + 3 new hens (Ninja, Mage, Robo-Hen).
Stage 6 = v3.1  DONE: Volcano (41-50) and Space (51-60, low gravity) biomes + 2 new bosses (11 bosses; final stays at wave 50: order mama, autumn, turkey, swamp, prof, snow, ember, volcano, FINAL, void, space).
Stage 7 = v3.2  DONE: Boss Rush mode (all bosses in a row, short breaks, own record).
Stage 8 = v3.3  DONE (ALL OF v3.x IS DONE): save export/import code, gamepad support, colour-blind mode, final regression.
ROADMAP v3.4+ (user picked these after v3.3; do them in order, publish after each)
Stage 9  = v3.4  DONE: daily LOGIN streak (7-day gift cycle, boosts, streak saver, milestones), STATS tab, 20 new achievements (39 total). Tabs GOALS/STATS/STREAK.
Stage 10 = v3.5  DONE: run autosave (checkpoint at every wave start + every 4 s + app hidden/paused; CONTINUE button in the menu; NEW RUN needs a second tap).
Stage 11 = v3.6  DONE: Ukrainian + Spanish (587 strings + 48 patterns, new font subset), 4-language picker, per-biome gameplay music + per-biome boss themes + final boss theme.
Stage 12 = v3.7  DONE: balance bot, late-wave HP/damage scaling, 11 bosses calibrated (25-100 s), economy (upgrades x2.2, wave coins 3+1.5w), Brickhouse nerf, full regression.
Rules from the user: Kamehameha is ADMIN ONLY (never for normal players); keep replies short; Russian in chat;
do not re-read the whole game file, use Grep/small Read; publish artifact after every stage
(url https://claude.ai/artifact/P9HDuiHPT9SitnJBbsVLiq), bump versionCode/versionName, rezip, send zip + index.html.
Build: source of truth /home/claude/cluck-and-load/game.content.html -> build.py (in session scratchpad) wraps to index.html and copies to app/src/main/assets.

Older notes:

Status (before v2.0): v1.9 done (adds Ultra Instinct + Kamehameha toggles to the mod menu, requested by the user). v1.8 done and published (v1.8 = admin code 133700 + mod menu in Settings, requested by the user).
Earlier: v1.7 done and published. All requested items (power-ups, countdown, bosses, pause,
meta progression, hens, achievements, daily challenge, settings, vibration, music, minimap)
were finished in v1.6. v1.7 adds (self-proposed): Android Back button, Runner/Medic/Kamikaze
enemies, four arena biomes switching after each boss.

Source of truth: the game script in app/src/main/assets/index.html (single file).
Nothing is pending. Ideas for later: more bosses for biomes 2-4, biome-specific obstacles,
sound for new enemies, more achievements.
Stage 13 = v3.8  DONE: FPS setting 30/60/120 (real frame cap, 4 languages), frame-rate independent boss damping + fire rate, Android refresh-rate bridge.
Stage 14 = v3.9  DONE: multiplayer step M1 - net core (host/join/PIN/ping/heartbeat), FRIEND menu + lobby screens, LanLink.java TCP+UDP beacon, Android bridge, 4-language texts.
Stage 15 = v4.0  DONE: multiplayer M2+M3 - host-authoritative co-op (2 hens, down/revive, shared XP, per-hen perks, 2-player scaling), client renders 20 Hz snapshots with own-hen prediction, results/coins for the friend, leave/pause handling, NET_PROTO 2.
Stage 16 = v4.1  DONE: main menu cut to START + 5 buttons, START opens a mode picker (classic / challenges / boss rush), difficulty modes Relaxed-Hard-Nightmare, custom mutator run, records tab in Goals, wardrobe tab in Hens, 4 languages.
Stage 17 = v4.2  DONE: iPhone-style motion - push/pop/fade screen transitions, spring press feedback, momentum scroll + rubber band, animated switches, pop-in pause/game-over, sliding toasts, ANIMATIONS setting.
Stage 18 = v4.3  DONE: UI themes - runtime colour re-mapping of the interface only (canvas fill/stroke hook active only while HUD/menus draw), White light mode + Dark + 10 colours, smooth blend, THEME card in Settings, 4 languages.
Stage 19 = v4.3.1 DONE: all 7 hen abilities have a 20 s cooldown (user request: abilities came back too fast).
Stage 20 = v4.4 DONE: co-op M4 polish - reconnect (host pause 30 s + client auto-rejoin with run id), PLAY AGAIN rematch, ping bars, revive 1.8 s / 60%, enemy co-op scaling only while both hens are in, balance re-check of all hens and a co-op bot test (test/cobal.mjs). Bluetooth dropped by the user.
Stage 21 = v4.5 DONE: DUEL mode (1v1 over the Wi-Fi link): best of 3 rounds, shrinking zone, weapon/corn drops, PvP versions of all 7 abilities, per-hen duel damage factors tuned with bot-vs-bot (test/dbal.mjs), results screen with coins and record, forfeit if the rival leaves, NET_PROTO 3. Test: test/duel1.mjs.
Stage 22 = v4.6 DONE: FRAME RATE AUTO (follows the screen, default; 30/60/90/120 manual), robust 120 Hz request in MainActivity (mode id + preferred rate + min/max range + API 35 setRequestedFrameRate, re-applied on focus/resume, displayInfo() to JS), GRAPHICS AUTO/HIGH/MEDIUM/LOW with adaptive canvas resolution, effects thinning, cached UI world layer + vignette, idle menu half rate on >=100 Hz, calmed UI animation clock, pre-warmed slide buffers. Tests: test/gfx46a.mjs, bench46.mjs.
Stage 23 = v4.7 DONE: duel rules page (EQUAL HENS / PISTOLS ONLY / NO ZONE with 75 s health decision / ARENA), rules sent to the friend in `go` (NET_PROTO 4), rules line in the HUD, DUEL STATS screen (wins, losses, win rate, streaks, favourite hen, by hen, by rule), richer record in localStorage cluckload.duel. Tests: duel47, net47, i18n47.
Next: v4.8 = duel achievements + hat/trail rewards, quick reactions, duel music + round-win effects.
Next ideas: spectator mode, more duel polish.
Old next: M4 polish (reconnect, revive/coin tuning), M5 Bluetooth RFCOMM transport (Java + permissions), M6 duel mode.
