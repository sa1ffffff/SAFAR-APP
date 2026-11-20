package com.safar.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Pictures for rich notifications, drawn on the phone so no map service is needed:
 * a ride's route over a muted, map-like ground with the rider's pickup ringed, and
 * people's photos cropped round.
 */
object RouteArt {
    private val GROUND = Color.parseColor("#E7E2D6")
    private val ROAD = Color.parseColor("#F4F1EA")
    private val INK = Color.parseColor("#1B1E26")
    private val SURFACE = Color.parseColor("#FBFAF6")

    /** [route] and [pickup] are (lat, lng); returns a 2:1 picture with the route framed in it. */
    fun route(route: List<Pair<Double, Double>>, pickup: Pair<Double, Double>?, width: Int = 1024, height: Int = 512): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(GROUND)

        // A few soft "streets" so the ground reads as a map, not a blank card.
        val road = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ROAD; strokeWidth = height * 0.045f; strokeCap = Paint.Cap.ROUND }
        c.drawLine(-50f, height * 0.82f, width + 50f, height * 0.30f, road)
        c.drawLine(width * 0.30f, -50f, width * 0.42f, height + 50f, road)
        road.strokeWidth = height * 0.018f
        c.drawLine(-50f, height * 0.18f, width + 50f, height * 0.05f, road)
        c.drawLine(width * 0.78f, -50f, width * 0.84f, height + 50f, road)
        c.drawLine(-50f, height * 0.62f, width + 50f, height * 0.95f, road)

        val points = route.ifEmpty { listOfNotNull(pickup) }
        if (points.isEmpty()) return bmp
        val all = points + listOfNotNull(pickup)
        val midLat = all.map { it.first }.average()
        val k = cos(Math.toRadians(midLat))
        val xs = all.map { it.second * k }
        val ys = all.map { it.first }
        val pad = height * 0.2f
        val spanX = max(xs.max() - xs.min(), 1e-4)
        val spanY = max(ys.max() - ys.min(), 1e-4)
        val scale = min((width - 2 * pad) / spanX, (height - 2 * pad) / spanY)
        val ox = (width - spanX * scale) / 2 - xs.min() * scale
        val oy = (height - spanY * scale) / 2 + ys.max() * scale
        fun px(p: Pair<Double, Double>) = (p.second * k * scale + ox).toFloat() to (oy - p.first * scale).toFloat()

        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { i, p -> val (x, y) = px(p); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK; style = Paint.Style.STROKE; strokeWidth = height * 0.022f
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            })
            val (ex, ey) = px(points.last())
            val s = height * 0.045f
            c.drawRoundRect(RectF(ex - s, ey - s, ex + s, ey + s), s * 0.3f, s * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })
        }
        pickup?.let {
            val (x, y) = px(it)
            c.drawCircle(x, y, height * 0.15f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; alpha = 24 })
            c.drawCircle(x, y, height * 0.045f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SURFACE })
            c.drawCircle(x, y, height * 0.045f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK; style = Paint.Style.STROKE; strokeWidth = height * 0.03f
            })
        }
        return bmp
    }

    /** Downloads a photo and crops it round; null if it can't be fetched quickly. */
    fun roundPhoto(url: String, size: Int = 192): Bitmap? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 4_000
        conn.readTimeout = 4_000
        val src = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
        conn.disconnect()
        val side = min(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        Canvas(out).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        out
    }.getOrNull()
}
