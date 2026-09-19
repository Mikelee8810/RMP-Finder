#!/usr/bin/env python3
"""Ask Google Places once per restaurant and cache the answer in Notion.

Why this exists
---------------
Two problems shared one cause. Hours were written into Notion as free prose
("Mon-Thu & Sun noon-10:00 PM"), which the reviewed Hours parser cannot read,
so 59 of 241 restaurants showed no hours in the app at all. Separately,
RatingsStore asked Google for a rating from *every phone*, on install and
again weekly, which is the only part of this system that costs money.

Both go away if the lookup happens once, centrally. This script does that
single pass: it asks Google about each restaurant and writes the answer back
into Notion, which is the editing workspace the dataset is built from. The
app then reads cached values and never calls Google itself.

Cost shape
----------
Google bills a Places request at the highest tier any requested field belongs
to, so the field mask here is the whole bill. Asking for reviews puts the call
in the most expensive tier, which also carries the smallest monthly free
allowance -- so this runs over the roster once a month at most, never per
device. Looking a place up by its stored Place ID (Place Details) is both
cheaper and more accurate than searching for it by name, so a text search is
only used to discover an ID we do not have yet, and the ID is saved so the
search never has to happen twice.

Responses are cached on disk. A re-run after a crash costs nothing, and
--dry-run can be replayed against the cache without spending a single call.

Hours are written back in the one shape build_data.parse_hours already reads
("Mon 9:00 AM-5:00 PM; Tue closed; ..."), which is why no parser change was
needed: we stopped writing prose instead of teaching it to read prose.

Requires NOTION_TOKEN and GOOGLE_PLACES_KEY in the environment.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_data import parse_hours  # noqa: E402
from sync_notion import (  # noqa: E402
    DATABASE_ID,
    SSL_CONTEXT,
    fetch_all_rows,
    notion_request,
    prop_text,
)

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "data/source/google-places-cache.json"
TODAY = datetime.now(ZoneInfo("America/New_York")).date().isoformat()

PLACES_DETAILS = "https://places.googleapis.com/v1/places/"
PLACES_SEARCH = "https://places.googleapis.com/v1/places:searchText"

# Every field we want, in one request. Adding to this list can move the call
# into a pricier tier, so keep it deliberate.
DETAIL_FIELDS = ",".join([
    "id", "displayName", "formattedAddress", "location", "businessStatus",
    "regularOpeningHours", "websiteUri", "priceLevel", "takeout", "dineIn",
    "accessibilityOptions", "restroom", "servesBreakfast", "servesLunch", "servesDinner",
    "nationalPhoneNumber", "rating", "userRatingCount", "reviews", "googleMapsUri",
])
SEARCH_FIELDS = ",".join(f"places.{f}" for f in DETAIL_FIELDS.split(","))

DAY_NAMES = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
# parse_hours wants Monday first; Google numbers days with Sunday as 0.
WRITE_ORDER = [1, 2, 3, 4, 5, 6, 0]

PRICE_LEVELS = {
    "PRICE_LEVEL_INEXPENSIVE": 1,
    "PRICE_LEVEL_MODERATE": 2,
    "PRICE_LEVEL_EXPENSIVE": 3,
    "PRICE_LEVEL_VERY_EXPENSIVE": 4,
}
PRICE_LABELS = {1: "$", 2: "$$", 3: "$$$", 4: "$$$$"}

# Google's vocabulary for "is this place still trading" is narrower than ours.
# Anything Google is not explicit about keeps whatever a human already decided.
BUSINESS_STATUS = {
    "OPERATIONAL": "likely_open",
    "CLOSED_TEMPORARILY": "temporarily_closed",
    "CLOSED_PERMANENTLY": "closed",
}


def google_request(url: str, key: str, fields: str, body: dict | None = None) -> dict:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method="POST" if body else "GET")
    req.add_header("X-Goog-Api-Key", key)
    req.add_header("X-Goog-FieldMask", fields)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30, context=SSL_CONTEXT) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"Places {url} -> {e.code}: {e.read().decode()[:400]}") from e


def lookup(place_id: str | None, name: str, address: str, zip_code: str | None, key: str) -> dict | None:
    """One place, by ID where we have one (cheaper and exact), else by search.

    A stored Place ID is an identity, so it is trusted. A text search is a
    guess: Google answers with its best match whether or not that match is the
    right restaurant, and the wrong one would be written into Notion as fact.
    So a searched result has to land in the ZIP we were looking in, or it is
    reported as unmatched and the row is left exactly as it was.
    """
    if place_id:
        return google_request(PLACES_DETAILS + urllib.parse.quote(place_id), key, DETAIL_FIELDS)
    body = {"textQuery": f"{name} {address}", "maxResultCount": 1}
    found = google_request(PLACES_SEARCH, key, SEARCH_FIELDS, body).get("places") or []
    if not found:
        return None
    candidate = found[0]
    if zip_code and zip_code not in (candidate.get("formattedAddress") or ""):
        return None
    return candidate


WEEK_MINUTES = 7 * 24 * 60


def hours_text(place: dict) -> str | None:
    """Google's opening periods as the semicolon format parse_hours reads.

    A period is a span between two points in the week, not a statement about
    one day. A Friday-and-Saturday 24-hour McDonald's arrives as a single
    period opening Thursday 7am and closing Sunday midnight, so reading only
    each period's opening day reports Friday and Saturday closed -- confident,
    wrong, and exactly the sort of thing someone would walk across a borough
    on. So paint the minutes each period actually covers onto the week, then
    read each day's spans back off that.

    Returns None rather than a partial string when the week cannot be stated
    unambiguously: a blank Hours cell is honest, a half-written one is not.
    """
    hours = place.get("regularOpeningHours")
    if not hours:
        return None
    periods = hours.get("periods") or []
    if not periods:
        return None

    # A single period with an open and no close is Google's "open 24/7".
    if len(periods) == 1 and "close" not in periods[0]:
        return "Open 24 hours daily"

    covered = bytearray(WEEK_MINUTES)
    for period in periods:
        start, end = period.get("open"), period.get("close")
        if not start or not end:
            return None  # an unpaired period means we cannot state the week
        try:
            begin = minute_of_week(start)
            finish = minute_of_week(end)
        except (TypeError, ValueError):
            return None
        # A close at or before the open wraps past the end of the week.
        length = (finish - begin) % WEEK_MINUTES or WEEK_MINUTES
        for offset in range(length):
            covered[(begin + offset) % WEEK_MINUTES] = 1

    if not any(covered):
        return None
    if all(covered):
        return "Open 24 hours daily"

    segments = []
    for day in WRITE_ORDER:
        spans = day_spans(covered, day)
        if not spans:
            segments.append(f"{DAY_NAMES[day]} closed")
            continue
        if len(spans) == 1 and spans[0] == (0, 1440):
            segments.append(f"{DAY_NAMES[day]} open 24 hours")
            continue
        # parse_hours takes one span per day segment, so a split day (lunch
        # and dinner) gets a segment per span.
        for begin, finish in spans:
            # "12:00 AM" as a close reads as the start of the day and makes the
            # parser roll the span into the next day, leaving an empty
            # midnight-to-midnight slot behind. "midnight" closes the day.
            close = "midnight" if finish == 1440 else hhmm(finish)
            segments.append(f"{DAY_NAMES[day]} {hhmm(begin)}-{close}")
    return "; ".join(segments)


def minute_of_week(point: dict) -> int:
    day, hour, minute = point["day"], point.get("hour", 0), point.get("minute", 0)
    if not 0 <= day <= 6 or not 0 <= hour <= 24 or not 0 <= minute <= 59:
        raise ValueError(f"Out-of-range time point: {point}")
    return (day * 24 * 60 + hour * 60 + minute) % WEEK_MINUTES


def day_spans(covered: bytearray, day: int) -> list[tuple[int, int]]:
    """The open spans inside one day, as minutes from that day's midnight."""
    base = day * 24 * 60
    spans: list[tuple[int, int]] = []
    begin = None
    for minute in range(1440):
        if covered[base + minute]:
            if begin is None:
                begin = minute
        elif begin is not None:
            spans.append((begin, minute))
            begin = None
    if begin is not None:
        spans.append((begin, 1440))
    return spans


