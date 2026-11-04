# Cluck & Load (Android)

A top-down chicken shooter. The game is a single offline HTML page
(`app/src/main/assets/index.html`) shown full-screen in a WebView, so the app
needs no permissions and no internet.

## Get the .apk

**Option A: GitHub, no PC or Android Studio needed**
1. Create a new GitHub repository and upload everything in this folder
   (including the hidden `.github` folder; if your browser skips it, use
   *Add file > Create new file*, name it `.github/workflows/build-apk.yml`,
   and paste the contents of that file).
2. Open the repo's **Actions** tab, pick **Build APK**, press **Run workflow**
   (it also runs on every push).
3. When it turns green, open the run and download the `cluck-and-load-apk`
   artifact. It is a zip containing `app-debug.apk`.
4. Copy the .apk to your phone, open it, and allow "Install unknown apps"
   when Android asks.

**Option B: Android Studio**
Open this folder, wait for Gradle sync, then *Build > Build Bundle(s) / APK(s) > Build APK(s)*.

**Option C: command line** (JDK 17 and the Android SDK installed)
```
./gradlew assembleDebug
```
The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

The debug build is signed with the standard debug key, which is fine for
installing on your own phone. For Google Play you would make a release build
with your own keystore.

## Controls

- Left thumb: move
- Right thumb: aim and fire (light aim assist)
- Weapon button (top right): swap weapons
- Desktop: WASD, mouse to aim and click to fire, Q to swap

## Progress, shop and hens

Coins are earned for every cleared wave, boss and group of kills, and are kept
between runs (saved on the phone). Spend them in the SHOP on permanent upgrades:
starting health, damage, speed, a spare weapon, a starting power-up of your
choice, XP and drops. The HENS screen has three extra chickens: Brickhouse
(tank), Hawkeye (sniper) and Boomer (bomber). Buy them with coins, or unlock
each one for free by defeating a boss (Big Mama, Gobbler, Professor Poult).
The GOALS screen has achievements with coin rewards and a daily challenge with
a different rule every day.

## Settings, music and map

The SETTINGS screen has a left-handed mode (swaps the sticks), separate sound and
music volume, aim sensitivity, aim assist strength, vibration and a minimap
toggle. Music is generated in the game itself (one theme for normal waves, a
faster one for boss fights). Vibration needs the VIBRATE permission, which is
already in the manifest. The minimap sits at the bottom centre, and arrows at the
screen edge point to pickups that are off screen.

## Pause

The pause button sits next to the sound button (top right). On PC, P or Esc
pauses too. The game also pauses by itself when the app goes to the background.

## Countdown and bosses

A 5-second countdown (with beeps) runs before every wave. **Every 5th wave from wave 10** a boss
arrives, each with its own mechanics (11 bosses, the cycle then repeats with tougher stats):

| Wave | Boss | Mechanic |
|------|------|----------|
| 10 | Big Mama Clucksworth | Charges at you; spawns chick swarms; enrages at half HP |
| 15 | Harvest Scarecrow | Pumpkin bombs, crow dives, leaf storm |
| 20 | Sergeant Gobbler | Front-arc feather shield, stomp shockwaves, fan volleys |
| 25 | Bog Hen Belladonna | Goo puddles, telegraphed leap, bog pulls |
| 30 | Professor Poult | Vanishes and decoys, homing bullets |
| 35 | Duchess Frostbeak | Ice spikes, flurry, spinning frost beam |
| 40 | Warlord Ember-Comb | Shielded by 4 totems you must destroy, fire zones, spirals |
| 45 | Magma Queen Pyra-Cluck (new) | Lava rocks, geyser walls with a gap to run through, fire rings; the floor turns to lava at half HP |
| 50 | **King Cluck the Last** | Final boss, 3 phases: volleys, rain, rings, summons, beams, storm |
| 55 | Emperor Plume | Telegraphed strikes, rotating beams, rings with a gap |
| 60 | Commander Nova-Cluck (new) | Telegraphed comet dashes with mines, meteor storms, gravity blasts that throw you far |

Beating a boss heals you and gives a reward pick with better rarity odds.
Beating **King Cluck** shows the **VICTORY ending** (+300 coins, achievement "King of the Coop"),
then you choose **KEEP FIGHTING** (endless mode - waves never stop, achievement "No End in Sight" at wave 60) or **FINISH RUN**.

