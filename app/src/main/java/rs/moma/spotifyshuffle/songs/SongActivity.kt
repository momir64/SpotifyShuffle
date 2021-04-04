package rs.moma.spotifyshuffle.songs

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import org.json.JSONObject
import rs.moma.spotifyshuffle.*
import rs.moma.spotifyshuffle.global.*
import java.util.Collections.shuffle

class SongActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    lateinit var songTouchHelper: ItemTouchHelper
    lateinit var songAdapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_songs)

        findViewById<ImageButton>(R.id.back_button).setOnClickListener { onBackPressed() }
        findViewById<TextView>(R.id.playlistTitle).text = intent.extras?.getString("playlistTitle")
        recyclerView = findViewById(R.id.songs_list)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = PreCachingLayoutManager(this)
        songAdapter = SongAdapter(ArrayList(), this)
        recyclerView.adapter = songAdapter
        songTouchHelper = ItemTouchHelper(SongTouchHelperCallback(songAdapter))
        songTouchHelper.attachToRecyclerView(recyclerView)
        loadSongs(songAdapter, intent.extras?.getString("playlistId")!!)
        findViewById<ImageButton>(R.id.delete_button).setOnClickListener { deleteSelected(songAdapter) }
        findViewById<ImageButton>(R.id.shuffle_button).setOnClickListener {
            shuffle(songAdapter.songList)
            songAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(0)
        }
        findViewById<FloatingActionButton>(R.id.done_button).setOnClickListener { updateSongs(songAdapter) }
    }

    private fun updateSongs(songAdapter: SongAdapter) {

    }

    private fun deleteSelected(songAdapter: SongAdapter) {
        selectable = false
        with(songAdapter) {
            var i = 0
            while (i < songList.size) {
                if (songList[i].selected) {
                    songList.removeAt(i)
                    notifyItemRemoved(i)
                    deletedSongs++
                } else {
                    notifyItemChanged(i)
                    i++
                }
            }
        }
    }

    override fun onBackPressed() {
        if (selectable) {
            selectable = false
            songAdapter.notifyDataSetChanged()
        } else
            super.onBackPressed()
    }

    private fun loadSongs(songAdapter: SongAdapter, playlistId: String) {
        Thread {
            var step = 0
            var url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?market=from_token"
            do {
                val songs = ArrayList<Song>()
                val request = Request.Builder()
                    .addHeader("Authorization", "Bearer " + getToken(this))
                    .url(url)
                    .build()
                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val obj = JSONObject(response.body!!.string())
                    url = obj.getString("next")
                    with(obj.getJSONArray("items")) {
                        for (i in 0 until length()) {
                            val track = getJSONObject(i).getJSONObject("track")
                            val images = track.getJSONObject("album").getJSONArray("images")
                            if (images.length() > 0 && !getJSONObject(i).getBoolean("is_local")) {
                                val artists = track.getJSONArray("artists")
                                var artist = artists.getJSONObject(0).getString("name")
                                for (j in 1 until artists.length())
                                    artist += ", " + artists.getJSONObject(j).getString("name")
                                val image = images.getJSONObject(if (images.length() == 1) 0 else 1).getString("url")
                                songs.add(Song(i + 1 + step * 100,
                                               track.getString("id"),
                                               track.getString("name"),
                                               artist,
                                               image))
                            }
                        }
                    }
                }
                runOnUiThread {
                    songAdapter.addSongs(songs)
                }
                step++
            } while (url != "null")
        }.start()
    }
}