def hhmm(minutes: int) -> str:
    """Minutes past midnight as "7:00 AM"; 1440 is written as midnight."""
    hour, minute = divmod(minutes % 1440, 60)
    suffix = "AM" if hour < 12 else "PM"
    return f"{hour % 12 or 12}:{minute:02d} {suffix}"


def clock(point: dict) -> str:
    hour, minute = point.get("hour", 0), point.get("minute", 0)
    suffix = "AM" if hour < 12 else "PM"
    display = hour % 12 or 12
    return f"{display}:{minute:02d} {suffix}"


# A review card in the app is a card, not an essay, and the longest reviews
# Google returns run well past a thousand characters. Cut them where a reader
# would stop anyway; the full text is one tap away on the Maps listing.
REVIEW_TEXT_LIMIT = 400


def reviews_of(place: dict) -> list[dict]:
    out = []
    for review in (place.get("reviews") or [])[:5]:
        text = ((review.get("text") or {}).get("text") or "").strip()
        rating = review.get("rating")
        if not text or not isinstance(rating, int) or not 1 <= rating <= 5:
            continue
        if len(text) > REVIEW_TEXT_LIMIT:
            cut = text[:REVIEW_TEXT_LIMIT].rsplit(" ", 1)[0].rstrip(" ,.;:—-")
            text = f"{cut}…"
        out.append({
            "author": ((review.get("authorAttribution") or {}).get("displayName") or "Google user").strip(),
            "rating": rating,
            "text": text,
            "when": (review.get("relativePublishTimeDescription") or "recently").strip(),
        })
    return out


