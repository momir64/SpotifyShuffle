package rs.moma.spotifyshuffle.songs

import android.os.Bundle
import android.util.TypedValue.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import rs.moma.spotifyshuffle.*
import rs.moma.spotifyshuffle.global.*
import java.util.*
import java.util.Collections.shuffle
import kotlin.collections.ArrayList

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
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(applyDimension(COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()))
        songAdapter = SongAdapter(this)
        recyclerView.adapter = songAdapter
        songTouchHelper = ItemTouchHelper(SongTouchHelperCallback(songAdapter))
        songTouchHelper.attachToRecyclerView(recyclerView)
        loadSongs(intent.extras?.getString("playlistId")!!)
        findViewById<ImageButton>(R.id.delete_button).setOnClickListener { deleteSelected(songAdapter) }
        findViewById<ImageButton>(R.id.shuffle_button).setOnClickListener {
            recyclerView.scrollToPosition(0)
            shuffle(songAdapter.songList)
            songAdapter.notifyItemRangeChanged(0, songAdapter.itemCount)
        }
        findViewById<FloatingActionButton>(R.id.done_button).setOnClickListener { updateSongs() }
    }

    private fun updateSongs() {
        selectable = false
        for (i in songAdapter.songList.indices)
            songAdapter.notifyItemChanged(i)
        Thread {
            var offset = 0
            while (offset < songAdapter.songList.size) {
                val url = "https://api.spotify.com/v1/playlists/${intent.extras?.getString("playlistId")!!}/tracks"
                val list = ArrayList<String>()
                for (i in offset until (offset + 100).coerceAtMost(songAdapter.songList.size))
                    list.add(songAdapter.songList[i].uri)
                val body = JSONObject().put("uris", JSONArray(list)).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + getToken(this@SongActivity))
                if (offset == 0)
                    request.put(body)
                else
                    request.post(body)
                val response = OkHttpClient().newCall(request.build()).execute()
                runOnUiThread {
                    if (response.isSuccessful)
                        showToast("Successfully updated", this@SongActivity)
                    else
                        showToast("Error occurred", this@SongActivity)
                }
                offset += 100
            }
        }.start()
    }

    private fun deleteSelected(songAdapter: SongAdapter) {
        selectable = false
        with(songAdapter) {
            var i = 0
            while (i < songList.size) {
                if (songList[i].selected) {
                    songList.removeAt(i)
                    notifyItemRemoved(i)
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
            songAdapter.notifyItemRangeChanged(0, songAdapter.itemCount)
        } else
            super.onBackPressed()
    }

    private fun loadSongs(playlistId: String) {
        Thread {
            var step = 0
            var url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?market=from_token"
            do {
                val songs = ArrayList<Song>()
                val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val obj = JSONObject(response.body!!.string())
                    url = obj.getString("next")
                    val items = obj.getJSONArray("items")
                    var x = 0
                    for (i in 0 until items.length())
                        if (!items.getJSONObject(i).isNull("track")) {
                            val track = items.getJSONObject(i).getJSONObject("track")
                            val images = track.getJSONObject("album").getJSONArray("images")
                            if (images.length() > 0 && !items.getJSONObject(i).getBoolean("is_local")) {
                                val artists = track.getJSONArray("artists")
                                var artist = artists.getJSONObject(0).getString("name")
                                for (j in 1 until artists.length())
                                    artist += ", " + artists.getJSONObject(j).getString("name")
                                val image = images.getJSONObject(if (images.length() == 1) 0 else 1).getString("url")
                                songs.add(Song(++x + step * 100,
                                               track.getString("uri"),
                                               track.getString("name"),
                                               artist,
                                               image))
                            }
                        }
                    runOnUiThread {
                        songAdapter.songList.addAll(songs)
                        songAdapter.notifyItemRangeInserted(songAdapter.itemCount - songs.size, songs.size)
                    }
                }
                step++
            } while (url != "null")
        }.start()
    }
}