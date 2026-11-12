"""Tiny regression check for Android popup contrast on the Dracula theme.

The Android framework creates its own dropdown row views. Unless their text
color is overridden, the framework theme may use almost-black foregrounds
on the app's dark popup background.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
screens = {
    "VisualAdditionActivity.java": ("styleSpinner", "s.setPopupBackgroundDrawable"),
    "MainActivity.java": ("styledItem", "modes.setPopupBackgroundDrawable"),
    "CustomPracticeActivity.java": ("styleOption", "spinner.setPopupBackgroundDrawable"),
}
for filename, (styler, background) in screens.items():
    source = (ROOT / "src/main/java/com/duodecimal/workout" / filename).read_text()
    assert re.search(
        r"getDropDownView\s*\([^)]*\)\s*\{\s*return\s+"
        + styler
        + r"\s*\(\s*super\.getDropDownView\([^;]+\),\s*FG\s*\)\s*;",
        source
    ), f"{filename}: dropdowns must use foreground FG, not BG"
    assert background in source, f"{filename}: dropdown popup needs a dark background"
    assert "Spinner.MODE_DROPDOWN" in source, f"{filename}: require dropdown spinner mode"
print("All three spinner screens: bright text on explicit dark popup backgrounds.")
