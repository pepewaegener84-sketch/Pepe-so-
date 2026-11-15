package tv.pepe.os

import android.content.Intent
import android.graphics.drawable.Drawable

/** Eine tatsaechlich installierte, startbare App. */
data class AppEntry(
    val pkg: String,
    val activity: String,
    val label: String,
    val icon: Drawable,
    val isBanner: Boolean
) {
    fun intent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setClassName(pkg, activity)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}
