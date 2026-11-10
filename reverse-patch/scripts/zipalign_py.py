#!/usr/bin/env python3
"""Align stored ZIP members of an unsigned APK (4-byte data, 4096-byte native libs)."""
import zipfile,copy,struct,sys
from pathlib import Path
src,dst=map(Path,sys.argv[1:3])
with zipfile.ZipFile(src) as zin,zipfile.ZipFile(dst,'w',allowZip64=True) as zout:
 for item in zin.infolist():
  new=copy.copy(item)
  if item.compress_type==zipfile.ZIP_STORED:
   align=4096 if item.filename.endswith('.so') else 4
   offset=zout.fp.tell()+30+len(item.filename.encode('utf-8'))+len(item.extra)
   n=(-offset-4)%align
   new.extra=item.extra+struct.pack('<HH',0xd935,n)+bytes(n)
  zout.writestr(new,zin.read(item.filename),compress_type=item.compress_type)
with zipfile.ZipFile(dst) as z:
 assert z.testzip() is None
 for item in z.infolist():
  if item.compress_type!=zipfile.ZIP_STORED:continue
  align=4096 if item.filename.endswith('.so') else 4
  with open(dst,'rb') as f:
   f.seek(item.header_offset)
   header=f.read(30)
  _,_,_,_,_,_,_,_,_,namelen,extralen=struct.unpack('<IHHHHHIIIHH',header)
  offset=item.header_offset+30+namelen+extralen
  assert offset%align==0,(item.filename,offset,align)
print('APK ZIP aligned successfully')
