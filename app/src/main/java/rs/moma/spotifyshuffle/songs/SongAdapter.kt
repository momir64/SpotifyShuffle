package rs.moma.spotifyshuffle.songs

import android.view.*
import android.view.View.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import rs.moma.spotifyshuffle.*
import kotlin.collections.ArrayList

var selectable = false

class SongAdapter(val songList: ArrayList<Song>, private val activity: SongActivity) : DragDropSwipeAdapter<Song, SongAdapter.SongViewHolder>(songList) {
    class SongViewHolder(songView: View) : DragDropSwipeAdapter.ViewHolder(songView) {
        var songImage: ImageView = songView.findViewById(R.id.song_image)
        var dragDots: ImageView = songView.findViewById(R.id.drag_dots)
        var songCard: CardView = songView.findViewById(R.id.song_card)
        var songArtist: TextView = songView.findViewById(R.id.song_artist)
        var songTitle: TextView = songView.findViewById(R.id.song_title)
        var songNum: TextView = songView.findViewById(R.id.song_num)
    }

    override fun getViewHolder(itemView: View): SongViewHolder = SongViewHolder(itemView)
    override fun getViewToTouchToStartDraggingItem(item: Song, viewHolder: SongViewHolder, position: Int): View = viewHolder.dragDots
    override fun onBindViewHolder(item: Song, viewHolder: SongViewHolder, position: Int) {
        with(viewHolder) {
            if (selectable) {
                dragDots.visibility = VISIBLE
                songNum.visibility = INVISIBLE
                activity.findViewById<ImageButton>(R.id.delete_button).visibility = VISIBLE
                activity.findViewById<ImageButton>(R.id.shuffle_button).visibility = INVISIBLE
            } else {
                for (i in songList.indices) {
                    songList[i].num = i + 1
                    songList[i].selected = false
                }
                dragDots.visibility = INVISIBLE
                songNum.visibility = VISIBLE
                activity.findViewById<ImageButton>(R.id.delete_button).visibility = INVISIBLE
                activity.findViewById<ImageButton>(R.id.shuffle_button).visibility = VISIBLE
                songNum.text = item.num.toString()
            }
            songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (item.selected) R.color.card_color_selected else R.color.card_color))
            Glide.with(activity).load(item.imageUrl).placeholder(R.drawable.ic_placeholder).into(songImage)
            songTitle.text = item.title
            songArtist.text = item.artist
            songCard.setOnClickListener {
                if (selectable) {
                    item.selected = !item.selected
                    songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (item.selected) R.color.card_color_selected else R.color.card_color))
                }
            }
            songCard.setOnLongClickListener {
                if (selectable) {
                    item.selected = !item.selected
                    songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (item.selected) R.color.card_color_selected else R.color.card_color))
                } else {
                    selectable = true
                    activity.songAdapter.notifyItemRangeChanged(0, songList.size)
                }
                true
            }
        }
    }
}