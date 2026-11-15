package tv.pepe.os

import android.content.Context
import android.content.SharedPreferences

/** Lokale Einstellungen. Keine Cloud, keine Konten. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (!::sp.isInitialized) {
            sp = c.applicationContext.getSharedPreferences("pepe", Context.MODE_PRIVATE)
        }
    }

    var background: Int
        get() = sp.getInt("bg", 1)
        set(v) { sp.edit().putInt("bg", v).apply() }

    var accentIndex: Int
        get() = sp.getInt("accent", 0)
        set(v) { sp.edit().putInt("accent", v).apply() }

    var clock24: Boolean
        get() = sp.getBoolean("clock24", true)
        set(v) { sp.edit().putBoolean("clock24", v).apply() }

    var anim: Boolean
        get() = sp.getBoolean("anim", true)
        set(v) { sp.edit().putBoolean("anim", v).apply() }

    fun accent(): Int = Look.ACCENTS[accentIndex.coerceIn(0, Look.ACCENTS.size - 1)]

    private fun list(key: String): List<String> =
        (sp.getString(key, "") ?: "").split("\n").filter { it.isNotBlank() }

    private fun save(key: String, l: List<String>) {
        sp.edit().putString(key, l.joinToString("\n")).apply()
    }

    fun favorites(): List<String> = list("fav")
    fun isFavorite(pkg: String): Boolean = favorites().contains(pkg)

    fun toggleFavorite(pkg: String) {
        val l = favorites().toMutableList()
        if (!l.remove(pkg)) l.add(pkg)
        save("fav", l)
    }

    /** Verschiebt ein Favorit; gibt die neue Position zurueck. */
    fun moveFavorite(pkg: String, delta: Int): Int {
        val l = favorites().toMutableList()
        val i = l.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j < 0 || j >= l.size) return i
        l.removeAt(i)
        l.add(j, pkg)
        save("fav", l)
        return j
    }

    fun recents(): List<String> = list("rec")

    fun pushRecent(pkg: String) {
        val l = recents().toMutableList()
        l.remove(pkg)
        l.add(0, pkg)
        save("rec", l.take(12))
    }
}