## New in 4.7 - duel rules and duel stats

- **DUEL RULES screen.** In the friend lobby the host taps START DUEL and gets a rules page before the fight (the choice is remembered; PLAY AGAIN reuses it). The friend gets the same rules in the `go` message, so both phones draw the same duel. NET_PROTO is now 4.
  - **EQUAL HENS**: both players are the classic hen with the same health (110), the same level-1 upgrades (HARDY HEN, HEAVY BEAK, QUICK LEGS) and the pistol only. Looks, hats and trails stay personal.
  - **PISTOLS ONLY**: every hen keeps its own ability and stats but starts with the pistol and no weapon crates drop (corn only, a bit slower).
  - **NO ZONE**: the arena never shrinks. A round that lasts 75 seconds is decided by who has more health (equal health = draw); a warning shows for the last 10 seconds.
  - **ARENA**: random (as before) or Sunny Meadow / Autumn Farm / Murky Swamp / Frozen Field.
  - The active rules are shown next to the countdown at the start of each round.
- **DUEL STATS screen** (friend page: DUEL STATS button, results screen: STATS button): wins, losses, win rate, best and current streak, favourite hen (most matches), rounds won/lost, draws, matches and wins by hen and by rule. The data is the local record `cluckload.duel`; new counters (per hen, per rule, rounds, draws) start with this version.
- The old "BLUETOOTH SOON" placeholder on the friend page is gone (Bluetooth is not planned).
- Tests: `test/duel47.mjs` (rules, drops, no-zone timer, record), `test/net47.mjs` (rules reach the friend), `test/i18n47.mjs`.

## New in 4.6 - 120 Hz screens and smoother, lighter drawing

- **FRAME RATE: AUTO (new default).** The game now follows the screen: 120 Hz phones (e.g. Poco X7 Pro) run at 120 FPS, 90 Hz at 90, 60 Hz at 60. Before, the default was a fixed 60 FPS cap, which is why 120 Hz phones never went above 60. Manual choices: 30, 60, 90, 120. Everyone who had the old default 60 is moved to AUTO once.
- **The app asks the phone harder for 120 Hz.** `MainActivity` now sends every hint phones listen to: the display mode id, `preferredRefreshRate`, the min/max preferred rate range (reflection, API 30+) and the Android 15 `View.setRequestedFrameRate` (reflection). It re-sends them when the window gets focus or the app resumes, and starts at "fastest mode" before the game even loads.
- **Settings shows the real screen.** `Screen: 60 Hz (max 120)` under the frame-rate buttons. If the phone keeps the screen below its maximum, a hint says to set the refresh rate to High / turn battery saver off in the phone settings (some phones, e.g. HyperOS, have a per-app refresh-rate switch or a battery-saver cap).
- **GRAPHICS: AUTO / HIGH / MEDIUM / LOW.** Drawing cost is mostly pixels, so AUTO watches the gap between pictures and walks the canvas resolution down (about 2x → 1x) when frames come late, and back up after ~6 s of calm (a level that failed right after going up is banned for 90 s, so it never flickers). The lower steps also thin out particles and weather. HIGH = up to 2.5x, MEDIUM = 1.75x, LOW = 1.25x, fixed. The current sharpness is shown next to the title.
- **Lighter menus.** The world under menu screens is only a faint backdrop, so it is now painted ~8 times a second and re-used in between. On a 120 Hz screen a still menu draws every second refresh (battery), and goes back to full rate the moment a finger, a slide or a spring moves. The vignette is a cached tiny gradient. The two slide-transition buffers are allocated up front, so the first slide does not hitch.
- **No more animation jumps.** Interface animations (slides, springs, toggles) run on a calmed clock: one late picture slows them a little instead of making them jump. Gameplay still uses the real time.
- **Measurements** (`test/bench46.mjs`, headless Chromium with software drawing, 800x380 at device scale 2.5, CPU = share of one core: a relative battery / GPU-load proxy, not a phone number): a still menu went from 104% to 76% (MEDIUM) and 68% to 52% (LOW) after the world cache; HIGH (2.5x) could not hold 60 FPS in the test (36-41 FPS) and MEDIUM only in menus, while LOW (1.25x) held 60 everywhere. 30 FPS costs about 35% less than 60 FPS at the same sharpness, and a still menu on a "120 Hz" screen costs about 40% less than one that keeps drawing every refresh.
- Tests: `test/gfx46a.mjs` (AUTO default, steps down under load and back up, presets, idle half rate, 90 FPS cap).

