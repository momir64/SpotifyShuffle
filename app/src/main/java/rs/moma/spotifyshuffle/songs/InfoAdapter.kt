package rs.moma.spotifyshuffle.songs

import android.annotation.SuppressLint
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.moma.spotifyshuffle.R

class InfoAdapter : RecyclerView.Adapter<InfoViewHolder>() {
    override fun getItemCount() = if (visible) 1 else 0
    var totalLength = 0
    var totalCount = 0
    var visible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder =
            InfoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_info, parent, false))

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.bindData(totalCount, totalLength)
    }
}

class InfoViewHolder(infoView: View) : RecyclerView.ViewHolder(infoView) {
    private var totalLengthText = infoView.findViewById<TextView>(R.id.total_length)
    private var totalCountText = infoView.findViewById<TextView>(R.id.total_count)
    var res: Resources = infoView.resources

    @SuppressLint("ClickableViewAccessibility")
    fun bindData(totalCount: Int, totalLength: Int) {
        totalLengthText.text = when {
            totalLength / 3600000 == 0 -> res.getQuantityString(R.plurals.total_length0, ((totalLength / 60000) % 60).coerceAtLeast(1), ((totalLength / 60000) % 60).coerceAtLeast(1))
            totalLength / 3600000 == 1 -> res.getQuantityString(R.plurals.total_length1, (totalLength / 60000) % 60, totalLength / 3600000, (totalLength / 60000) % 60)
            else                       -> res.getQuantityString(R.plurals.total_length, (totalLength / 60000) % 60, totalLength / 3600000, (totalLength / 60000) % 60)
        }
        totalCountText.text = res.getQuantityString(R.plurals.total_count, totalCount, totalCount)
    }
}