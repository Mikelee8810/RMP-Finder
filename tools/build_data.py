#!/usr/bin/env python3
import copy, hashlib, json, re
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'data/source/notion-rmp-snapshot-2026-09-09.json'
GEO=ROOT/'data/source/census-geocodes-2026-09-09.json'
OUT=ROOT/'data/restaurants.json'
REVIEW=ROOT/'data/source/geocode-review-2026-09-09.json'
MANIFEST=ROOT/'data/manifest.json'
DATE='2026-09-09'
DATASET_VERSION=2
OTDA='https://otda.ny.gov/programs/rmp/participating-restaurants/default.asp'
CENSUS='https://geocoding.geo.census.gov/geocoder/'

# Snapshot rows whose enrichment changed an official OTDA identity field.
OFFICIAL={
 192: {'zip':'11413'},
 193: {'name':"Burger King/Popeye's", 'line1':'16150 Cross Bay Boulevard'},
 194: {'name':'Burger King/Popeyes', 'line1':'161 Cross Bay Boulevard'},
 196: {'name':'Burger King','line1':'6259 Fresh Pond Road','city':'Queens','zip':'11365'},
 201: {'zip':'11432'},
 206: {'line1':'106-15 71st Street'},
 210: {'line1':'22-14 31st Avenue'},
 217: {'line1':'75-50 101st Street'},
 221: {'name':'Pablo Peppa Jerk Chicken','line1':'226-08 Merrick Boulevard'},
 234: {'line1':'89 Guyon Street'},
}
CURRENT={
 12: {'line1':'2366 Grand Concourse','city':'Bronx','zip':'10458'},
 50: {'line1':'1663 Linden Boulevard','city':'Brooklyn','zip':'11212'},
 53: {'line1':'522 Fulton Street','city':'Brooklyn','zip':'11201'},
 56: {'line1':'2322 86th Street','city':'Brooklyn','zip':'11214'},
 69: {'line1':'1364 Granville Payne Avenue','city':'Brooklyn','zip':'11239'},
 89: {'line1':'1275 Fulton Street','city':'Brooklyn','zip':'11216'},
 92: {'line1':'204-11 Hillside Avenue','city':'Hollis','zip':'11423'},
 97: {'line1':'3540 Nostrand Avenue','city':'Brooklyn','zip':'11229'},
  106:{'line1':'1983 86th Street','city':'Brooklyn','zip':'11214'},
  110:{'line1':'25 101st Avenue','city':'Brooklyn','zip':'11208'},
  111:{'line1':'1077A Rutland Road','city':'Brooklyn','zip':'11212'},
 143:{'line1':'229B Dyckman Street','city':'New York','zip':'10034'},
 151:{'line1':'1342 Saint Nicholas Avenue','city':'New York','zip':'10033'},
 192:{'line1':'109-10 Guy R Brewer Boulevard','city':'Jamaica','zip':'11433'},
 193:{'line1':'161-50 Cross Bay Boulevard','city':'Howard Beach','zip':'11414'},
 194:{'line1':'161-50 Cross Bay Boulevard','city':'Howard Beach','zip':'11414'},
 196:{'line1':'62-59 Fresh Pond Road','city':'Ridgewood','zip':'11385'},
 206:{'line1':'106-15 71st Avenue','city':'Forest Hills','zip':'11375'},
 210:{'line1':'22-50 31st Street','city':'Astoria','zip':'11105'},
 217:{'line1':'75-50 101st Avenue','city':'Ozone Park','zip':'11416'},
 221:{'line1':'226-08A Merrick Boulevard','city':'Laurelton','zip':'11413'},
 234:{'line1':'89 Guyon Avenue','city':'Staten Island','zip':'10306'},
}
CURRENT_NAMES={58:'Hajveri Restaurant',73:'Golden Bay Parkway Restaurant',135:'Anba Tonel',152:'El Valle Seafood Restaurant',183:"N&M's Pizza Bar",193:'Popeyes',194:'Burger King',196:'Popeyes',221:"Peppa's Jerk Chicken"}