# Notion caps a single rich-text run at 2000 characters but allows many runs
# per property, and reading a cell concatenates them. The reviews blob runs
# past 5000 characters on a chatty restaurant, so it has to be split rather
# than trimmed: a trimmed JSON blob is not shorter JSON, it is broken JSON,
# and the app would then show no reviews at all for exactly the places that
# have the most.
NOTION_RUN_LIMIT = 2000


def rich(value: str) -> dict:
    runs = [value[i:i + NOTION_RUN_LIMIT] for i in range(0, len(value), NOTION_RUN_LIMIT)] or [""]
    return {"rich_text": [{"type": "text", "text": {"content": run}} for run in runs]}


def notion_updates(place: dict, prior_status: str | None) -> dict:
    """The Notion cells this place's answer should set.

    Google wins on hours and coordinates by explicit decision. It does not win
    on business status: it only ever moves a row to a state it states plainly,
    so a human's "conflicting" or "rebranded" survives a bland OPERATIONAL.
    """
    props: dict = {"Google Checked": {"date": {"start": TODAY}}}

    if place.get("id"):
        props["Google Place ID"] = rich(place["id"])

    text = hours_text(place)
    if text and parse_hours(text):
        props["Hours"] = rich(text)
        props["Hours Status"] = {"select": {"name": "Cached"}}
        props["Hours Verified"] = {"date": {"start": TODAY}}

    location = place.get("location") or {}
    if isinstance(location.get("latitude"), (int, float)):
        props["Latitude"] = {"number": round(location["latitude"], 7)}
        props["Longitude"] = {"number": round(location["longitude"], 7)}

    if place.get("websiteUri"):
        props["Website"] = {"url": place["websiteUri"]}

    status = BUSINESS_STATUS.get(place.get("businessStatus", ""))
    # Only promote to likely_open from a status that was merely unconfirmed;
    # never overwrite a reviewed judgement with Google's default.
    if status and (status != "likely_open" or prior_status in (None, "unknown", "likely_open")):
        props["Business Status"] = {"select": {"name": status}}

    if isinstance(place.get("rating"), (int, float)):
        props["Rating"] = {"number": round(float(place["rating"]), 1)}
    if isinstance(place.get("userRatingCount"), int):
        props["Rating Count"] = {"number": place["userRatingCount"]}

    level = PRICE_LEVELS.get(place.get("priceLevel", ""))
    if level:
        props["Price Level"] = {"select": {"name": PRICE_LABELS[level]}}

    service_columns = (
        ("takeout", "Takeout"),
        ("dineIn", "Dine In"),
        ("restroom", "Restroom"),
        ("servesBreakfast", "Serves Breakfast"),
        ("servesLunch", "Serves Lunch"),
        ("servesDinner", "Serves Dinner"),
    )
    for field, column in service_columns:
        if isinstance(place.get(field), bool):
            props[column] = {"select": {"name": "Yes" if place[field] else "No"}}

    accessibility = place.get("accessibilityOptions") or {}
    accessibility_columns = (
        ("wheelchairAccessibleEntrance", "Wheelchair Entrance"),
        ("wheelchairAccessibleRestroom", "Wheelchair Restroom"),
        ("wheelchairAccessibleSeating", "Wheelchair Seating"),
        ("wheelchairAccessibleParking", "Wheelchair Parking"),
    )
    for field, column in accessibility_columns:
        if isinstance(accessibility.get(field), bool):
            props[column] = {"select": {"name": "Yes" if accessibility[field] else "No"}}

    # Review text is deliberately NOT written to Notion. Notion is the human
    # editing workspace: every column there is something a person might sit
    # down and change. Nobody hand-edits five reviews as a JSON blob in a
    # spreadsheet cell, and storing it that way is one bad escape away from
    # the app showing no reviews at all. The full responses are committed to
    # data/source/google-places-cache.json instead, and sync_notion.py reads
    # the reviews from there.

    return props


