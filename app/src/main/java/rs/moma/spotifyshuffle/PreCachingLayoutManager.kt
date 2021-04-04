package rs.moma.spotifyshuffle

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PreCachingLayoutManager(private val context: Context) : LinearLayoutManager(context) {
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        val displayMetrics = DisplayMetrics()
        context.display?.getRealMetrics(displayMetrics)
        return displayMetrics.heightPixels * 3
    }
}