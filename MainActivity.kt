package tv.pepe.os

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var root: View
    private lateinit var scroll: ScrollView
    private lateinit var rvApps: RecyclerView
    private lateinit var rvRecent: RecyclerView
    private lateinit var rvFav: RecyclerView
    private lateinit var btnSettings: TextView
    private lateinit var apps: AppAdapter
    private lateinit var recents: AppAdapter
    private lateinit var favs: AppAdapter

    private var all: List<AppEntry> = emptyList()
    private val exec = Executors.newSingleThreadExecutor()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        scroll = findViewById(R.id.scroll)
        rvApps = findViewById(R.id.rvApps)
        rvRecent = findViewById(R.id.rvRecent)
        rvFav = findViewById(R.id.rvFav)
        btnSettings = findViewById(R.id.btnSettings)

        apps = AppAdapter(Section.APPS, 220, 124, ::launch, ::showMenu)
        recents = AppAdapter(Section.RECENT, 160, 90, ::launch, ::showMenu)
        favs = AppAdapter(Section.FAV, 160, 90, ::launch, ::showMenu)

        listOf(rvApps to apps, rvRecent to recents, rvFav to favs).forEach { (rv, a) ->
            rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            rv.adapter = a
            rv.itemAnimator = null
        }

        Look.styleButton(btnSettings)
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(pkgReceiver, f)
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(pkgReceiver) } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        reload()
    }

    override fun onDestroy() {
        super.onDestroy()
        exec.shutdown()
    }

    private fun applyTheme() {
        root.background = Look.background(this)
        val f = if (Prefs.clock24) "HH:mm" else "h:mm a"
        findViewById<TextClock>(R.id.clock).apply { format24Hour = f; format12Hour = f }
        findViewById<TextClock>(R.id.date).apply {
            format24Hour = "EEEE, d. MMMM yyyy"
            format12Hour = "EEEE, d. MMMM yyyy"
        }
    }

    private fun reload() {
        exec.execute {
            val list = AppRepository.load(packageManager, packageName)
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    all = list
                    refreshRows()
                }
            }
        }
    }

    private fun refreshRows() {
        val byPkg = all.associateBy { it.pkg }
        apps.submit(all)
        recents.submit(Prefs.recents().mapNotNull { byPkg[it] }.take(8))
        favs.submit(Prefs.favorites().mapNotNull { byPkg[it] })
        findViewById<View>(R.id.recentBlock).visibility = if (recents.size > 0) View.VISIBLE else View.GONE
        findViewById<View>(R.id.favHint).visibility = if (favs.size > 0) View.GONE else View.VISIBLE
        rvFav.visibility = if (favs.size > 0) View.VISIBLE else View.GONE
        rvApps.postDelayed({ if (currentFocus == null) focusFirst() }, 80)
    }

    private fun focusFirst() {
        val v = rvApps.findViewHolderForAdapterPosition(0)?.itemView
        if (v != null) v.requestFocus() else btnSettings.requestFocus()
    }

    private fun launch(e: AppEntry) {
        try {
            startActivity(e.intent())
            Prefs.pushRecent(e.pkg)
        } catch (ex: Exception) {
            toast("„${e.label}“ lässt sich nicht starten.")
            reload()
        }
    }

    private fun showMenu(e: AppEntry, section: Section, v: View) {
        val pos = (v.parent as? RecyclerView)?.getChildAdapterPosition(v) ?: -1
        val labels = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()
        fun add(l: String, a: () -> Unit) { labels.add(l); actions.add(a) }

        add("Starten") { launch(e) }
        if (Prefs.isFavorite(e.pkg)) {
            add("Aus Favoriten entfernen") { Prefs.toggleFavorite(e.pkg); refreshRows() }
        } else {
            add("Zu Favoriten hinzufügen") { Prefs.toggleFavorite(e.pkg); refreshRows() }
        }
        if (section == Section.FAV) {
            if (pos > 0) add("Nach links verschieben") { moveFav(e, -1) }
            if (pos in 0 until favs.size - 1) add("Nach rechts verschieben") { moveFav(e, 1) }
        }
        add("App-Info (Systemeinstellungen)") { openInfo(e) }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(e.label)
            .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
            .show()
    }

    private fun moveFav(e: AppEntry, delta: Int) {
        val j = Prefs.moveFavorite(e.pkg, delta)
        refreshRows()
        rvFav.scrollToPosition(j)
        rvFav.postDelayed({ rvFav.findViewHolderForAdapterPosition(j)?.itemView?.requestFocus() }, 120)
    }

    private fun openInfo(e: AppEntry) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${e.pkg}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (ex: Exception) {
            toast("App-Info ist auf diesem Gerät nicht verfügbar.")
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    /** Menue-Taste: App-Menue auf einer Kachel, sonst Einstellungen. */
    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        if (ev.keyCode == KeyEvent.KEYCODE_MENU) {
            if (ev.action == KeyEvent.ACTION_UP) {
                val f = currentFocus
                val e = f?.getTag(R.id.tag_entry) as? AppEntry
                val s = f?.getTag(R.id.tag_section) as? Section
                if (f != null && e != null && s != null) showMenu(e, s, f)
                else startActivity(Intent(this, SettingsActivity::class.java))
            }
            return true
        }
        return super.dispatchKeyEvent(ev)
    }

    /** Zurueck: nach oben springen, die Startseite nie verlassen. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        scroll.smoothScrollTo(0, 0)
        rvApps.scrollToPosition(0)
        rvApps.postDelayed({ focusFirst() }, 100)
    }
}
