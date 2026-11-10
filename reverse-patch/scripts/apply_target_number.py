#!/usr/bin/env python3
"""Convert original Target Number: targets, numerical tiles and calculation history."""
from pathlib import Path
import re
p=Path(__file__).resolve().parent/'decoded/smali_classes3/com/sankhyantra/mathstricks/game/activity/targetnumber/TargetNumberActivity.smali'
s=p.read_text()
H='Lcom/sankhyantra/mathstricks/duo/Dozenal;'
def edit(n,fn):
 global s
 m=re.search(r'(?ms)^\.method [^\n]*\s'+n+r'\([^\n]*\n.*?^\.end method',s)
 assert m,n
 a=m.group();b=fn(a)
 assert a!=b,n
 s=s[:m.start()]+b+s[m.end():]
for n in ('o3','T3','Y3'):
 def integer(t):
  k='Ljava/lang/String;->valueOf(I)Ljava/lang/String;'
  assert t.count(k)==1,n
  return t.replace(k,H+'->integer(I)Ljava/lang/String;')
 edit(n,integer)
def history(t):
 pat=re.compile(r'(?m)^(\s*invoke-virtual \{(v\d+)\}, Ljava/lang/StringBuilder;->toString\(\)Ljava/lang/String;\n\n\s*move-result-object (v\d+))$')
 assert len(pat.findall(t))==2
 return pat.sub(lambda m:m.group(1)+f'\n\n    invoke-static {{{m.group(3)}}}, {H}->expression(Ljava/lang/String;)Ljava/lang/String;\n\n    move-result-object {m.group(3)}',t)
edit('Y3',history)
p.write_text(s)
print('Patched Target Number')