## New in 4.5 - DUEL (1 vs 1 over Wi-Fi)

- **A new mode for two phones.** Host a game and connect a friend as usual, then press **START DUEL** (next to START CO-OP) in the lobby. Both hens spawn on opposite sides of the arena and fight: **first to win 2 rounds takes the match**.
- **Every round** starts with a 3 second get-ready countdown. Weapons and corn (health) drop in the arena during the round, so run for them. After 20 seconds a **red zone** starts to shrink towards the middle and hurts everyone outside it, so no one can hide forever.
- **Your hen, your ability.** Each player uses the hen chosen on their own phone (own weapon kit, speed and look), but shop upgrades and starter perks are switched off, and health is the hen's own base health. Abilities work against the other player: BATTLE CRY and SHIELD CHARGE as before, SHIELD CHARGE and SHADOW STEP dash through the rival for damage, BULLET TIME slows the rival and their shots, BIG BANG blasts them and wipes their bullets, METEOR SHOWER drops fireballs around them, SENTRY TURRET shoots at them for 6 seconds.
- **Coins.** The winner gets 25 coins, the loser 10; if the rival leaves in the middle you win by default (10 coins). Your wins and losses are shown on the results screen. PLAY AGAIN starts a rematch at once.
- **Balance.** Every hen has a small duel damage factor found with bot-vs-bot tests (all 7 hens win about 45-55% of the matches against each other), tests: test/duel1.mjs (two phones), test/dbal.mjs (bots).
- The Wi-Fi protocol number went up to 3, so both phones need version 4.5.

## New in 4.4 - co-op polish and balance

- **Reconnect.** If the Wi-Fi link drops in the middle of a co-op run, the run is not lost any more. The host is paused for up to 30 seconds (CONNECTION LOST panel with a countdown) and the friend's phone keeps trying to get back in by itself (RECONNECTING...). When the link is back the game goes on from the same moment, also between waves while power-ups are being picked. The host can press CONTINUE ALONE to stop waiting. A friend who leaves on purpose still ends his side at once.
- **PLAY AGAIN.** On the results screen the host can start a new co-op run with the same friend with one tap, no trip to the lobby.
- **Connection bars** next to the friend's health bar (green / yellow / red).
- **Revive** is a little quicker (1.8 s instead of 2.2 s) and brings the hen back with 60% health instead of 50%.
- **Fair difficulty when alone.** The co-op enemy boost (+50% enemies, +50% health, +90% boss health) now applies only while both hens are in the run. If the friend leaves, the host plays at normal strength.
- **Balance check.** All 7 hens were re-tested with the 20 s ability cooldown: every hen reaches about wave 33-45 with the test bot, no hen is far ahead or behind. A two-hen test team reaches about wave 30 and earns about as many coins per minute as a solo hen, so the co-op coin rewards stay as they are.

## New in 4.3.1 - one cooldown for every ability

- Every hen ability now has the same 20 second cooldown (it was 7-20 s), counted from the moment you press it. The first ability is still ready about 5 s after a run starts.

## New in 4.3 - interface themes (white, dark and 10 colours)

- Settings -> THEME: 12 looks for the whole interface - Green (classic), White, Dark, Blue, Teal, Purple, Pink, Red, Orange, Yellow, Brown, Gray.
- Switching is a smooth colour blend, not a jump. The choice is saved with the game.
- Menus, shop, hens, goals, challenges, Boss Rush, lobby, HUD, pause, perk cards, game over and toasts all follow the theme; the game world, hens, enemies and coin icons keep their own colours.
- White is a real light mode (dark text on light panels). Gray is silver, Dark is gold on near-black.
- Theme names are translated into Russian, Ukrainian and Spanish. Green = the exact old look with zero extra cost.

## New in 4.2 - smooth, iPhone-style motion

