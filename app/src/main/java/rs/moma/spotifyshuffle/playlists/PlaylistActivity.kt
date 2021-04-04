package rs.moma.spotifyshuffle.playlists

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import okhttp3.*
import org.json.JSONObject
import rs.moma.spotifyshuffle.PreCachingLayoutManager
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.*


class PlaylistActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)

        recyclerView = findViewById(R.id.playlists_list)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = PreCachingLayoutManager(this)
        playlistAdapter = PlaylistAdapter(ArrayList(), this)
        recyclerView.adapter = playlistAdapter
        loadPlaylists(playlistAdapter)
    }

    private fun loadPlaylists(playlistAdapter: PlaylistAdapter) {
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
                }
                runOnUiThread {
                    playlistAdapter.addPlaylists(playlists)
                }
            } while (url != "null")
        }.start()
    }
}