package rs.moma.spotifyshuffle.songs

import android.annotation.SuppressLint
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rs.moma.spotifyshuffle.*
import java.util.*
import kotlin.collections.ArrayList

var selectable = false

class SongAdapter(val songList: ArrayList<Song>, private val activity: SongActivity) : RecyclerView.Adapter<SongViewHolder>() {
    var deletedSongs = 0
    override fun getItemCount() = songList.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bindData(songList, songList[position], activity)
    }

    fun addSongs(songs: ArrayList<Song>) {
        songList.addAll(songs)
        notifyItemRangeInserted(itemCount - songs.size, songs.size)
    }

    fun swapSongs(from: Int, to: Int) {
        Collections.swap(songList, from, to)
        notifyItemMoved(from, to)
    }
}

class SongViewHolder(songView: View) : RecyclerView.ViewHolder(songView) {
    private var songImage = songView.findViewById<ImageView>(R.id.song_image)
    private var dragDots = songView.findViewById<ImageView>(R.id.drag_dots)
    private var songCard = songView.findViewById<CardView>(R.id.song_card)
    private var songArtist = songView.findViewById<TextView>(R.id.song_artist)
    private var songTitle = songView.findViewById<TextView>(R.id.song_title)
    private var songNum = songView.findViewById<TextView>(R.id.song_num)

    @SuppressLint("ClickableViewAccessibility")
    fun bindData(songList: ArrayList<Song>, song: Song, activity: SongActivity) {
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
            songNum.text = song.num.toString()
        }
        songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (song.selected) R.color.card_color_selected else R.color.card_color))
        Glide.with(activity).load(song.imageUrl).placeholder(R.drawable.ic_placeholder).into(songImage)
        songTitle.text = song.title
        songArtist.text = song.artist
        songCard.setOnClickListener {
            if (selectable) {
                song.selected = !song.selected
                songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (song.selected) R.color.card_color_selected else R.color.card_color))
            }
        }
        songCard.setOnLongClickListener {
            if (selectable) {
                song.selected = !song.selected
                songCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (song.selected) R.color.card_color_selected else R.color.card_color))
            } else {
                selectable = true
                activity.songAdapter.notifyItemRangeChanged(0, songList.size)
            }
            true
        }
        dragDots.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN)
                activity.songTouchHelper.startDrag(this)
            true
        }
    }
}