# Reviewed conflict flags are explicit. Do not infer identity/phone conflicts from
# unrelated prose such as "rather than guessed" or "hours may differ".
PHONE_CONFLICTS={1,16,23,44,66,94,108,128,149,153,156,196,223}

# Current-business state is separate from OTDA eligibility. Explicit reviewed states
# take precedence over older words preserved in historical notes.
STATUS_OVERRIDES={
  26:'likely_open',
  27:'likely_open',
  41:'likely_open',
  130:'conflicting',
  135:'rebranded',
  140:'conflicting',
  160:'likely_open',
  161:'conflicting',
  192:'conflicting',
  230:'conflicting',
  237:'conflicting',
}
BUSINESS_REVIEW_DATE='2026-09-10'
BUSINESS_REVIEW_DATES={
  26:'2026-09-12',
  27:'2026-09-12',
  108:'2026-09-12',
  160:'2026-09-12',
  164:'2026-09-12',
}
BUSINESS_STATUS_SOURCES={
  26:[('Luna Café official site','https://lunacafeny.com')],
  27:[('1 Sabor Latino official site','https://www.1saborlatino.com/')],
  41:[
    ('Atomic Wings official location page','https://www.atomicwings.com/locations/brooklyn-ny'),
    ('Downtown Brooklyn directory','https://www.downtownbrooklyn.com/directory/'),
  ],
  130:[
    ('DoorDash current merchant listing','https://www.doordash.com/en/store/roseli-chinese-restaurant-brooklyn-27525225/'),
    ('current business directory listing','https://restaurantguru.com/Roseli-%E7%91%B0%E9%BA%97-Chinese-Restaurant-New-York'),
  ],
  135:[
    ('New York Department of State record mirror','https://www.bizprofile.net/ny/brooklyn/anba-tonel-lounge-and'),
    ('Brooklyn Community Board 14 event listing','https://cb14brooklyn.com/community-event/haitian-heritage-month-and-flag-day-celebration/'),
  ],
  160:[('Lady Chow Kitchen official site','https://www.ladychowkitchen.com/')],
  164:[('McDonald\'s official location page','https://www.mcdonalds.com/us/en-us/location/NY/New-York/1528-Broadway/39147.html')],
  108:[('Memphis Seoul official location page','https://getmemphisseoul.com/location')],
}

# Reviewed coordinate replacements / acceptances. These never rewrite the official address.
MANUAL_GEO={
 125: (-73.9198354,40.6097032,'Public Storage geocoder','5100 Kings Plaza, Brooklyn, NY 11234','Census incorrectly resolved the OTDA mall address to 5100 Kings Highway; exact Kings Plaza address fallback geocoder used.'),
 103: (-73.9434979,40.701362,'Public Storage geocoder','700 Broadway, Brooklyn, NY 11206','Census returned no match; exact-address fallback geocoder used.'),
 139: (-73.996310719159,40.743065743723,'U.S. Census Geocoder','197 7TH AVE, NEW YORK, NY, 10011','Census matched the exact street address after removing the stale source ZIP; ZIP conflict preserved.'),
 192: (-73.790004487684,40.694748357541,'U.S. Census Geocoder','109-10 GUY R BREWER BLVD, JAMAICA, NY, 11433','Exact street address; Census ZIP conflicts with OTDA ZIP 11413.'),
 193: (-73.840534746208,40.661352393524,'U.S. Census Geocoder','161-50 CROSS BAY BLVD, HOWARD BEACH, NY, 11414','OTDA omits the Queens hyphen; reviewed against the shared current property.'),
 194: (-73.840534746208,40.661352393524,'U.S. Census Geocoder','161-50 CROSS BAY BLVD, HOWARD BEACH, NY, 11414','OTDA address is malformed; reviewed against the shared current property.'),
 196: (-73.90049812637,40.712665301587,'U.S. Census Geocoder','62-59 FRESH POND RD, RIDGEWOOD, NY, 11385','OTDA line/ZIP conflict reviewed against the same Queens-numbered location.'),
 206: (-73.8451,40.719994,'Public Storage geocoder','106-15 71st Ave, Forest Hills, NY 11375','Census misparsed the hyphenated official street; exact current-business address used for location review.'),
 210: (-73.929663314984,40.767170036469,'U.S. Census Geocoder','22-14 31ST AVE, ASTORIA, NY, 11106','Exact OTDA street address; Census ZIP conflicts with OTDA ZIP 11105.'),
 217: (-73.861897460484,40.67968325124,'U.S. Census Geocoder','75-50 101ST AVE, OZONE PARK, NY, 11416','OTDA says Street; Census and current business resolve Avenue at the same number.'),
 234: (-74.127869917254,40.564847601149,'U.S. Census Geocoder','89 GUYON AVE, STATEN ISLAND, NY, 10306','OTDA key says Street; Census resolves Avenue at the same number.'),
  238: (-73.9209245,41.2897856,'Public Storage geocoder','911 South Street, Peekskill, NY 10566','Census returned no match; exact-address fallback geocoder used.'),
}

