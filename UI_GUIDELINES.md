# Duodecimal Workout UI conventions

## Navigation and information architecture

The main experience has three peer destinations:

- **Practice** (launcher): adaptive and manual fact practice. The main action is **Start practice**.
- **Learn**: glyph recognition, visual addition, visual multiplication.
- **More**: mixed and specialized arithmetic workouts.

Use a consistent top-level destination selector on these three screens. Child drills return to their parent destination. An action that starts an exercise must not look like a destination tab.

## Language

Use sentence case for control text, headings, and statuses. Prefer verbs for actions:
**Start practice**, **View progress table**, **Hide table**, **Continue learning**.

Avoid acronyms unless explained. Use **base twelve** in instructional text.
Use **glyph** for the displayed custom numeral and **digit** for its numeric value.

Name glyph components consistently:
- Caps: **Bar**, **Breve**, **Dot** (digit mod 3).
- Bodies: **Loop**, **Stem**, **Vee**, **El** (digit mod 4).

Arabic references in glyph recognition must be rendered as ordinary decimal numerals
(0 through 11), never with the custom glyph renderer.

## Visual hierarchy

Use Dracula colors consistently:
- Background: #282A36
- Surface: #44475A
- Foreground: #F8F8F2
- Primary action: #BD93F9 (with dark ink)
- Informational accent: #8BE9FD
- Success: #50FA7B
- Error: #FF5555

Only one primary, high-contrast call to action per screen.
Use secondary surfaces for navigation or disclosure controls.
Display progress plainly: **Unlocked**, **Mastered**, **First try**.
Leave detailed per-fact stats in an accessible drilldown rather than
placing long instructions or dense grids ahead of primary practice.

## Accessibility

- Interactive tap regions at least 48dp high; keypad options use 64dp or more.
- All selectable graphical cells should have meaningful content descriptions.
- Color is not the only indicator: also provide labels, statuses and descriptions.
- Do not rely on system glyph availability for the custom duodecimal numerals.
- Preserve text contrast on dark dropdown menus and selected states.
- Support standard Android Back/up behavior.

## Persistence and updates

Never change `com.duodecimal.workout` or reset SharedPreferences namespaces
just to refresh the UI. Keep the stable release signing certificate and increment
versionCode for every release. Adaptive mastery and response-time records are
local user data and must survive normal upgrades.

## Implementation scope

This project uses Java Android views and native layout primitives. Apply
Material design principles (hierarchy, discoverability, predictable navigation,
touch targets and accessibility) without requiring an unrelated Compose or
Material Components migration.
