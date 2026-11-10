#!/usr/bin/env python3
"""Patch an Apktool-decoded copy of the original Math Tricks Workout 2.8.5 APK."""
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parent/'decoded'
SMALI=ROOT/'smali_classes3'
HELP='Lcom/sankhyantra/mathstricks/duo/Dozenal;'

def method_change(path,name,mutator):
    t=path.read_text()
    pat=re.compile(r'(?ms)^\.method [^\n]*? '+re.escape(name)+r'\([^\n]*\n.*?^\.end method')
    matches=list(pat.finditer(t))
    assert len(matches)==1,(path,name,len(matches))
    old=matches[0].group()
    new=mutator(old)
    assert new!=old,name
    path.write_text(t[:matches[0].start()]+new+t[matches[0].end():])

engine=SMALI/'P6/b.smali'
method_change(engine,'w',lambda _:f'''.method public w()Ljava/lang/String;
    .locals 1
    iget-object v0, p0, LP6/b;->a:Ljava/lang/String;
    invoke-static {{v0}}, {HELP}->expression(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method''')
method_change(engine,'z',lambda _:f'''.method public z()Ljava/lang/String;
    .locals 2
    iget-wide v0, p0, LP6/b;->b:D
    invoke-static {{v0, v1}}, {HELP}->format(D)Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method''')

frag=SMALI/'l5/x.smali'
method_change(frag,'P3',lambda _:f'''.method private P3(Ljava/lang/String;)Z
    .locals 2
    iget-object v0, p0, Ll5/x;->t1:LP6/b;
    if-nez v0, :found
    const/4 v0, 0x0
    return v0
    :found
    invoke-virtual {{v0}}, LP6/b;->A()D
    move-result-wide v0
    invoke-static {{v0, v1, p1}}, {HELP}->matches(DLjava/lang/String;)Z
    move-result v0
    return v0
.end method''')

def split(t):
    for num in ('v1','v3'):
        old=f'invoke-virtual {{v2, {num}}}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;'
        assert t.count(old)==1
        new=(f'invoke-static {{{num}}}, {HELP}->integer(I)Ljava/lang/String;\n    move-result-object {num}\n'
             +f'    invoke-virtual {{v2, {num}}}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;')
        t=t.replace(old,new)
    return t
method_change(frag,'c4',split)

def keyfilter(t):
    assert t.count('const-string v4, "0123456789-"')==1
    return t.replace('const-string v4, "0123456789-"','const-string v4, "0123456789-↊↋AaBb"')
method_change(frag,'N3',keyfilter)

def bind(t):
    old='    return-void\n.end method'
    assert t.endswith(old)
    return t.replace(old,f'''    iget-object v0, p0, Ll5/x;->c1:Landroid/widget/LinearLayout;
    invoke-static {{p0, v0}}, {HELP}->attach(Ll5/x;Landroid/view/View;)V
    return-void
.end method''')
method_change(frag,'b4',bind)

for name in ('practise_num_pad_layout_phone_v2.xml','practise_num_pad_layout_calc_v2.xml',
             'practise_num_pad_layout_phone.xml','practise_num_pad_layout_calc.xml'):
    path=ROOT/'res/layout'/name
    xml=path.read_text()
    assert 'duo_ten' not in xml and '<LinearLayout' in xml
    style='@style/MTWNumKey' if name.endswith('_v2.xml') else '@style/KeyPadBtn'
    row=f'''    <LinearLayout android:orientation="horizontal" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="@dimen/mtw_numpad_gap">
        <TextView android:id="@+id/duo_ten" android:text="↊" android:contentDescription="Duodecimal ten" android:layout_marginEnd="@dimen/mtw_numpad_gap" style="{style}" />
        <TextView android:id="@+id/duo_eleven" android:text="↋" android:contentDescription="Duodecimal eleven" style="{style}" />
    </LinearLayout>
'''
    end='</LinearLayout>'
    assert xml.rstrip().endswith(end)
    pos=xml.rfind(end)
    path.write_text(xml[:pos]+row+xml[pos:])
print('Patched original workout generator, input checker and four numeric keypads')
