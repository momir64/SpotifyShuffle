package rs.moma.spotifyshuffle.songs

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SongTouchHelperCallback(private val adapter: SongAdapter) : ItemTouchHelper.Callback() {
    override fun isLongPressDragEnabled(): Boolean = false
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.swapSongs(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }
}