# Census matches that were manually reviewed because the returned street number/name
# is a normalization, range, unit suffix, or service-road form of the OTDA address.
REVIEWED_GEO={
  4: 'OTDA/source line includes East Bedford Park Boulevard; Census omits the directional word while retaining the same house number and ZIP.',
  9: 'OTDA/source line says East Tremont Street; Census returns East Tremont Avenue at the same house number and ZIP. Official wording retained.',
  12: 'OTDA/source line says Grand Concourse Road; Census normalizes the street name to Grand Concourse at the same house number and ZIP.',
  16: 'OTDA/source line contains the spelling Avneue; Census normalizes it to Avenue at the same house number and ZIP.',
  30: 'OTDA lists 815 Hutchinson Parkway; Census resolves the same property as 815 Hutchinson River Parkway Service Road. Reviewed against the live OTDA entry.',
  33: 'OTDA unit suffix A is omitted by Census; street number, street, borough, and ZIP otherwise match.',
  52: 'OTDA lists the 417-21 Fulton Street range; Census resolves 417 Fulton Street within that range.',
  54: 'OTDA/source line says South Conduit Avenue; Census returns South Conduit Boulevard at the same house number and ZIP. Official wording retained.',
  82: 'OTDA/source line spells Koscuiszko; Census returns Kosciuszko at the same house number and ZIP.',
  110: 'OTDA/source line says 25 101 Avenue; the current restaurant listing and Census both resolve 25 101st Avenue with the same phone and ZIP. Official wording retained separately.',
  128: 'OTDA/source line says Conduit Boulevard; Census adds South at the same house number and ZIP. Official wording retained.',
  143: 'OTDA lists 227-229 Dyckman Street; Census resolves 229 Dyckman Street within that range.',
  152: 'OTDA/source line says Sherman Street; Census returns Sherman Avenue at the same house number and ZIP. Official wording retained.',
  163: 'OTDA/source line spells Delancy; Census returns Delancey at the same house number and ZIP.',
  181: 'OTDA/source line spells Delancy; Census returns Delancey at the same house number and ZIP.',
  202: 'OTDA unit suffix D is omitted by Census; street number, street, city, and ZIP otherwise match.',
  221: 'Current-business address adds unit suffix A; base street number, street, city, and ZIP match the official OTDA address.',
}

DAYS=['monday','tuesday','wednesday','thursday','friday','saturday','sunday']
DAYIDX={k:i for i,k in enumerate(['mon','tue','wed','thu','fri','sat','sun'])}

def addr(line1,city,zipv):
    line2=None
    m=re.match(r'^(.*?),\s*(Ste\.?|Suite|Unit)\s*(.+)$', line1, re.I)
    if m: line1=m.group(1); line2=f'{m.group(2)} {m.group(3)}'
    d={'line1':line1,'city':city,'state':'NY','zip':zipv}
    if line2: d['line2']=line2
    return d

