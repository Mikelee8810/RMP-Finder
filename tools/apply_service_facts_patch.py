#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text()
    if old not in text:
        raise SystemExit(f"Expected patch anchor not found in {path}: {old[:120]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"Patch anchor is not unique in {path}: {old[:120]!r}")
    file.write_text(text.replace(old, new, 1))


replace_once(
    "tools/backfill_google.py",
    '    "regularOpeningHours", "websiteUri", "priceLevel", "takeout", "dineIn",\n    "nationalPhoneNumber", "rating", "userRatingCount", "reviews", "googleMapsUri",\n',
    '    "regularOpeningHours", "websiteUri", "priceLevel", "takeout", "dineIn",\n    "accessibilityOptions", "restroom", "servesBreakfast", "servesLunch", "servesDinner",\n    "nationalPhoneNumber", "rating", "userRatingCount", "reviews", "googleMapsUri",\n',
)

replace_once(
    "tools/backfill_google.py",
    '''    for field, column in (("takeout", "Takeout"), ("dineIn", "Dine In")):\n        if isinstance(place.get(field), bool):\n            props[column] = {"select": {"name": "Yes" if place[field] else "No"}}\n''',
    '''    service_columns = (\n        ("takeout", "Takeout"),\n        ("dineIn", "Dine In"),\n        ("restroom", "Restroom"),\n        ("servesBreakfast", "Serves Breakfast"),\n        ("servesLunch", "Serves Lunch"),\n        ("servesDinner", "Serves Dinner"),\n    )\n    for field, column in service_columns:\n        if isinstance(place.get(field), bool):\n            props[column] = {"select": {"name": "Yes" if place[field] else "No"}}\n\n    accessibility = place.get("accessibilityOptions") or {}\n    accessibility_columns = (\n        ("wheelchairAccessibleEntrance", "Wheelchair Entrance"),\n        ("wheelchairAccessibleRestroom", "Wheelchair Restroom"),\n        ("wheelchairAccessibleSeating", "Wheelchair Seating"),\n        ("wheelchairAccessibleParking", "Wheelchair Parking"),\n    )\n    for field, column in accessibility_columns:\n        if isinstance(accessibility.get(field), bool):\n            props[column] = {"select": {"name": "Yes" if accessibility[field] else "No"}}\n''',
)

replace_once(
    "tools/sync_notion.py",
    '''        "takeout": prop_text(props, "Takeout"),\n        "dineIn": prop_text(props, "Dine In"),\n        "googleChecked": prop_text(props, "Google Checked"),\n''',
    '''        "takeout": prop_text(props, "Takeout"),\n        "dineIn": prop_text(props, "Dine In"),\n        "wheelchairAccessibleEntrance": prop_text(props, "Wheelchair Entrance"),\n        "wheelchairAccessibleRestroom": prop_text(props, "Wheelchair Restroom"),\n        "wheelchairAccessibleSeating": prop_text(props, "Wheelchair Seating"),\n        "wheelchairAccessibleParking": prop_text(props, "Wheelchair Parking"),\n        "restroom": prop_text(props, "Restroom"),\n        "servesBreakfast": prop_text(props, "Serves Breakfast"),\n        "servesLunch": prop_text(props, "Serves Lunch"),\n        "servesDinner": prop_text(props, "Serves Dinner"),\n        "googleChecked": prop_text(props, "Google Checked"),\n''',
)

replace_once(
    "tools/sync_notion.py",
    '''            "takeout": yes_no(row["takeout"], prior.get("takeout") if prior else None),\n            "dineIn": yes_no(row["dineIn"], prior.get("dineIn") if prior else None),\n            "reviews": cached_reviews.get(key) or (prior.get("reviews", []) if prior else []),\n''',
    '''            "takeout": yes_no(row["takeout"], prior.get("takeout") if prior else None),\n            "dineIn": yes_no(row["dineIn"], prior.get("dineIn") if prior else None),\n            "wheelchairAccessibleEntrance": yes_no(row["wheelchairAccessibleEntrance"], prior.get("wheelchairAccessibleEntrance") if prior else None),\n            "wheelchairAccessibleRestroom": yes_no(row["wheelchairAccessibleRestroom"], prior.get("wheelchairAccessibleRestroom") if prior else None),\n            "wheelchairAccessibleSeating": yes_no(row["wheelchairAccessibleSeating"], prior.get("wheelchairAccessibleSeating") if prior else None),\n            "wheelchairAccessibleParking": yes_no(row["wheelchairAccessibleParking"], prior.get("wheelchairAccessibleParking") if prior else None),\n            "restroom": yes_no(row["restroom"], prior.get("restroom") if prior else None),\n            "servesBreakfast": yes_no(row["servesBreakfast"], prior.get("servesBreakfast") if prior else None),\n            "servesLunch": yes_no(row["servesLunch"], prior.get("servesLunch") if prior else None),\n            "servesDinner": yes_no(row["servesDinner"], prior.get("servesDinner") if prior else None),\n            "reviews": cached_reviews.get(key) or (prior.get("reviews", []) if prior else []),\n''',
)

replace_once(
    "data/restaurant.schema.json",
    '''    "dineIn": {\n      "type": [\n        "boolean",\n        "null"\n      ]\n    },\n    "reviews": {\n''',
    '''    "dineIn": {\n      "type": [\n        "boolean",\n        "null"\n      ]\n    },\n    "wheelchairAccessibleEntrance": { "type": ["boolean", "null"] },\n    "wheelchairAccessibleRestroom": { "type": ["boolean", "null"] },\n    "wheelchairAccessibleSeating": { "type": ["boolean", "null"] },\n    "wheelchairAccessibleParking": { "type": ["boolean", "null"] },\n    "restroom": { "type": ["boolean", "null"] },\n    "servesBreakfast": { "type": ["boolean", "null"] },\n    "servesLunch": { "type": ["boolean", "null"] },\n    "servesDinner": { "type": ["boolean", "null"] },\n    "reviews": {\n''',
)

replace_once(
    "app/src/main/java/com/mike/rmpfinder/data/RmpModels.kt",
    '''    val takeout: Boolean?,\n    val dineIn: Boolean?,\n    /** Up to five reviews, frozen at the last refresh. The app never fetches its own. */\n''',
    '''    val takeout: Boolean?,\n    val dineIn: Boolean?,\n    val wheelchairAccessibleEntrance: Boolean? = null,\n    val wheelchairAccessibleRestroom: Boolean? = null,\n    val wheelchairAccessibleSeating: Boolean? = null,\n    val wheelchairAccessibleParking: Boolean? = null,\n    val restroom: Boolean? = null,\n    val servesBreakfast: Boolean? = null,\n    val servesLunch: Boolean? = null,\n    val servesDinner: Boolean? = null,\n    /** Up to five reviews, frozen at the last refresh. The app never fetches its own. */\n''',
)

replace_once(
    "app/src/main/java/com/mike/rmpfinder/data/DatasetCodec.kt",
    '''"priceLevel", "takeout", "dineIn", "reviews",''',
    '''"priceLevel", "takeout", "dineIn", "wheelchairAccessibleEntrance", "wheelchairAccessibleRestroom", "wheelchairAccessibleSeating", "wheelchairAccessibleParking", "restroom", "servesBreakfast", "servesLunch", "servesDinner", "reviews",''',
)

replace_once(
    "app/src/main/java/com/mike/rmpfinder/data/DatasetCodec.kt",
    '''        val takeout = obj.nullableBoolean("takeout")\n        val dineIn = obj.nullableBoolean("dineIn")\n        val reviews = parseReviews(obj.arrayOrEmpty("reviews"))\n''',
    '''        val takeout = obj.nullableBoolean("takeout")\n        val dineIn = obj.nullableBoolean("dineIn")\n        val wheelchairAccessibleEntrance = obj.nullableBoolean("wheelchairAccessibleEntrance")\n        val wheelchairAccessibleRestroom = obj.nullableBoolean("wheelchairAccessibleRestroom")\n        val wheelchairAccessibleSeating = obj.nullableBoolean("wheelchairAccessibleSeating")\n        val wheelchairAccessibleParking = obj.nullableBoolean("wheelchairAccessibleParking")\n        val restroom = obj.nullableBoolean("restroom")\n        val servesBreakfast = obj.nullableBoolean("servesBreakfast")\n        val servesLunch = obj.nullableBoolean("servesLunch")\n        val servesDinner = obj.nullableBoolean("servesDinner")\n        val reviews = parseReviews(obj.arrayOrEmpty("reviews"))\n''',
)

replace_once(
    "app/src/main/java/com/mike/rmpfinder/data/DatasetCodec.kt",
    '''            googlePlaceId, rating, ratingCount, priceLevel, takeout, dineIn, reviews, googleCheckedAt,\n''',
    '''            googlePlaceId, rating, ratingCount, priceLevel, takeout, dineIn,\n            wheelchairAccessibleEntrance, wheelchairAccessibleRestroom, wheelchairAccessibleSeating,\n            wheelchairAccessibleParking, restroom, servesBreakfast, servesLunch, servesDinner,\n            reviews, googleCheckedAt,\n''',
)

replace_once(
    "app/src/main/java/com/mike/rmpfinder/ui/RmpFinderUi.kt",
    '''    val photo = remember(restaurant.rmpKey) { RestaurantPhotos.forRestaurant(context, restaurant) }\n    val blurb = remember(restaurant.rmpKey) { RestaurantBlurbs.forRestaurant(context, restaurant) }\n    WarmGround(underStatusBar = true) {\n''',
    '''    val photo = remember(restaurant.rmpKey) { RestaurantPhotos.forRestaurant(context, restaurant) }\n    val blurb = remember(restaurant.rmpKey) { RestaurantBlurbs.forRestaurant(context, restaurant) }\n    val hasServiceFacts = listOf(\n        restaurant.dineIn, restaurant.restroom,\n        restaurant.servesBreakfast, restaurant.servesLunch, restaurant.servesDinner,\n        restaurant.wheelchairAccessibleEntrance, restaurant.wheelchairAccessibleRestroom,\n        restaurant.wheelchairAccessibleSeating, restaurant.wheelchairAccessibleParking,\n    ).any { it != null }\n    WarmGround(underStatusBar = true) {\n''',
)

replace_once(
    "app/src/main/java/com/mike/rmpfinder/ui/RmpFinderUi.kt",
    '''            item {\n                DetailCard("Hours") {\n''',
    '''            if (hasServiceFacts) {\n                item {\n                    DetailCard("At this location") {\n                        restaurant.dineIn?.let { Text("Dine-in: ${if (it) "Yes" else "No"}", style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted) }\n                        restaurant.restroom?.let { Text("Restroom: ${if (it) "Yes" else "No"}", style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted) }\n\n                        val meals = listOf(\n                            "Breakfast" to restaurant.servesBreakfast,\n                            "Lunch" to restaurant.servesLunch,\n                            "Dinner" to restaurant.servesDinner,\n                        ).filter { it.second != null }\n                        if (meals.isNotEmpty()) {\n                            Text(\n                                "Meals: " + meals.joinToString(" • ") { (name, value) -> "$name ${if (value == true) "Yes" else "No"}" },\n                                style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted,\n                            )\n                        }\n\n                        val access = listOf(\n                            "Entrance" to restaurant.wheelchairAccessibleEntrance,\n                            "Restroom" to restaurant.wheelchairAccessibleRestroom,\n                            "Seating" to restaurant.wheelchairAccessibleSeating,\n                            "Parking" to restaurant.wheelchairAccessibleParking,\n                        ).filter { it.second != null }\n                        if (access.isNotEmpty()) {\n                            Text(\n                                "Wheelchair access: " + access.joinToString(" • ") { (name, value) -> "$name ${if (value == true) "Yes" else "No"}" },\n                                style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted,\n                            )\n                        }\n                    }\n                }\n            }\n\n            item {\n                DetailCard("Hours") {\n''',
)

print("Applied service-facts patch successfully.")