- **Screen transitions.** Going deeper slides the new screen in from the right while the old one drifts left and dims; going back does the reverse; switching between sibling screens (for example the HENS / WARDROBE tabs) cross-fades. Taps are ignored for the first moment of a transition so nothing is pressed by accident.
- **Springy buttons.** Every button, the START pill, the round menu buttons and the mode cards squeeze a little under your finger and spring back with a tiny bounce. Dragging your finger off a button cancels the press, like on iOS.
- **Momentum scrolling with a rubber band.** Lists keep gliding after you flick them and stretch past the top or bottom edge, then spring back.
- **Animated switches.** The toggles in Settings and the mutator switches glide their knob with a small overshoot and fade the track colour.
- **Pop-ups.** The pause menu and the game over panel grow in with a soft bounce; toasts slide in from the edge.
- **Settings > ANIMATIONS** turns all of this off for a plain, instant interface (saved on the phone; it is not part of the save code).

## New in 4.1 - a cleaner main menu and difficulty modes

- **Fewer buttons.** The main menu now has START plus five round buttons (SHOP, HENS, GOALS, FRIEND, SETTINGS) instead of nine. WARDROBE moved into the HENS screen (HENS / WARDROBE tabs) and RECORDS into GOALS (fourth tab). MODES and RUSH moved into the start picker.
- **START opens a mode picker** (CONTINUE still resumes a saved run straight away; NEW RUN opens the picker, and starting something new there asks you to tap twice before it throws the saved run away):
  - **Classic run** - the normal game, always without mutators.
  - **Challenges** - three difficulty modes, each with a PLAY button: **Relaxed** (enemies 30% weaker, hit 30% softer, coins halved, not counted for records), **Hard** (enemies +35% health, hit +30% harder, +50% coins) and **Nightmare** (+90% health, +70% damage, faster enemies, double coins). Below them is the **custom run** with the old mutators (they now apply only to a custom run, not to every run).
  - **Boss Rush** - the same screen as before.
- Difficulty is remembered by the run: a saved Hard run resumes as Hard, RETRY restarts in the same mode, the pause screen shows the mode, and the records list shows it next to the hen. Co-op, daily challenge and Boss Rush always use the normal numbers.
- The Android Back button goes Challenges / Boss Rush -> picker -> main menu. All new texts are in four languages.

## New in 4.0 - play with a friend, step 2: CO-OP over Wi-Fi

Two phones on the same Wi-Fi (or one phone sharing its hotspot) can now fight the waves together. Open **FRIEND**, connect like in 3.9, and the host taps **START CO-OP**.

- **Two hens, one run.** The host phone runs the whole game; the friend's phone shows it live and sends only its stick / fire / ability input. Each player keeps his own hen (look, hat, upgrades, starter perks), his own weapon, perks, level and ability. Hens are marked with a blue / pink arrow and name; an arrow at the screen edge points to the friend and a small panel shows his health.
- **Teamwork.** Enemies chase the nearest hen. A hen with no health is **down** (not dead): stand next to it for about 2 seconds to pick it up, or it gets up on its own with 40-50% health after the next wave / boss. The run ends when both hens are down at the same time.
- **Shared XP and waves, separate perks.** Experience is shared, but each player picks his own perk between waves; the game waits for both (a silent friend is auto-picked after 25 seconds). Enemies and bosses get tougher for two players (+50% health, bosses about x1.9, more enemies on screen).
- **Rewards.** The friend gets the team's coins, kills, wave and best-wave stats just like a solo run; leaving early keeps what the team earned so far. Co-op runs never touch the leaderboard, the save code or any cheats.
- **Leaving.** If the friend leaves or the link drops, the host keeps playing alone; if the host leaves, the friend gets the results screen. The host pausing shows HOST PAUSED on the friend's screen.
- Network protocol version bumped to 2 (3.9 and 4.0 phones cannot connect to each other - update both). Snapshots run at 20 Hz and use roughly 30-80 KB/s even in the late waves.
- Fixed while testing: boss health above 100 000 (waves 50+) was shown as empty, and the host camera followed the wrong hen.
- Not in this version yet: Bluetooth link, duel mode, reconnecting to a run in progress.

## New in 3.9 - play with a friend, step 1: the Wi-Fi link

The first part of local multiplayer (co-op comes next). It only works inside the Android app.

