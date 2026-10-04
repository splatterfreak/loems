"""Regression checks for the completed battle-state mappings and QA exports."""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res/drawable-nodpi'
SOURCE = ROOT / 'app/src/main/java/de/loems/app/ui/LoemsApp.kt'
STATES = ('attack','hit','double_attack','double_hit','victory','defeat')

def main():
    code=SOURCE.read_text(encoding='utf-8')
    def block(name):
        return re.search(r'private val '+name+r' = .*?(?=\n    private val |\n})',code,re.S).group()
    hashes=[]
    for name,prefix in [('BAD','loem_bad'),('GLOOM_WIZARD_MALE','loem_gloom_wizard_poop_male'),
                        ('GLOOM_WIZARD_FEMALE','loem_gloom_wizard_poop_female')]:
        section=block(name)
        assert 'staticAsset(' not in section and 'advancedStaticBattleSet(' not in section, name
        for state in STATES:
            resource=f'{prefix}_battle_{state}_sheet'
            assert f'R.drawable.{resource}' in section,(name,state,'game mapping')
            assert code.count(f'R.drawable.{resource}') >= 2,(name,state,'debug mapping')
            data=(RES/f'{resource}.webp').read_bytes()
            assert data[:4]==b'RIFF' and b'VP8L' in data[:64],resource
            hashes.append(hashlib.sha256(data).hexdigest())
    assert len(hashes)==len(set(hashes)), 'Duplicated state bitmaps'
    assert 'loem_bad_battle_' not in block('BABY'), 'Wurst assets assigned to baby'
    assert 'female_battle_defeat' not in block('STORMKAISER_MALE')
    assert 'loem_stormkaiser_female_battle_defeat_sheet' in block('STORMKAISER_FEMALE')
    assert 'playOnceAndHold = phase == BattleAnimationPhase.RESULT_HOLD' in code
    count=0
    for folder in ('bad-battle','gloom-battle','projectiles'):
        records=json.loads((ROOT/f'art/qa/{folder}/results.json').read_text())
        for record in records:
            assert record['qa_pass'] and not record['crop_failures'],record
            assert record.get('lossless_exact',True),record
            assert (ROOT/f'art/qa/{folder}'/record['file']).read_bytes()==(RES/record['file']).read_bytes(),record
            count+=1
    assert count==34,count
    print(f'PASS: 18 dedicated new states, gender outcome mappings, 34 QA/production pairs, lossless exports and no duplicated state files.')

if __name__=='__main__':
    main()
