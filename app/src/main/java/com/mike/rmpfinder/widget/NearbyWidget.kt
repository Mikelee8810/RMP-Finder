package com.mike.rmpfinder.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity as actionStartIntent
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mike.rmpfinder.MainActivity
import com.mike.rmpfinder.RmpFinderApplication
import com.mike.rmpfinder.data.OpenNow
import com.mike.rmpfinder.data.distanceMiles
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Home-screen widget: the three closest RMP spots that are open right now.
 *
 * Distances come from the last location the app saw (saved in prefs), so the
 * widget works without asking for location itself. Android refreshes it every
 * 30 minutes and the app nudges it whenever a new location lands.
 */
class NearbyWidget : GlanceAppWidget() {
    private data class Spot(val key: String, val name: String, val miles: String)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RmpFinderApplication
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull()
        val now = Instant.now()
        val spots = if (lat == null || lon == null) emptyList() else {
            app.repository.restaurants.first()
                .filter { OpenNow.isOpen(it, now) }
                .map { it to distanceMiles(lat, lon, it.latitude, it.longitude) }
                .sortedBy { it.second }
                .take(3)
                .map { (r, d) -> Spot(r.rmpKey, r.displayName, String.format(Locale.US, "%.1f mi", d)) }
        }
        provideContent { GlanceTheme { WidgetBody(spots, hasLocation = lat != null) } }
    }

    @Composable
    private fun WidgetBody(spots: List<Spot>, hasLocation: Boolean) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Ink).cornerRadius(24.dp).padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Box(GlanceModifier.size(8.dp).background(Open).cornerRadius(4.dp)) {}
                Spacer(GlanceModifier.width(8.dp))
                Text("OPEN NEAR YOU", style = TextStyle(color = ColorProvider(Cream), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                Text("10% off", style = TextStyle(color = ColorProvider(Butter), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.height(10.dp))
            if (spots.isEmpty()) {
                Text(
                    if (hasLocation) "Nothing open right now." else "Open RMP Finder once so it knows where you are.",
                    style = TextStyle(color = ColorProvider(CreamMuted), fontSize = 13.sp),
                )
            }
            val context = LocalContext.current
            spots.forEach { spot ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable(actionStartIntent(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_RMP_KEY, spot.key))),
                ) {
                    Text(spot.name, maxLines = 1, style = TextStyle(color = ColorProvider(Cream), fontSize = 15.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    Text(spot.miles, style = TextStyle(color = ColorProvider(Butter), fontSize = 13.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
    }

    companion object {
        const val PREFS = "rmp_prefs"
        const val KEY_LAT = "last_lat"
        const val KEY_LON = "last_lon"
        private val Ink = Color(0xFF2A170E)
        private val Cream = Color(0xFFFFF6EA)
        private val CreamMuted = Color(0xFFD9C8B8)
        private val Butter = Color(0xFFFFCF4D)
        private val Open = Color(0xFF4ADE80)

        /** Saves the newest location and redraws every copy of the widget. */
        suspend fun refresh(context: Context, latitude: Double, longitude: Double) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAT, latitude.toString()).putString(KEY_LON, longitude.toString()).apply()
            NearbyWidget().updateAll(context)
        }
    }
}

class NearbyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NearbyWidget()
}