- New **FRIEND** button on the main menu. One phone taps **HOST A GAME** and sees its address and a 4-digit PIN; the other taps **JOIN A GAME**, picks the host from the list of games near it (or types the address) and enters the PIN. Both phones must be on the same Wi-Fi, or one phone shares its hotspot.
- Both phones show who is connected and the live ping. If the link drops (no answer for 6 seconds) both sides notice; the host opens its room again by itself.
- Wrong PIN and different game versions are rejected with a clear message. The screens are translated into all four languages.
- Under the hood: `LanLink.java` (TCP link plus a small UDP beacon so hosts show up in the list) is wired to the game through `CluckNative.net*` and `window.__netEv`. The game talks in one-line JSON messages, so the same game code will also run over Bluetooth later.
- New manifest permissions: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE. They are used only for the local link; the game still never goes online.
- Not in this version yet: the co-op game itself (START CO-OP is greyed out), Bluetooth, duel mode.

## New in 3.8 - frame rate setting (30 / 60 / 120 FPS)

- Settings has a new **Frame rate** card with 30, 60 and 120 FPS buttons and a live FPS counter. The choice is saved on the device (it is not part of the save code).
- It is real, not cosmetic: the game loop only updates and draws as often as the chosen rate. 30 FPS saves battery (the game still runs its physics twice per picture, so hits and bullets stay precise), 60 is the default, 120 makes motion smoother on a 120 Hz screen (on a 60 Hz screen 120 simply runs at 60).
- Inside the Android app the choice is also sent to the phone (`CluckNative.setFrameRate`), which asks the system for a matching display mode: 120 requests the highest rate the screen supports, 30 and 60 keep the screen at 60 Hz.
- The game is now independent of the frame rate: boss movement damping (it was counted per frame) and the weapon fire rate (it was rounded to the frame) were fixed, so a hen shoots and bosses move the same at 30, 60 and 120 FPS. Enemy movement was already time-based (checked at all three rates).

## New in 3.7 - final balance pass

An automatic bot played thousands of runs (every boss, every biome, every hen, fresh / mid / fully upgraded accounts) and the numbers were tuned from that.

- Enemy health and damage now keep growing after wave 12 (they used to stop at wave 25), the screen holds more enemies later in a run, and late enemies move a little faster. A fresh account with no upgrades now wins roughly one run in five at best; a fully upgraded account wins most of the time.
- All 11 bosses were calibrated one by one so a fight lasts about 25-100 seconds (final boss about 90) instead of a few seconds. Boss health grows with the wave, boss damage grows too, and bosses no longer feel like a wall or a pushover.
- Ember-Comb: the fire totems are the only way to break the shield, and the calibration fixed a case where the fight could drag on for minutes.
- Economy: wave coins are now 3 + 1.5 per wave (smoother), upgrades cost 2.2x as much (about 24k coins for everything instead of 11k), so a full set takes dozens of runs while the first upgrade still costs only one short run.
- Brickhouse hen: 150 health and 10% less damage taken (was 170 and 15%), because it was clearly stronger than every other hen. Texts updated in all four languages.
- Built-in balance bot and tuning knobs stay in the debug API (window.__cl.dbg.botRun / setBal / setBossK) for future tuning; they do nothing for normal play.

## New in 3.6 - Ukrainian, Spanish, a theme for every biome

- Four languages now: English, Russian, Ukrainian and Spanish. The game picks one from the phone language the first time (uk -> Ukrainian, es -> Spanish, ru/be/kk -> Russian, everything else English). Change it any time in Settings -> Language (2x2 grid of native names).
- Every screen is translated, including the daily gift, stats, achievements, boss tips and mod menu. Plural forms are handled (monedas / монети / монет).
- The built-in font now has the Spanish letters and the Ukrainian letters (i, yi, ye, g with upturned tail, accents, inverted marks).
- Music: each biome has its own gameplay theme - Sunny Meadow (bright major), Autumn Farm (the classic minor groove), Murky Swamp (slow, bubbly, phrygian), Frozen Field (icy bells), Ashen Volcano (heavy half-time), Deep Space (open fifths with echoes).
- Boss fights have a boss theme per biome (same pulse, different key, lead sound and extras), and King Cluck the Last has his own faster final theme with a sustained choir.

## New in 3.5 - run autosave

