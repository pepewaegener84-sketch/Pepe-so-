package tv.pepe.os

import android.content.Intent
import android.content.pm.PackageManager

/** Ermittelt die installierten und startbaren Apps (Leanback + normale Launcher-Apps). */
object AppRepository {

    // Bevorzugte Reihenfolge. Erscheint nur, wenn die App wirklich installiert ist.
    private val FEATURED = listOf(
        "com.netflix.ninja",
        "com.amazon.firetv.youtube",
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus"
    )

    fun load(pm: PackageManager, ownPackage: String): List<AppEntry> {
        val map = LinkedHashMap<String, AppEntry>()
        val categories = listOf(
            Intent.CATEGORY_LEANBACK_LAUNCHER to true,
            Intent.CATEGORY_LAUNCHER to false
        )
        for ((category, leanback) in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == ownPackage || map.containsKey(ai.packageName)) continue
                val banner = if (leanback) ai.loadBanner(pm) else null
                val icon = banner ?: ri.loadIcon(pm)
                map[ai.packageName] = AppEntry(
                    pkg = ai.packageName,
                    activity = ai.name,
                    label = ri.loadLabel(pm).toString(),
                    icon = icon,
                    isBanner = banner != null
                )
            }
        }
        fun rank(p: String): Int = FEATURED.indexOf(p).let { if (it < 0) Int.MAX_VALUE else it }
        return map.values.sortedWith(
            compareBy<AppEntry>({ rank(it.pkg) }, { it.label.lowercase() })
        )
    }
}
