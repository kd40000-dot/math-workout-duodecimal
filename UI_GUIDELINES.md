# Duodecimal Workout UI conventions

## Navigation (AnkiDroid-inspired)

All primary and exercise screens use one top app bar with a menu icon that
opens a left-side navigation drawer. Do not introduce bottom navigation tabs.

The drawer groups destinations by task:

- **Study** — Practice, Browse facts, Statistics
- **Learn** — Learning tools (glyph recognition, visual addition and visual multiplication)
- **Other** — Other workouts, Settings

The default launch destination is **Practice**, with one clear primary action:
**Start practice**. Keep study settings and numerical learning rules on the
Settings screen. Browse facts opens the table, with per-fact details on tap.

## Settings (Keybr-inspired)

- **Fluency target:** target successful response time and priority for slow facts
- **Mastery criteria:** minimum accuracy, number of correct answers, and streak
- **Unlocking:** percentage gate or all-facts gate; best-achieved vs current
  mastery check; initial facts and batch size
- **Fact order:** row-major table, simpler first, or stable mixed order
- **Sessions:** questions per adaptive session and mastered-fact review

Every control must read/write the same SharedPreferences values used by
AdaptiveFacts. Never present a setting with no effect. Keep sensible defaults
and describe the difference between current and ever-achieved mastery.

## Copy, mathematical terminology and information hierarchy

Use concise sentence-case controls: **Start practice**, **Browse facts**,
**Learning tools**, **Settings**, **Continue learning**. Avoid all-caps labels.

Digit math uses the consistent component names:
- Caps: **Bar**, **Breve**, **Dot** (digit mod 3)
- Bodies: **Loop**, **Stem**, **Vee**, **El** (digit mod 4)

Show full duodecimal glyphs in mathematical problems. Recognition drills
must use ordinary Arabic references for decimal values 0–11.

The Dracula palette is:
- Background #282A36
- Surface #44475A
- Foreground #F8F8F2
- Primary #BD93F9
- Informational #8BE9FD
- Success #50FA7B
- Error #FF5555

Primary actions should be visually dominant. Dense progress tables and
fine-grained statistics should be disclosed on demand, not interleaved with
the main workout call to action.

## Accessibility and data

- Controls need legible text, contrast and at least 48dp touch targets.
- Every icon, drawer item and custom glyph cell needs a content description.
- The drawer must dismiss on backdrop tap or Android Back.
- Never use color alone to communicate a fact's mastery or lock state.
- Retain package ID `com.duodecimal.workout` and the persistent signing key.
- Preserve the old `adaptive_v1_*` progress keys; user-specific mastery,
  response times and unlocks must survive every ordinary APK update.

The current implementation is native Android views. Apply modern navigation,
accessibility and information architecture without an unrelated UI-framework
migration.
