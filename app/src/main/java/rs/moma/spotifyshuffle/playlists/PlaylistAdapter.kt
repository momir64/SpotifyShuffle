package rs.moma.spotifyshuffle.playlists

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rs.moma.spotifyshuffle.*
import rs.moma.spotifyshuffle.songs.SongActivity


class PlaylistAdapter(val playlistList: ArrayList<Playlist>, private val activity: PlaylistActivity) : RecyclerView.Adapter<PlaylistViewHolder>() {
    override fun getItemCount() = playlistList.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bindData(playlistList[position], activity)
    }
}

class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var playlistTitle = itemView.findViewById<TextView>(R.id.playlist_text)
    private var playlistImage = itemView.findViewById<ImageView>(R.id.playlist_image)
    private var playlistCard = itemView.findViewById<CardView>(R.id.playlist_card)

    fun bindData(playlist: Playlist, activity: PlaylistActivity) {
        Glide.with(activity).load(playlist.imageUrl).placeholder(R.drawable.ic_placeholder).into(playlistImage)
        playlistTitle.text = playlist.title
        playlistCard.setOnClickListener {
            val intent = Intent(activity, SongActivity::class.java)
            val bundle = Bundle()
            bundle.putString("playlistTitle", playlist.title)
            bundle.putString("playlistId", playlist.id)
            intent.putExtras(bundle)
            activity.startActivity(intent)
        }
    }
}