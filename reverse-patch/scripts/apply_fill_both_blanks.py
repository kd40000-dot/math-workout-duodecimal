#!/usr/bin/env python3
"""Convert original Fill Both Blanks: number cards, equation, and integer comparisons."""
from pathlib import Path
import re
p=Path(__file__).resolve().parent/'decoded/smali_classes3/com/sankhyantra/mathstricks/game/activity/fillbothblanks/FillBothBlanksActivity.smali'
s=p.read_text()
H='Lcom/sankhyantra/mathstricks/duo/Dozenal;'
def edit(n,fn):
 global s
 m=re.search(r'(?ms)^\.method [^\n]*\s'+n+r'\([^\n]*\n.*?^\.end method',s)
 assert m,n
 a=m.group();b=fn(a)
 assert a!=b,n
 s=s[:m.start()]+b+s[m.end():]
for n in ('E2','e3'):
 def parse(t):
  k='Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I'
  assert t.count(k)==1,n
  return t.replace(k,H+'->parseInt(Ljava/lang/String;)I')
 edit(n,parse)
for n in ('g3','l3','S3','u3','j3'):
 def number(t):
  k='Ljava/lang/String;->valueOf(I)Ljava/lang/String;'
  assert t.count(k)>=1,n
  return t.replace(k,H+'->integer(I)Ljava/lang/String;')
 edit(n,number)
for n in ('j3','I3'):
 def expr(t):
  reg='v6' if n=='j3' else 'v0'
  old=f'invoke-virtual {{{reg}}}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;\n\n    move-result-object {reg}'
  assert t.count(old)==1,n
  return t.replace(old,old+f'\n\n    invoke-static {{{reg}}}, {H}->expression(Ljava/lang/String;)Ljava/lang/String;\n\n    move-result-object {reg}')
 edit(n,expr)
p.write_text(s)
print('Patched Fill Both Blanks')