- If the app is closed (or killed) in the middle of a run, the menu shows **CONTINUE** (with wave and level) the next time you open the game. The game saves a checkpoint at the start of every wave, every few seconds while playing, when the app goes to the background (Android `onPause` calls `window.__save()`), and on the perk screen (the same three cards come back).
- Continuing restores the hen, power-ups, weapons and ammo, health, level, score, run coins, run modes, daily-challenge flag and active boosts. The current wave starts over (enemies are not saved).
- **NEW RUN** next to CONTINUE needs a second tap to abandon the saved run (same guard on Daily Challenge and Boss Rush starts). Dying, quitting from the pause menu, finishing the game, importing a save code or resetting progress clears the checkpoint. Boss Rush and admin/cheat runs are never saved. A checkpoint older than 14 days or from another day's Daily Challenge is dropped. Damaged data is rejected / clamped.

## New in 3.4 - daily gift streak, stats, 20 new achievements

- **Daily gift** (just open the game): a gift screen pops up on the first launch of each day. A 7-day cycle - coins, then boosts (XP +25%, COIN +30%, +30 health, and on day 7 a jackpot with SECOND WIND = one revive). Each new week the coins grow by 20% (up to +100%). Boosts are used automatically in your next normal run (not Daily Challenge / Boss Rush); quitting before wave 2 with no kills gives them back.
- **Streak rules**: log in on consecutive days to keep the streak. One missed day is forgiven by the **streak saver** (a new one is earned by finishing day 7). Bonus coins at 14 / 30 / 60 / 100 days. The streak travels with the save code. The menu has a flame chip with the current day (red dot = gift ready).
- **GOALS now has 3 tabs**: GOALS (daily challenge + achievements), STATS (runs, play time, kills, bosses, records, coins earned/spent, collection, per-hen runs and wins, all 11 bosses) and STREAK (the gift calendar and active boosts).
- **20 new achievements** (39 in total): Frequent Flyer, Coop Regular, Chicken Apocalypse, Trophy Wall, No One Left Standing, Boss Buster, Hot Feet, One Small Step, Overpowered, Killing Spree, Rampage, Flawless, Golden Goose, Fully Loaded, Special Move, Showoff, Time Flies, Weekly Regular, Devoted, Rush Warm-up.
- Test tip: open `index.html#nogift` to skip the daily gift popup.

## New in 3.3 - save code, gamepad, colour-blind mode

