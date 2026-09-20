import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import backfill_google  # noqa: E402
import sync_notion  # noqa: E402
from unittest.mock import patch  # noqa: E402


YES = {"type": "select", "select": {"name": "Yes"}}
NO = {"type": "select", "select": {"name": "No"}}
UNKNOWN = {"type": "select", "select": {"name": "Unknown"}}


class GoogleServiceFactsTest(unittest.TestCase):
    def test_address_match_requires_house_number_and_street(self):
        self.assertTrue(backfill_google.address_matches(
            "162-02 Jamaica Avenue Jamaica NY 11432", "11432",
            "162-02 Jamaica Ave, Jamaica, NY 11432, USA",
        ))
        self.assertTrue(backfill_google.address_matches(
            "6259 Fresh Pond Road Queens NY 11385", "11385",
            "62-59 Fresh Pond Rd, Ridgewood, NY 11385, USA",
        ))
        self.assertFalse(backfill_google.address_matches(
            "162-02 Jamaica Avenue Jamaica NY 11432", "11432",
            "164-17 Jamaica Ave, Jamaica, NY 11432, USA",
        ))
        self.assertTrue(backfill_google.address_matches(
            "6259 Fresh Pond Road Queens NY 11365", "11365",
            "62-59 Fresh Pond Rd, Ridgewood, NY 11385, USA",
        ))
        self.assertFalse(backfill_google.address_matches(
            "6259 Fresh Pond Road Queens NY 11385", "11385",
            "62-59 Metropolitan Ave, Ridgewood, NY 11385, USA",
        ))
        self.assertTrue(backfill_google.address_matches(
            "4 East Gun Hill Road Bronx NY 10467", "10467",
            "4 E Gun Hill Rd, Bronx, NY 10469, USA",
        ))
        self.assertTrue(backfill_google.address_matches(
            "1397 Rockaway Parkway Brooklyn NY 11236", "11236",
            "THE JERRY BUILDING, 1397 Rockaway Pkwy, Brooklyn, NY 11236, USA",
        ))
        self.assertTrue(backfill_google.address_matches(
            "907-911 South Street Peekskill NY 10566", "10566",
            "907-911 South St, Peekskill, NY 10566, USA",
        ))

    @patch("backfill_google.google_request")
    def test_lookup_rechecks_stored_id_and_rejects_wrong_search_result(self, request):
        request.side_effect = [
            {"id": "wrong", "formattedAddress": "164-17 Jamaica Ave, Jamaica, NY 11432, USA"},
            {"places": [{"id": "still-wrong", "formattedAddress": "164-17 Jamaica Ave, Jamaica, NY 11432, USA"}]},
        ]

        result = backfill_google.lookup(
            "wrong", "Jamaican Flavors", "162-02 Jamaica Avenue Jamaica NY 11432", "11432", "key"
        )

        self.assertIsNone(result)
        self.assertEqual(2, request.call_count)

    def test_field_mask_requests_accessibility_restroom_and_meals(self):
        requested = set(backfill_google.DETAIL_FIELDS.split(","))
        self.assertTrue({
            "accessibilityOptions",
            "restroom",
            "servesBreakfast",
            "servesLunch",
            "servesDinner",
        }.issubset(requested))

    def test_notion_updates_writes_accessibility_restroom_meals_and_dine_in(self):
        place = {
            "accessibilityOptions": {
                "wheelchairAccessibleEntrance": True,
                "wheelchairAccessibleRestroom": False,
                "wheelchairAccessibleSeating": True,
                "wheelchairAccessibleParking": False,
            },
            "restroom": True,
            "servesBreakfast": True,
            "servesLunch": False,
            "servesDinner": True,
            "dineIn": True,
        }

        props = backfill_google.notion_updates(place)

        self.assertEqual("Yes", props["Wheelchair Entrance"]["select"]["name"])
        self.assertEqual("No", props["Wheelchair Restroom"]["select"]["name"])
        self.assertEqual("Yes", props["Wheelchair Seating"]["select"]["name"])
        self.assertEqual("No", props["Wheelchair Parking"]["select"]["name"])
        self.assertEqual("Yes", props["Restroom"]["select"]["name"])
        self.assertEqual("Yes", props["Serves Breakfast"]["select"]["name"])
        self.assertEqual("No", props["Serves Lunch"]["select"]["name"])
        self.assertEqual("Yes", props["Serves Dinner"]["select"]["name"])
        self.assertEqual("Yes", props["Dine In"]["select"]["name"])

    def test_notion_updates_preserves_human_reviewed_service_facts(self):
        place = {
            "takeout": False,
            "dineIn": False,
            "restroom": False,
            "servesBreakfast": False,
            "servesLunch": False,
            "servesDinner": False,
            "accessibilityOptions": {"wheelchairAccessibleEntrance": False},
            "parkingOptions": {"freeStreetParking": False},
        }
        prior = {
            "Takeout": "Yes",
            "Dine In": "Yes",
            "Restroom": "Yes",
            "Serves Breakfast": "Yes",
            "Serves Lunch": "Yes",
            "Serves Dinner": "Yes",
            "Wheelchair Entrance": "Yes",
            "Parking": "Yes",
        }

        props = backfill_google.notion_updates(place, prior)

        for column in prior:
            self.assertNotIn(column, props)

    def test_notion_updates_fills_only_unknown_service_facts(self):
        place = {"dineIn": True, "restroom": False}
        props = backfill_google.notion_updates(place, {"Dine In": "Unknown", "Restroom": "Yes"})

        self.assertEqual("Yes", props["Dine In"]["select"]["name"])
        self.assertNotIn("Restroom", props)

    def test_parse_row_reads_new_notion_columns(self):
        page = {
            "id": "row-1",
            "properties": {
                "Wheelchair Entrance": YES,
                "Wheelchair Restroom": NO,
                "Wheelchair Seating": YES,
                "Wheelchair Parking": NO,
                "Restroom": YES,
                "Serves Breakfast": YES,
                "Serves Lunch": NO,
                "Serves Dinner": YES,
                "Dine In": YES,
            },
        }

        row = sync_notion.parse_row(page)

        self.assertEqual("Yes", row["wheelchairAccessibleEntrance"])
        self.assertEqual("No", row["wheelchairAccessibleRestroom"])
        self.assertEqual("Yes", row["wheelchairAccessibleSeating"])
        self.assertEqual("No", row["wheelchairAccessibleParking"])
        self.assertEqual("Yes", row["restroom"])
        self.assertEqual("Yes", row["servesBreakfast"])
        self.assertEqual("No", row["servesLunch"])
        self.assertEqual("Yes", row["servesDinner"])
        self.assertEqual("Yes", row["dineIn"])


if __name__ == "__main__":
    unittest.main()
