package rs.moma.spotifyshuffle.songs

import android.annotation.SuppressLint
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import rs.moma.spotifyshuffle.*
import rs.moma.spotifyshuffle.global.dp2px
import java.util.Collections.swap

var selectable = false

class SongAdapter(private val activity: SongActivity) : RecyclerView.Adapter<SongViewHolder>() {
    val songList = ArrayList<Song>()
    override fun getItemCount() = songList.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        val viewHolder = SongViewHolder(view)

        viewHolder.itemView.findViewById<ImageView>(R.id.drag_dots).setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN)
                activity.songTouchHelper.startDrag(viewHolder)
            return@setOnTouchListener true
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bindData(songList, songList[position], activity, this)
    }

    fun moveSong(from: Int, to: Int) {
        swap(songList, from, to)
        notifyItemMoved(from, to)
    }
}

class SongViewHolder(songView: View) : RecyclerView.ViewHolder(songView) {
    private var songImage = songView.findViewById<ImageView>(R.id.song_image)
    private var dragDots = songView.findViewById<ImageView>(R.id.drag_dots)
    private var songCard = songView.findViewById<MaterialCardView>(R.id.song_card)
    private var songArtist = songView.findViewById<TextView>(R.id.song_artist)
    private var songTitle = songView.findViewById<TextView>(R.id.song_title)
    private var songNum = songView.findViewById<TextView>(R.id.song_num)

    @SuppressLint("ClickableViewAccessibility")
    fun bindData(songList: ArrayList<Song>, song: Song, activity: SongActivity, songAdapter: SongAdapter) {
        val copyButton = activity.findViewById<ImageButton>(R.id.copy_button)
        val deleteButton = activity.findViewById<ImageButton>(R.id.delete_button)
        val shuffleButton = activity.findViewById<ImageButton>(R.id.shuffle_button)
        if (selectable) {
            dragDots.visibility = VISIBLE
            songNum.visibility = INVISIBLE
            copyButton.visibility = VISIBLE
            deleteButton.visibility = VISIBLE
            shuffleButton.visibility = INVISIBLE
        } else {
            for (i in songList.indices) {
                songList[i].num = i + 1
                songList[i].selected = false
            }
            dragDots.visibility = INVISIBLE
            songNum.visibility = VISIBLE
            copyButton.visibility = INVISIBLE
            deleteButton.visibility = INVISIBLE
            shuffleButton.visibility = VISIBLE
            songNum.text = song.num.toString()
        }
        songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (song.selected) R.color.card_color_selected else R.color.card_color))
        songCard.strokeWidth = if (song.selected) dp2px(1.5, activity) else 0
        Glide.with(activity).load(song.imageUrl).placeholder(R.drawable.ic_placeholder).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).dontTransform().into(songImage)
        songTitle.text = song.title
        songArtist.text = song.artist
        songCard.setOnClickListener {
            if (selectable) {
                song.selected = !song.selected
                songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (song.selected) R.color.card_color_selected else R.color.card_color))
                songCard.strokeWidth = if (song.selected) dp2px(1.5, activity) else 0
            }
        }
        songCard.setOnLongClickListener {
            if (selectable) {
                song.selected = !song.selected
                songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (song.selected) R.color.card_color_selected else R.color.card_color))
                songCard.strokeWidth = if (song.selected) dp2px(1.5, activity) else 0
            } else {
                selectable = true
                song.selected = true
                val playlistTitle = activity.findViewById<TextView>(R.id.playlist_title)
                val playlistTitleParams = playlistTitle.layoutParams as ConstraintLayout.LayoutParams
                playlistTitleParams.marginEnd = dp2px(95, activity)
                playlistTitle.layoutParams = playlistTitleParams
                songAdapter.notifyItemRangeChanged(0, songList.size)
            }
            return@setOnLongClickListener true
        }
    }
}

class SongTouchHelperCallback(private val adapter: SongAdapter) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
    override fun isLongPressDragEnabled(): Boolean = false
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.moveSong(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }
}