#!/usr/bin/env python3
"""Convert original Find Missing Number: question, four choices, and comparisons."""
from pathlib import Path
import re
p=Path(__file__).resolve().parent/'decoded/smali_classes3/com/sankhyantra/mathstricks/game/activity/missingnumber/FindMissingNumberActivity.smali'
s=p.read_text()
H='Lcom/sankhyantra/mathstricks/duo/Dozenal;'
def edit(n,fn):
 global s
 m=re.search(r'(?ms)^\.method [^\n]*\s'+n+r'\([^\n]*\n.*?^\.end method',s)
 assert m,n
 old=m.group();new=fn(old)
 assert new!=old,n
 s=s[:m.start()]+new+s[m.end():]
for n in ('b3','g3'):
 def parse(t):
  old='Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I'
  assert t.count(old)>=1,n
  return t.replace(old,H+'->parseInt(Ljava/lang/String;)I')
 edit(n,parse)
def options(t):
 old='invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;\n\n    move-result-object v1'
 assert t.count(old)==4
 return t.replace(old,old+f'\n\n    invoke-static {{v1}}, {H}->expression(Ljava/lang/String;)Ljava/lang/String;\n\n    move-result-object v1')
edit('w3',options)
def question(t):
 old='invoke-virtual {p1}, LU5/b;->b()Ljava/lang/String;\n\n    move-result-object p1'
 assert t.count(old)==1
 return t.replace(old,old+f'\n\n    invoke-static {{p1}}, {H}->expression(Ljava/lang/String;)Ljava/lang/String;\n\n    move-result-object p1')
edit('l3',question)
p.write_text(s)
print('Patched Find Missing Number')
