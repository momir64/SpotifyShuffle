package rs.moma.spotifyshuffle.songs

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
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
import java.util.concurrent.atomic.AtomicInteger
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
        recyclerView.doOnLayout {
            Loader.itemVisibleCount = it.measuredHeight / applyDimension(COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt()
            Loader.loaded = AtomicInteger()
            loadSongs(intent.extras?.getString("playlistId")!!)
        }
        findViewById<ImageButton>(R.id.delete_button).setOnClickListener { deleteSelected(songAdapter) }
        findViewById<ImageButton>(R.id.shuffle_button).setOnClickListener {
            shuffle(songAdapter.songList)
            songAdapter.notifyItemRangeChanged(0, songAdapter.itemCount)
            recyclerView.scrollToPosition(0)
        }
        findViewById<FloatingActionButton>(R.id.done_button).setOnClickListener { updateSongs(songAdapter) }
    }

    private fun updateSongs(songAdapter: SongAdapter) {
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
                val songs = Collections.synchronizedList(ArrayList<Song>())
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
                                synchronized(songs) {
                                    songs.add(Song(i + 1 + step * 100,
                                                   track.getString("uri"),
                                                   track.getString("name"),
                                                   artist,
                                                   image))
                                }
                                if (step == 0 && i < Loader.itemVisibleCount)
                                    Loader.loadFirst(image, songs, songAdapter, this@SongActivity)
                            }
                        }
                    }
                    Loader.loadLater(songs, songAdapter, this@SongActivity)
                }
                step++
            } while (url != "null")
        }.start()
    }

    object Loader {
        var loaded = AtomicInteger()
        var itemVisibleCount = 0
        fun loadFirst(image: String, songs: MutableList<Song>, adapter: SongAdapter, activity: SongActivity) {
            Glide.with(activity).load(image).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean = false
                override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                    loaded.incrementAndGet()
                    if (loaded.toInt() == itemVisibleCount) activity.runOnUiThread {
                        synchronized(songs) {
                            adapter.songList.addAll(songs.subList(0, itemVisibleCount))
                            songs.subList(0, itemVisibleCount).clear()
                        }
                        adapter.notifyItemRangeInserted(0, itemVisibleCount)
                        loadLater(songs, adapter, activity)
                    }
                    return false
                }
            }).preload()
        }

        fun loadLater(songs: MutableList<Song>, adapter: SongAdapter, activity: SongActivity) {
            if (loaded.toInt() == itemVisibleCount) activity.runOnUiThread {
                synchronized(songs) {
                    val size = songs.size
                    adapter.songList.addAll(songs)
                    songs.clear()
                    adapter.notifyItemRangeInserted(adapter.itemCount - size, size)
                    Thread {
                        startChain(adapter.itemCount - size, adapter.itemCount, adapter, activity)
                    }.start()
                }
            }
        }

        fun startChain(from: Int, to: Int, adapter: SongAdapter, activity: SongActivity) {
            if (from < to) {
                Glide.with(activity).load(adapter.songList[from].imageUrl).listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean = false
                    override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                        startChain(from + 1, to, adapter, activity)
                        return false
                    }
                }).preload()
            }
        }
    }
}