def dayset(spec):
    spec=spec.lower().replace('daily','mon-sun').replace('sunday','sun').replace('monday','mon').replace('tuesday','tue').replace('wednesday','wed').replace('thursday','thu').replace('friday','fri').replace('saturday','sat')
    out=set()
    for part in re.split(r'\s*(?:&|,)\s*',spec):
        bits=[b for b in part.strip().split('-') if b]
        if len(bits)==1 and bits[0][:3] in DAYIDX: out.add(DAYIDX[bits[0][:3]])
        elif len(bits)==2 and bits[0][:3] in DAYIDX and bits[1][:3] in DAYIDX:
            a,b=DAYIDX[bits[0][:3]],DAYIDX[bits[1][:3]]; i=a
            while True:
                out.add(i)
                if i==b: break
                i=(i+1)%7
        elif len(bits)>2 and all(x[:3] in DAYIDX for x in bits):
            out.update(DAYIDX[x[:3]] for x in bits)
        else: return None
    return out

def parse_time(t, other_mer=None, is_close=False):
    t=t.strip().lower().replace('.','')
    if t=='noon': return 12*60
    if t=='midnight': return 24*60 if is_close else 0
    m=re.fullmatch(r'(\d{1,2})(?::(\d{2}))?\s*(am|pm)?',t)
    if not m: return None
    h=int(m.group(1)); minute=int(m.group(2) or 0); mer=m.group(3) or other_mer
    if mer:
        h%=12
        if mer=='pm': h+=12
    elif h==24 and minute==0: return 1440
    if h>23 or minute>59: return None
    return h*60+minute

def parse_range(txt):
    txt=txt.strip().replace('–','-')
    m=re.fullmatch(r'(.+?)\s*-\s*(.+?)(?:\s+next day)?',txt,re.I)
    if not m:return None
    a,b=m.group(1),m.group(2)
    am=re.search(r'\b(am|pm)\b',a,re.I); bm=re.search(r'\b(am|pm)\b',b,re.I)
    amer=am.group(1).lower() if am else (bm.group(1).lower() if bm else None)
    bmer=bm.group(1).lower() if bm else None
    start=parse_time(a,amer,False); end=parse_time(b,bmer or amer,True)
    if start is None or end is None:return None
    # When the first time omitted meridiem, inherit from the second but choose the plausible same-day start.
    if not am and bm and start>=end and end!=1440:
        alt=start-12*60
        if alt>=0: start=alt
    return start,end

def hm(v):
    if v==1440:return '24:00'
    return f'{v//60:02d}:{v%60:02d}'

def parse_hours(text):
    if not text:return None
    low=text.lower()
    uncertainty=['conflicting','likely ','previously','approximately','about ','roughly','commonly','ordering schedule','current sources conflict','current business listing marks permanently closed','shared/co-located','burger king:','popeyes:', 'sit-down:', 'takeout:', ' plus ', 'with a short']
    if any(x in low for x in uncertainty): return None
    if low.strip() in ('open 24 hours daily','24 hours daily'):
        return {'timezone':'America/New_York',**{d:[{'open':'00:00','close':'24:00'}] for d in DAYS}}
    periods=[[] for _ in DAYS]; assigned=set()
    segments=[x.strip() for x in text.replace('—','-').split(';') if x.strip()]
    for seg in segments:
        if re.fullmatch(r'\d{1,2}(?::\d{2})?\s*(?:am|pm)\s*-\s*\d{1,2}(?::\d{2})?\s*(?:am|pm)',seg,re.I):
            ds=set(range(7)); val=seg
        else:
            m=re.match(r'^(.+?)\s+(closed(?:/not listed)?|not listed|open 24 hours|24 hours|.+)$',seg,re.I)
            if not m:return None
            ds=dayset(m.group(1)); val=m.group(2).strip()
            if ds is None:return None
        vl=val.lower()
        if 'not listed' in vl and 'closed' not in vl:return None
        if vl.startswith('closed'):
            assigned.update(ds); continue
        if vl in ('open 24 hours','24 hours'):
            for d in ds: periods[d].append({'open':'00:00','close':'24:00'})
            assigned.update(ds); continue
        rr=parse_range(val)
        if not rr:return None
        start,end=rr
        for d in ds:
            if end>start:
                periods[d].append({'open':hm(start),'close':hm(end)})
            else:
                periods[d].append({'open':hm(start),'close':'24:00'})
                periods[(d+1)%7].append({'open':'00:00','close':hm(end)})
            assigned.add(d)
    if assigned != set(range(7)): return None
    for p in periods: p.sort(key=lambda x:x['open'])
    return {'timezone':'America/New_York',**{DAYS[i]:periods[i] for i in range(7)}}

