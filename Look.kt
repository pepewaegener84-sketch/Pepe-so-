package tv.pepe.os

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

/** Gemeinsame Optik: Farben, Hintergruende, Kartenform. */
object Look {
    val BACKGROUNDS = arrayOf("Tiefschwarz", "Graphit", "Mitternacht", "Silberglanz")
    val ACCENT_NAMES = arrayOf("Silber", "Weiß", "Eisblau")
    val ACCENTS = intArrayOf(0xFFC8CCD2.toInt(), 0xFFFFFFFF.toInt(), 0xFF9EC9FF.toInt())
    val CARD = 0xFF1C1C1E.toInt()
    val TEXT = 0xFFF5F5F7.toInt()
    val TEXT_DIM = 0xFF8E8E93.toInt()

    private fun linear(top: Long, bottom: Long) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top.toInt(), bottom.toInt())
    )

    fun background(ctx: Context): GradientDrawable = when (Prefs.background) {
        1 -> linear(0xFF202024, 0xFF000000)
        2 -> linear(0xFF0B1426, 0xFF000000)
        3 -> GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = ctx.resources.displayMetrics.widthPixels * 0.8f
            setGradientCenter(0.5f, 0.2f)
            colors = intArrayOf(0xFF2E2E33.toInt(), 0xFF000000.toInt())
        }
        else -> GradientDrawable().apply { setColor(Color.BLACK) }
    }

    fun card(ctx: Context): GradientDrawable = GradientDrawable().apply {
        setColor(CARD)
        cornerRadius = ctx.dp(18).toFloat()
    }

    /** Einheitlicher Fokus-Look fuer Buttons. */
    fun styleButton(tv: TextView) {
        val c = tv.context
        val bg = card(c)
        tv.background = bg
        tv.setOnFocusChangeListener { _, focused ->
            bg.setStroke(if (focused) c.dp(3) else 0, Prefs.accent())
            tv.setTextColor(if (focused) Prefs.accent() else TEXT)
        }
    }
}
