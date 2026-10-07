package com.mike.rmpfinder.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity as actionStartIntent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.mike.rmpfinder.data.RmpRestaurant
import com.mike.rmpfinder.data.distanceMiles
import com.mike.rmpfinder.reviews.CachedReviews
import com.mike.rmpfinder.ui.RestaurantLogos
import com.mike.rmpfinder.ui.closesAt
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Home-screen widget: scrollable list of open RMP spots near you.
 * Features:
 * - Scrollable LazyColumn supporting up to 20 nearby open restaurants.
 * - Interactive 1-tap Google Maps directions pin for instant transit/driving.
 * - Manual tap-to-refresh button.
 * - Downsampled 36dp bitmaps to stay safely under Android IPC TransactionTooLarge limits.
 * - 2-second timeout protection on cold launches so widget never hangs.
 */
class NearbyWidget : GlanceAppWidget() {
    data class Spot(
        val key: String,
        val name: String,
        val miles: String,
        val closes: String?,
        val logo: Bitmap,
        val navUri: String,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RmpFinderApplication
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull()
        val now = Instant.now()
        // 36dp logo to prevent TransactionTooLargeException across IPC
        val logoPx = (36 * context.resources.displayMetrics.density).toInt()

        val allRestaurants = withTimeoutOrNull(2500) {
            runCatching { app.repository.restaurants.first() }.getOrNull()
        }.orEmpty()

        val spots = if (lat == null || lon == null || allRestaurants.isEmpty()) {
            emptyList()
        } else {
            allRestaurants
                .filter { OpenNow.isOpen(it, now) }
                .map { it to distanceMiles(lat, lon, it.latitude, it.longitude) }
                .sortedBy { it.second }
                .take(20)
                .map { (r, d) ->
                    val addr = r.currentAddress ?: r.officialAddress
                    val navUrl = CachedReviews.mapsDirectionsUrl(r, addr, "transit")
                    Spot(
                        key = r.rmpKey,
                        name = r.displayName,
                        miles = String.format(Locale.US, "%.1f mi", d),
                        closes = closesAt(r, now),
                        logo = RestaurantLogos.markerBitmap(context, r, logoPx),
                        navUri = navUrl,
                    )
                }
        }

        val backdrop = backdrop(context)
        provideContent {
            GlanceTheme {
                WidgetBody(
                    spots = spots,
                    hasLocation = lat != null,
                    backdrop = backdrop,
                )
            }
        }
    }

    @Composable
    private fun WidgetBody(spots: List<Spot>, hasLocation: Boolean, backdrop: Bitmap) {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(26.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Image(
                provider = ImageProvider(backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Widget Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Box(GlanceModifier.size(8.dp).background(Open).cornerRadius(4.dp)) {}
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        "OPEN NEAR YOU",
                        style = TextStyle(color = ColorProvider(Cream), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    // Tap to refresh widget
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(12.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable(actionRunCallback<RefreshWidgetAction>())
                    ) {
                        Text(
                            "↻ Refresh",
                            style = TextStyle(color = ColorProvider(CreamMuted), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }

                if (spots.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (hasLocation) "Nothing open nearby right now." else "Open RMP Finder once so it knows where you are.",
                            style = TextStyle(color = ColorProvider(CreamMuted), fontSize = 13.sp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxSize()
                    ) {
                        items(spots) { spot ->
                            val itemIntent = Intent(context, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_RMP_KEY, spot.key)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spot.navUri)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(actionStartIntent(itemIntent))
                            ) {
                                Image(
                                    provider = ImageProvider(spot.logo),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(36.dp),
                                )
                                Spacer(GlanceModifier.width(10.dp))
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        spot.name,
                                        maxLines = 1,
                                        style = TextStyle(color = ColorProvider(Cream), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        spot.closes?.let { "Open · closes $it" } ?: "Open now",
                                        maxLines = 1,
                                        style = TextStyle(color = ColorProvider(CreamMuted), fontSize = 11.sp),
                                    )
                                }
                                Spacer(GlanceModifier.width(6.dp))
                                // Distance pill
                                Box(GlanceModifier.background(PillFill).cornerRadius(10.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(spot.miles, style = TextStyle(color = ColorProvider(Cream), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }
                                Spacer(GlanceModifier.width(6.dp))
                                // Direct Maps Navigation Icon
                                Box(
                                    modifier = GlanceModifier
                                        .size(30.dp)
                                        .background(PillFill)
                                        .cornerRadius(15.dp)
                                        .clickable(actionStartIntent(mapsIntent)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("➤", style = TextStyle(color = ColorProvider(Butter), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Crimson-to-tomato gradient card backdrop. */
    private fun backdrop(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (360 * density).toInt().coerceAtLeast(100)
        val h = (240 * density).toInt().coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            0f, 0f, w.toFloat(), h.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(0xFFB83218.toInt(), 0xFF7A1B08.toInt(), 0xFF4E1006.toInt()),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawRect(
            0f, 0f, w.toFloat(), h.toFloat(),
            Paint().apply {
                shader = RadialGradient(
                    w * 0.95f, h * 0.05f, w * 0.7f,
                    intArrayOf(0x55FFCF4D, 0x00FFCF4D),
                    null,
                    Shader.TileMode.CLAMP,
                )
            },
        )
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

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NearbyWidget().updateAll(context)
    }
}

class NearbyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NearbyWidget()
}
