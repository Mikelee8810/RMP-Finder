package com.mike.rmpfinder.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity as actionStartIntent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.mike.rmpfinder.ui.RestaurantLogos
import com.mike.rmpfinder.ui.closesAt
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Home-screen widget: the three closest RMP spots that are open right now,
 * each with its brand mark, closing time and distance on a tomato card.
 *
 * Distances come from the last location the app saw (saved in prefs), so the
 * widget works without asking for location itself. Android refreshes it every
 * 30 minutes and the app nudges it whenever a new location lands.
 */
class NearbyWidget : GlanceAppWidget() {
    private data class Spot(val key: String, val name: String, val miles: String, val closes: String?, val logo: Bitmap)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RmpFinderApplication
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull()
        val now = Instant.now()
        val logoPx = (44 * context.resources.displayMetrics.density).toInt()
        val spots = if (lat == null || lon == null) emptyList() else {
            app.repository.restaurants.first()
                .filter { OpenNow.isOpen(it, now) }
                .map { it to distanceMiles(lat, lon, it.latitude, it.longitude) }
                .sortedBy { it.second }
                .take(3)
                .map { (r, d) ->
                    Spot(r.rmpKey, r.displayName, String.format(Locale.US, "%.1f mi", d), closesAt(r, now), RestaurantLogos.markerBitmap(context, r, logoPx))
                }
        }
        val backdrop = backdrop(context)
        provideContent { GlanceTheme { WidgetBody(spots, hasLocation = lat != null, backdrop) } }
    }

    @Composable
    private fun WidgetBody(spots: List<Spot>, hasLocation: Boolean, backdrop: Bitmap) {
        val context = LocalContext.current
        Box(GlanceModifier.fillMaxSize().cornerRadius(26.dp).clickable(actionStartActivity<MainActivity>())) {
            Image(ImageProvider(backdrop), contentDescription = null, contentScale = ContentScale.Crop, modifier = GlanceModifier.fillMaxSize())
            Column(GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                    Box(GlanceModifier.size(9.dp).background(Open).cornerRadius(5.dp)) {}
                    Spacer(GlanceModifier.width(8.dp))
                    Text("OPEN NEAR YOU", style = TextStyle(color = ColorProvider(Cream), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.defaultWeight())
                    Box(GlanceModifier.background(Butter).cornerRadius(10.dp).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Text("10% OFF", style = TextStyle(color = ColorProvider(ButterInk), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                if (spots.isEmpty()) {
                    Text(
                        if (hasLocation) "Nothing open right now." else "Open RMP Finder once so it knows where you are.",
                        style = TextStyle(color = ColorProvider(CreamMuted), fontSize = 13.sp),
                    )
                }
                spots.forEach { spot ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable(actionStartIntent(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_RMP_KEY, spot.key))),
                    ) {
                        Image(ImageProvider(spot.logo), contentDescription = null, modifier = GlanceModifier.size(40.dp))
                        Spacer(GlanceModifier.width(12.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text(spot.name, maxLines = 1, style = TextStyle(color = ColorProvider(Cream), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                            Text(
                                spot.closes?.let { "Open · closes $it" } ?: "Open now",
                                maxLines = 1, style = TextStyle(color = ColorProvider(CreamMuted), fontSize = 12.sp),
                            )
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Box(GlanceModifier.background(PillFill).cornerRadius(12.dp).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(spot.miles, style = TextStyle(color = ColorProvider(Cream), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }

    /** Tomato card: diagonal crimson-to-tomato gradient with a soft cream glow in the corner. */
    private fun backdrop(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (340 * density).toInt()
        val h = (180 * density).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(0xFFB83218.toInt(), 0xFF7A1B08.toInt(), 0xFF4E1006.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        })
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = RadialGradient(w * 0.95f, h * 0.05f, w * 0.7f, intArrayOf(0x55FFCF4D, 0x00FFCF4D), null, Shader.TileMode.CLAMP)
        })
        return bitmap
    }

    companion object {
        const val PREFS = "rmp_prefs"
        const val KEY_LAT = "last_lat"
        const val KEY_LON = "last_lon"
        private val Cream = Color(0xFFFFF6EA)
        private val CreamMuted = Color(0xFFFFD9CC)
        private val Butter = Color(0xFFFFCF4D)
        private val ButterInk = Color(0xFF5C4200)
        private val Open = Color(0xFF4ADE80)
        private val PillFill = Color(0x33FFFFFF)

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