def flags(note,idx):
    n=(note or '').lower(); f=set()
    if any(x in n for x in ['address mismatch','address discrepancy','current business address','zip conflict','zip 112','otda/static source says','malformed','same address now use','resolve to 2322','two doors','same location; otda']): f.add('address_mismatch')
    if idx in CURRENT_NAMES: f.add('name_mismatch')
    if 'rebrand' in n or idx in (58,73,135,183): f.add('rebranded')
    if idx in PHONE_CONFLICTS: f.add('phone_conflict')
    if ('hours' in n and any(x in n for x in ['conflict','disagree','differ','unconfirmed'])): f.add('hours_conflict')
    if 'status' in n and 'conflict' in n or ('official site' in n and 'closed' in n): f.add('status_conflict')
    if idx in CURRENT: f.add('address_mismatch')
    return sorted(f)

def business_status(note,hs,idx):
    n=(note or '').lower()
    if idx in STATUS_OVERRIDES:return STATUS_OVERRIDES[idx]
    if any(x in n for x in ['conflicting current status','current-status conflict','status conflict','current sources conflict on status']): return 'conflicting'
    if 'reopening inspection' in n or 'reopened' in n:return 'likely_open'
    if 'temporarily closed' in n:return 'temporarily_closed'
    if 'permanently closed' in n:return 'closed'
    if idx==203 or ('show this location closed' in n):return 'closed'
    if idx==106:return 'moved'
    if idx in (58,73,135,183):return 'rebranded'
    return 'likely_open' if hs=='Cached' else 'unknown'

def official_for(i,r):
    o=OFFICIAL.get(i,{})
    return o.get('name',r['Restaurant']), addr(o.get('line1',r['Address']),o.get('city',r['City']),o.get('zip',r['ZIP']))

def current_for(i,r):
    if i in CURRENT:
        c=CURRENT[i]; return addr(c['line1'],c['city'],c['zip'])
    return None

def geocode_for(i,r,g,official):
    if i in MANUAL_GEO:
        lon,lat,provider,matched,reason=MANUAL_GEO[i]
        return {'latitude':lat,'longitude':lon,'addressRole':'official_rmp','provider':provider,'matchQuality':'manual','precision':'approximate' if 'Public Storage' in provider else 'interpolated','checkedAt':DATE,'matchedAddress':matched}, reason
    ms=((g.get('result') or {}).get('addressMatches') or [])
    if not ms: raise ValueError(f'No reviewed geocode for row {i} {r["RMP Key"]}')
    m=ms[0]; comp=m.get('addressComponents',{}); matched=m.get('matchedAddress')
    zipdiff=comp.get('zip') and comp.get('zip')!=official['zip']
    reviewed=REVIEWED_GEO.get(i)
    quality='matched' if (zipdiff or reviewed) else 'exact'
    reason=reviewed or 'Accepted Census address-range match.'
    if zipdiff:
        reason += f' Matched ZIP {comp.get("zip")} differs from OTDA ZIP {official["zip"]}; reviewed and OTDA value retained.'
    return {'latitude':m['coordinates']['y'],'longitude':m['coordinates']['x'],'addressRole':'official_rmp','provider':'U.S. Census Geocoder','matchQuality':quality,'precision':'interpolated','checkedAt':DATE,'matchedAddress':matched},reason

