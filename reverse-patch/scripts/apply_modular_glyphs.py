#!/usr/bin/env python3
"""Hook Draft A numeral presentation into the *original* Math Tricks Workout APK.

Run this on an Apktool-decoded copy of the already patched original 2.8.5 APK.
The workflow build-original-glyph-dex.yml independently compiles classes6.dex.

Usage:
    cd reverse-patch/scripts
    python3 apply_modular_glyphs.py
    apktool b decoded -o unsigned-with-modular-numerals.apk
    # Append classes6.dex to unsigned ZIP, then sign with persistent
    # patch signing key and --lib-page-alignment 16384.

Does not change the original Android package, exercise values or save format.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parent / "decoded"
path = root / "smali_classes3/com/sankhyantra/mathstricks/MTWApplication.smali"
source = path.read_text()
method = re.search(r"(?ms)^\.method public onCreate\(\)V\n.*?^\.end method", source)
assert method, "Original MTWApplication.onCreate() not found"

body = method.group()
marker = "    invoke-super {p0}, Landroid/app/Application;->onCreate()V"
assert body.count(marker) == 1, "Unexpected original Application structure"
hook = "    invoke-static {p0}, Lcom/sankhyantra/mathstricks/duo/OriginalGlyphInstaller;->install(Landroid/app/Application;)V"
assert hook not in body, "Glyph hook already applied"

body = body.replace(marker, marker + "\n\n" + hook, 1)
path.write_text(source[:method.start()] + body + source[method.end():])
print("Patched MTWApplication to install Draft A numeral renderer on activity start")
