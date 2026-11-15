package tv.pepe.os

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

enum class Section { APPS, RECENT, FAV }

class AppAdapter(
    private val section: Section,
    private val cardW: Int,
    private val cardH: Int,
    private val onClick: (AppEntry) -> Unit,
    private val onMenu: (AppEntry, Section, View) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()
    val size: Int get() = items.size

    /** Gibt false zurueck, wenn sich nichts geaendert hat (Fokus bleibt erhalten). */
    fun submit(n: List<AppEntry>): Boolean {
        if (n.map { it.pkg } == items.map { it.pkg }) return false
        items = n
        notifyDataSetChanged()
        return true
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: FrameLayout = v.findViewById(R.id.card)
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
        val bg = Look.card(v.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val c = parent.context
        val v = LayoutInflater.from(c).inflate(R.layout.item_app, parent, false)
        v.layoutParams = RecyclerView.LayoutParams(c.dp(cardW), ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginEnd = c.dp(14) }
        val vh = VH(v)
        vh.card.layoutParams.height = c.dp(cardH)
        vh.card.background = vh.bg
        v.setOnFocusChangeListener { _, focused -> focusChanged(vh, focused) }
        v.setOnClickListener { items.getOrNull(vh.bindingAdapterPosition)?.let(onClick) }
        v.setOnLongClickListener {
            items.getOrNull(vh.bindingAdapterPosition)?.let { onMenu(it, section, v) }
            true
        }
        return vh
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val e = items[position]
        val c = h.itemView.context
        h.itemView.setTag(R.id.tag_entry, e)
        h.itemView.setTag(R.id.tag_section, section)
        h.label.text = e.label
        h.icon.setImageDrawable(e.icon)
        if (e.isBanner) {
            h.icon.scaleType = ImageView.ScaleType.CENTER_CROP
            h.icon.setPadding(0, 0, 0, 0)
        } else {
            h.icon.scaleType = ImageView.ScaleType.FIT_CENTER
            val p = c.dp(cardH) / 5
            h.icon.setPadding(p, p, p, p)
        }
        if (!h.itemView.hasFocus()) {
            h.bg.setStroke(0, 0)
            h.label.setTextColor(Look.TEXT_DIM)
            h.itemView.scaleX = 1f
            h.itemView.scaleY = 1f
            h.itemView.translationZ = 0f
        }
    }

    override fun getItemCount(): Int = items.size

    private fun focusChanged(h: VH, focused: Boolean) {
        val c = h.itemView.context
        h.bg.setStroke(if (focused) c.dp(3) else 0, Prefs.accent())
        h.label.setTextColor(if (focused) Prefs.accent() else Look.TEXT_DIM)
        val s = if (focused && Prefs.anim) 1.08f else 1f
        h.itemView.animate()
            .scaleX(s).scaleY(s)
            .translationZ(if (focused) c.dp(12).toFloat() else 0f)
            .setDuration(if (Prefs.anim) 160L else 0L)
            .start()
    }
}
