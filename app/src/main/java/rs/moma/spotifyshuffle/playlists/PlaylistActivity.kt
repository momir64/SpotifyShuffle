package rs.moma.spotifyshuffle.playlists

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.TypedValue.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class PlaylistActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)

        recyclerView = findViewById(R.id.playlists_list)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(applyDimension(COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()))
        playlistAdapter = PlaylistAdapter(ArrayList(), this)
        recyclerView.adapter = playlistAdapter
        loadPlaylists(playlistAdapter)
    }

    override fun onStart() {
        super.onStart()
        Timer().schedule(500) {
            for (playlist in playlistAdapter.playlistList)
                Glide.with(this@PlaylistActivity).load(playlist.imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).preload()
        }
    }

    override fun onResume() {
        playlistAdapter.playlistList.clear()
        loadPlaylists(playlistAdapter, false)
        super.onResume()
    }

    private fun loadPlaylists(playlistAdapter: PlaylistAdapter, notify: Boolean = true) {
        Thread {
            var url = "https://api.spotify.com/v1/me/playlists?limit=50"
            do {
                val playlists = ArrayList<Playlist>()
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
                            val images = getJSONObject(i).getJSONArray("images")
                            if (images.length() > 0) {
                                val image = images.getJSONObject(if (images.length() == 1) 0 else 1).getString("url")
                                playlists.add(Playlist(getJSONObject(i).getString("id"),
                                                       getJSONObject(i).getString("name"),
                                                       getJSONObject(i).getString("description"),
                                                       image))
                            }
                        }
                    }
                    runOnUiThread {
                        playlistAdapter.playlistList.addAll(playlists)
                        if (notify)
                            playlistAdapter.notifyItemRangeInserted(playlistAdapter.itemCount - playlists.size, playlists.size)
                    }
                }
            } while (url != "null")
            runOnUiThread {
                if (!notify)
                    playlistAdapter.notifyItemRangeChanged(0, playlistAdapter.playlistList.size)
            }
        }.start()
    }
}