#!/usr/bin/env python3
"""Check native libraries before publishing a patched original APK.

The original Math Tricks Workout 2.8.5 sets extractNativeLibs=false.
Android must load its .so directly from a stored, page-aligned ZIP entry.
An earlier patched build had an offset that was not 4K aligned and failed
installation with INSTALL_FAILED_INVALID_APK / native libraries res=-2.
"""
import struct
import sys
import zipfile
from pathlib import Path

apk = Path(sys.argv[1])
assert apk.is_file(), apk
with zipfile.ZipFile(apk) as archive, apk.open("rb") as stream:
    assert archive.testzip() is None, "ZIP CRC failure"
    assert "classes5.dex" in archive.namelist(), "Missing base-twelve helper dex"
    native = [i for i in archive.infolist() if i.filename.startswith("lib/") and i.filename.endswith(".so")]
    assert native, "Original app's native libraries missing"
    for entry in native:
        stream.seek(entry.header_offset)
        header = struct.unpack("<IHHHHHIIIHH", stream.read(30))
        assert header[0] == 0x04034B50, "Invalid ZIP local header"
        offset = entry.header_offset + 30 + header[-2] + header[-1]
        assert entry.compress_type == zipfile.ZIP_STORED, f"Compressed native library: {entry.filename}"
        assert offset % 16384 == 0, f"Native library not aligned to 16 KiB: {entry.filename} @ {offset}"
        print(f"Native library OK: {entry.filename} @ byte {offset}")
    arsc = archive.getinfo("resources.arsc")
    assert arsc.compress_type == zipfile.ZIP_STORED, "Compressed resources.arsc"
    print(f"ZIP CRCs OK; {len(archive.infolist())} entries; {len(native)} correctly aligned native libraries")