def load_cache() -> dict:
    if CACHE.exists():
        return json.loads(CACHE.read_text())
    return {}


def save_cache(cache: dict) -> None:
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    CACHE.write_text(json.dumps(cache, indent=2, sort_keys=True, ensure_ascii=False) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true",
                        help="Print what would change without writing a single Notion cell.")
    parser.add_argument("--limit", type=int, default=0,
                        help="Only process the first N rows. Use this to trial the run.")
    parser.add_argument("--only-missing", action="store_true",
                        help="Skip rows that already carry a Google Checked date.")
    parser.add_argument("--refresh-cache", action="store_true",
                        help="Ask Google again even for places already in the local cache.")
    args = parser.parse_args()

    token = os.environ.get("NOTION_TOKEN")
    key = os.environ.get("GOOGLE_PLACES_KEY")
    if not token or not key:
        print("NOTION_TOKEN and GOOGLE_PLACES_KEY must both be set.", file=sys.stderr)
        return 2

    cache = load_cache()
    rows = fetch_all_rows(token)
    rows.sort(key=lambda r: prop_text(r["properties"], "RMP Key") or "")
    if args.only_missing:
        rows = [r for r in rows if not prop_text(r["properties"], "Google Checked")]
    if args.limit:
        rows = rows[: args.limit]

    calls = skipped = written = 0
    unmatched: list[str] = []

    for row in rows:
        props = row["properties"]
        rmp_key = prop_text(props, "RMP Key") or row["id"]
        name = prop_text(props, "Restaurant") or ""
        address = " ".join(filter(None, [
            prop_text(props, "Address"), prop_text(props, "City"), "NY", prop_text(props, "ZIP"),
        ]))
        place_id = prop_text(props, "Google Place ID")

        cached = cache.get(rmp_key)
        if cached and not args.refresh_cache:
            place = cached
            skipped += 1
        else:
            try:
                place = lookup(place_id, name, address, prop_text(props, "ZIP"), key)
            except RuntimeError as err:
                print(f"  ! {rmp_key}: {err}", file=sys.stderr)
                continue
            calls += 1
            time.sleep(0.12)  # stay well clear of the per-second quota
            if place:
                cache[rmp_key] = place
                save_cache(cache)

        if not place:
            unmatched.append(rmp_key)
            print(f"  ? {rmp_key}: Google returned no match")
            continue

        updates = notion_updates(place, prop_text(props, "Business Status"))
        summary = ", ".join(sorted(updates))
        if args.dry_run:
            print(f"  · {rmp_key}: would set {summary}")
            continue
        notion_request("PATCH", f"pages/{row['id']}", token, {"properties": updates})
        written += 1
        print(f"  + {rmp_key}: set {summary}")

    print(f"\nrows={len(rows)} google_calls={calls} from_cache={skipped} "
          f"notion_writes={written} unmatched={len(unmatched)}")
    if unmatched:
        print("Unmatched (left exactly as they were):")
        for key_name in unmatched:
            print(f"  {key_name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