def main():
    src=json.load(open(SRC)); geo=json.load(open(GEO))
    assert len(src)==len(geo)==241
    records=[]; reviews=[]; parsed=0
    for i,(r,g) in enumerate(zip(src,geo)):
        business_reviewed_at=BUSINESS_REVIEW_DATES.get(i,BUSINESS_REVIEW_DATE)
        official_name,official_addr=official_for(i,r)
        current_addr=current_for(i,r); current_name=CURRENT_NAMES.get(i)
        fl=flags(r.get('Notes'),i); bs=business_status(r.get('Notes'),r['Hours Status'],i)
        hours=parse_hours(r.get('Hours'))
        if hours: parsed+=1
        if r['Hours Status']=='Unknown': hstatus='unknown'
        elif r['Hours Status']=='Needs refresh': hstatus='conflicting' if ('hours_conflict' in fl or 'status_conflict' in fl) else 'stale'
        else: hstatus='usable' if hours else ('conflicting' if 'hours_conflict' in fl else 'partial')
        if bs in ('closed','temporarily_closed','moved') and hstatus=='usable': hstatus='stale'
        coord,review_reason=geocode_for(i,r,g,official_addr)
        rec={
          'rmpKey':r['RMP Key'],'officialName':official_name,'currentName':current_name,
          'aliases':([r['Restaurant']] if r['Restaurant']!=official_name and r['Restaurant']!=current_name else []),
          'officialAddress':official_addr,'currentAddress':current_addr,'borough':r['Area'],'zip':official_addr['zip'],
          'coordinates':coord,'phone':r.get('Phone'),'website':r.get('Website'),'menuUrl':None,'imageUrl':None,'imageAttribution':None,
          'businessStatus':bs,'hoursStatus':hstatus,'hours':hours,'rmpVerifiedAt':r['RMP Verified'],
          'businessCheckedAt':business_reviewed_at if i in BUSINESS_STATUS_SOURCES else r.get('Hours Verified'),'conflictFlags':fl,
          'sources':[
            {'kind':'NYS OTDA Restaurant Meals Program','role':'rmp_eligibility','url':r.get('OTDA Source') or OTDA,'checkedAt':r['RMP Verified']},
            {'kind':coord['provider'],'role':'coordinates','url':CENSUS if 'Census' in coord['provider'] else None,'checkedAt':DATE},
          ]
        }
        for kind,url in BUSINESS_STATUS_SOURCES.get(i,[]):
            rec['sources'].append({'kind':kind,'role':'business_status','url':url,'checkedAt':business_reviewed_at})
        if r.get('Hours Verified'):
            rec['sources'].append({'kind':'reviewed business enrichment snapshot','role':'hours','url':r.get('Website'),'checkedAt':r['Hours Verified']})
        if current_addr:
            rec['sources'].append({'kind':'reviewed business enrichment snapshot','role':'address','url':r.get('Website'),'checkedAt':r.get('Hours Verified') or DATE})
        if r.get('Phone'):
            rec['sources'].append({'kind':'reviewed business enrichment snapshot','role':'phone','url':r.get('Website'),'checkedAt':r.get('Hours Verified') or DATE})
        if r.get('Website'):
            rec['sources'].append({'kind':'reviewed business enrichment snapshot','role':'website','url':r['Website'],'checkedAt':r.get('Hours Verified') or DATE})
        records.append(rec)
        reviews.append({'index':i,'rmpKey':r['RMP Key'],'officialAddress':official_addr,'inputAddress':g.get('inputAddress'),'matchedAddress':coord.get('matchedAddress'),'latitude':coord['latitude'],'longitude':coord['longitude'],'provider':coord['provider'],'matchQuality':coord['matchQuality'],'precision':coord['precision'],'review':review_reason})
    # Deterministic restaurant order and formatting.
    OUT.write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
    REVIEW.write_text(json.dumps(reviews,ensure_ascii=False,indent=2)+'\n')
    sha=hashlib.sha256(OUT.read_bytes()).hexdigest()
    manifest={'datasetVersion':DATASET_VERSION,'schemaVersion':1,'generatedAt':datetime.now(ZoneInfo('America/New_York')).isoformat(timespec='seconds'),'recordCount':len(records),'sha256':sha,'datasetUrl':'https://raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/data/restaurants.json','minimumAppVersion':1,'sourceRevision':None,'programPolicy':{'rmpDiscountPercent':10,'checkedAt':DATE,'sourceUrl':'https://otda.ny.gov/policy/gis/2025/25DC012.pdf'}}
    MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(f'generated records={len(records)} parsed_hours={parsed} explicit_nonparsed_hours={sum(1 for r in src if r.get("Hours"))-parsed} sha256={sha}')
if __name__=='__main__': main()
