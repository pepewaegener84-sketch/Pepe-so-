package tv.pepe.os

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var root: FrameLayout
    private val rows = ArrayList<Pair<TextView, () -> String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)

        root = FrameLayout(this)
        root.background = Look.background(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(36), dp(48), dp(36))
        }
        col.addView(TextView(this).apply {
            text = "Einstellungen"
            textSize = 26f
            setTextColor(Look.TEXT)
            letterSpacing = 0.05f
            setPadding(0, 0, 0, dp(20))
        })

        fun row(text: () -> String, action: () -> Unit) {
            val tv = TextView(this).apply {
                textSize = 17f
                setTextColor(Look.TEXT)
                setPadding(dp(22), dp(16), dp(22), dp(16))
                isFocusable = true
                isClickable = true
                layoutParams = LinearLayout.LayoutParams(dp(560), ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(12) }
            }
            Look.styleButton(tv)
            tv.setOnClickListener { action(); refresh() }
            rows.add(tv to text)
            col.addView(tv)
        }

        row({ "Hintergrund: ${Look.BACKGROUNDS[Prefs.background]}" }) {
            Prefs.background = (Prefs.background + 1) % Look.BACKGROUNDS.size
            root.background = Look.background(this)
        }
        row({ "Akzentfarbe: ${Look.ACCENT_NAMES[Prefs.accentIndex]}" }) {
            Prefs.accentIndex = (Prefs.accentIndex + 1) % Look.ACCENTS.size
        }
        row({ "Uhrzeit: ${if (Prefs.clock24) "24 Stunden" else "12 Stunden"}" }) {
            Prefs.clock24 = !Prefs.clock24
        }
        row({ "Fokus-Animation: ${if (Prefs.anim) "An" else "Aus"}" }) {
            Prefs.anim = !Prefs.anim
        }
        row({ "Fire-TV-Systemeinstellungen öffnen" }) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(this, "Auf diesem Gerät nicht verfügbar.", Toast.LENGTH_LONG).show()
            }
        }
        row({ "Über PEPE TV OS" }) { showAbout() }

        col.addView(TextView(this).apply {
            text = "OK = ändern    Zurück = Startseite"
            textSize = 13f
            setTextColor(Look.TEXT_DIM)
            setPadding(0, dp(8), 0, 0)
        })

        val sv = ScrollView(this).apply { addView(col) }
        root.addView(sv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL
        ))
        setContentView(root)
        refresh()
        rows.first().first.requestFocus()
    }

    private fun refresh() {
        rows.forEach { (tv, t) -> tv.text = t() }
    }

    private fun showAbout() {
        val v = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("PEPE TV OS $v")
            .setMessage(
                "Eigenständige Startseiten-App, kein Systemersatz.\n" +
                "Die Home-Taste öffnet weiterhin die Fire-TV-Oberfläche.\n" +
                "Alle Daten bleiben lokal auf diesem Gerät."
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