- **Save code** (Settings -> SAVE CODE): shows your whole progress (coins, upgrades, hens, bosses beaten, achievements, records, wardrobe, Boss Rush records) as one short text code (`CLK2...`, ~300-400 characters, compressed, with a checksum). **COPY CODE** and send it to yourself; on the other phone open the same screen, **PASTE** (or long-press the box and choose Paste) and tap **IMPORT CODE** twice (the second tap confirms replacing that phone's progress). Damaged or edited codes are rejected. No cloud involved. Never transferred: device settings and the admin/cheat state (the Kamehameha stays admin-code only).
- **Gamepad** (any controller the Android WebView / browser exposes through the Gamepad API; Settings -> GAMEPAD toggles it): left stick or D-pad moves, right stick aims (auto-fires when pushed), RT/RB fires, **A or Y** = hen ability, **X or LB** = swap weapon, **Start** = pause. Menus: D-pad / left stick moves a glowing focus ring, **A** presses, **B** goes back, right stick scrolls lists, left/right changes a focused slider. Perk screen: D-pad picks a card, A confirms. Touch sticks hide while a pad is in use; rumble on hits. Disconnecting a pad pauses the game.
- **Colour-blind mode** (Settings -> COLOR-BLIND MODE): danger never relies on red/green any more - enemy bullets become orange diamonds with a white outline, ground telegraphs, zones, beams, rings and lava rims get white outlines and a warning triangle, every enemy has an orange ring under it and your hen a blue one, friendly shots/flames/rockets turn blue or white.

## New in 3.2 - Boss Rush

Main menu -> **RUSH**. Fight **all 11 bosses in a row** (in wave order: Mama ... King Cluck, Emperor Plume, Nova-Cluck) with your selected hen and shop upgrades; game modes are off.
- Between bosses: a short break - you heal a bit, pick **one reward** (boss-quality rarity odds) and a 3-second countdown starts the next fight.
- The **timer** counts only the time spent in boss fights (not perk screens or countdowns), so runs are easy to compare. HUD shows `BOSS 4/11` and the clock.
- **Record** = most bosses beaten, then the fastest total fight time. The Rush screen keeps your best, your best full-clear time and the top 5 rushes; the result screen shows a split time for every boss and who stopped you.
- Coins are small (25-65 per boss, +100 for a full clear), achievement **Boss Rusher** (+300). Cheated (admin) runs never set records.

## New in 3.1 - Volcano, Deep Space and 2 new bosses

- **Ashen Volcano (waves 41-50)**: glowing **lava pools** burn you (about 8 damage every 0.45 s, you are thrown out of the pool); every 9-13 s the ground **erupts** (ERUPTION! - telegraphed circles that leave burning ground). Embers rise from the floor. Waves 45 and 50 (the final boss King Cluck) are fought here.
- **Deep Space (waves 51-60)**: **low gravity** - you accelerate and stop slowly (you drift), enemies drift too, and hits knock you much farther. Every 9-13 s a **meteor shower** (INCOMING METEORS!) falls on telegraphed spots. Stars drift in parallax as you move.
- New bosses **Magma Queen Pyra-Cluck** (wave 45) and **Commander Nova-Cluck** (wave 60); Emperor Plume moves to wave 55. The boss order is now mama, autumn, turkey, swamp, prof, snow, ember, volcano, FINAL, void, space (then repeats stronger). The admin mod menu "BOSS FIGHT" selector lists all 11.
- Biomes now cycle in 6 steps of 10 waves; Russian translation for every new string.

## New in 3.0 - hen abilities and 3 new hens

Every hen now has an **active ability** with a cooldown (button under the weapon button; keyboard: **F** or **Shift**):

| Hen | Ability | What it does |
|-----|---------|--------------|
| Cluck | Battle Cry (20 s) | 5 s of +60% fire rate and +25% speed |
| Brickhouse | Shield Charge (20 s) | dash through enemies (75 damage), then a shield |
| Hawkeye | Bullet Time (20 s) | enemies and their bullets at 30% speed for 4 s, every shot crits |
| Boomer | Big Bang (20 s) | huge explosion, erases enemy bullets |
| Ninja (new, 800 coins or beat Duchess Frostbeak) | Shadow Step (20 s) | blink through enemies (85 damage), next 3 shots crit. Fast, 10% dodge, Rapid Peck start |
| Mage (new, 1000 coins or beat Ember-Comb) | Meteor Shower (20 s) | fireballs fall on the nearest enemies. Every bullet burns, slows and chain-zaps |
| Robo-Hen (new, 1200 coins or beat King Cluck) | Sentry Turret (20 s) | turret shoots for 10 s. Starts with a drone chick and Laser Beak |

The first ability is ready a few seconds after the run starts. With the mod-menu "infinite ammo" the cooldown is 0.6 s.

## New in 2.3 - Russian localization

- **Settings -> LANGUAGE**: switch between **ENGLISH** and **РУССКИЙ** at any time; the choice is saved.
  On the first launch the language follows the phone / browser language (Russian, Ukrainian, Belarusian or
  Kazakh -> Russian, everything else -> English).
- Everything is translated: menus, shop, hens, goals, achievements, settings, mod menu, run modes, wardrobe,
  records, perks, weapons, bosses and their tips, biome warnings, floating texts, the ending screen.
- A small Cyrillic font (Carlito Bold subset, OFL licence, ~25 KB) is embedded in the page, so Russian text looks
  the same offline in the APK. The game logo stays "CLUCK & LOAD" in both languages.
- Technical note: the translation layer is `tr()` in the script (dictionary `I18N` + number patterns `I18N_PAT`);
  it hooks `fillText / strokeText / measureText`, so new strings only need an entry in `I18N`.
  Debug helper: `window.__cl.dbg.miss()` lists strings that were drawn without a translation.

## New in 2.2 - biome mechanics, bosses, ending

- **Autumn Farm (11-20)**: a wind gust (WIND GUST! warning) pushes you sideways for 3 seconds every 9-14 seconds; falling leaves.
- **Murky Swamp (21-30)**: goo puddles slow you down (x0.55); fog rolls in and out (FOG ROLLS IN warning) and limits your view.
- **Frozen Field (31-40)**: the ice makes you slide (low friction); snow falls.
- 4 new bosses (Harvest Scarecrow, Bog Hen Belladonna, Duchess Frostbeak and the final King Cluck) - see the table above; a boss now arrives every 5 waves.
- Ending screen + endless mode, new achievements `King of the Coop` (+400) and `No End in Sight` (+200).


## Power-ups and XP

After every wave you pick 1 of 3 power-up cards. There are 51, in three
rarities (common, rare, epic), and most stack. Examples: Double Damage, Swift
Talons (faster movement), Mirror Feathers (bullet reflection), Double XP,
Explosive Eggs, Chain Lightning, Orbiting Eggs, Second Wind, plus gun upgrades
and two unlockable weapons (Egg-zooka and Laser Beak). Kills earn XP; each
level gives a small automatic damage and health boost.

## New in 1.7

- **Android Back button**: during a run it pauses / resumes, in the shop, hens, goals,
  settings and game-over screens it goes back to the menu, and only on the main menu
  does it close the app.
- **New enemies**: Runner (fast, zig-zags, from wave 3), Medic (heals nearby chickens,
  keeps its distance, from wave 6) and Kamikaze (lights a fuse next to you and explodes,
  from wave 8). Shoot them early - a Kamikaze killed before the fuse burns out does not explode.
- **Arena biomes**: Sunny Meadow (waves 1-10), Autumn Farm (11-20), Murky Swamp (21-30),
  Frozen Field (31-40), then the cycle repeats. The ground, hedge, minimap and vignette change after every boss.

## New in 2.1 - new weapons

Three new guns, unlocked by power-up cards during a run (then they also drop as crates; on a PC press 6 / 7 / 8, Q or E cycles):

| Weapon | Power-up | What it does |
|--------|----------|--------------|
| Flamer (flamethrower) | Flamethrower (epic) | Short-range fire stream, pierces everything in the cone and sets enemies on fire. 150 fuel |
| Crossbow | Crossbow (rare) | Slow heavy bolt that pierces 4 enemies. 14 bolts |
| Grenades (launcher) | Grenade Launcher (epic) | Lobbed grenade: slows down, bounces off crates and the fence, blows up on contact with an enemy or after 1 second with a big blast. 10 grenades |

In-run weapon upgrades (cards that only appear once you own the gun): Napalm Tank, Barbed Bolts, Heavy Grenades,
and new Mk II cards for the Egg-zooka and Laser Beak. "Peashooter Only" mode blocks all of them.

## New in 2.0

- **Mod menu extras** (admin only, code `133700`): the **Kamehameha button is now on screen from the first second of every run**
  as soon as the mod menu is unlocked (it can still be switched off in the mod menu, and it never appears for normal players).
  New: **START WAVE** slider (1-100), **SLOW MOTION** slider (0.2x-1x), **BOSS FIGHT** picker (start a run straight at any boss) and
  **ADD COINS** (+1,000 / +10,000 / +100,000). Runs that use cheats are not written to the Records table.
- **Modes** (menu -> MODES): Glass Cannon, Horde, Turbo, Heavy Hitters, Iron Chicken (no healing), Peashooter Only.
  Every mode you switch on adds a bonus to the coins you earn at the end of the run.
- **Records** (menu -> RECORDS): your 10 best runs (score, wave, hen, number of modes) plus lifetime stats.
- **Wardrobe** (menu -> WARDROBE): hats, body colours and trails bought with coins.
- **Drone Chick** (Shop): a little helper that circles you and shoots; up to 3 drones.

## Admin code and mod menu (1.8)

Settings -> scroll to the bottom -> **ADMIN CODE**. Enter the secret code `133700` on the
on-screen keypad (or on a keyboard). The button then turns into **MOD MENU** with:
infinite coins (your real balance comes back when you switch it off), god mode,
infinite damage (one-hit kills, bosses too), infinite ammo with all weapons, a speedhack
slider (1x-5x), unlock all hens, max all shop upgrades, and **LOCK MOD MENU** to switch
everything off again. Added in 1.9: **Ultra Instinct** (silver aura, you automatically dodge every bullet
and chicken and nothing can hit you) and **Kamehameha** (a button on the right side of the screen, or the K key:
charge a giant beam that wipes the whole map, kills the boss and ends the run with a VICTORY screen;
all bosses count as beaten, so the boss hens unlock). To change the code edit `ADMIN_CODE` in `app/src/main/assets/index.html`.

## Customising

Edit `app/src/main/assets/index.html` (plain JavaScript and canvas) to change
weapons, enemies, waves and colours. App name and orientation live in
`app/src/main/AndroidManifest.xml`; the package id is in `app/build.gradle`.
