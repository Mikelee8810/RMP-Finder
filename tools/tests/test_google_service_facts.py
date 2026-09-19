import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import backfill_google  # noqa: E402
import sync_notion  # noqa: E402


YES = {"type": "select", "select": {"name": "Yes"}}
NO = {"type": "select", "select": {"name": "No"}}
UNKNOWN = {"type": "select", "select": {"name": "Unknown"}}


class GoogleServiceFactsTest(unittest.TestCase):
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

        props = backfill_google.notion_updates(place, None)

        self.assertEqual("Yes", props["Wheelchair Entrance"]["select"]["name"])
        self.assertEqual("No", props["Wheelchair Restroom"]["select"]["name"])
        self.assertEqual("Yes", props["Wheelchair Seating"]["select"]["name"])
        self.assertEqual("No", props["Wheelchair Parking"]["select"]["name"])
        self.assertEqual("Yes", props["Restroom"]["select"]["name"])
        self.assertEqual("Yes", props["Serves Breakfast"]["select"]["name"])
        self.assertEqual("No", props["Serves Lunch"]["select"]["name"])
        self.assertEqual("Yes", props["Serves Dinner"]["select"]["name"])
        self.assertEqual("Yes", props["Dine In"]["select"]["name"])

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
