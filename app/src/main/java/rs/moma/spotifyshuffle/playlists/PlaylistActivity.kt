package rs.moma.spotifyshuffle.playlists

import android.os.Bundle
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.VerticalSpaceItemDecoration
import rs.moma.spotifyshuffle.global.getToken

class PlaylistActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter
    private var reLoad: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)

        recyclerView = findViewById(R.id.playlists_list)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(applyDimension(COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()))
        playlistAdapter = PlaylistAdapter(ArrayList(), this)
        recyclerView.adapter = playlistAdapter
        loadPlaylists()
        reLoad = false
    }

    override fun onResume() {
        if (reLoad)
            reLoadPlaylists()
        else
            reLoad = true
        super.onResume()
    }

    private fun loadPlaylists() {
        Thread {
            var url = "https://api.spotify.com/v1/me/playlists?limit=50"
            do {
                val playlists = ArrayList<Playlist>()
                val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val obj = JSONObject(response.body!!.string())
                    url = obj.getString("next")
                    val items = obj.getJSONArray("items")
                    for (i in 0 until items.length()) {
                        val images = items.getJSONObject(i).getJSONArray("images")
                        if (images.length() > 0) {
                            val image = images.getJSONObject(if (images.length() == 1) 0 else 1).getString("url")
                            playlists.add(Playlist(items.getJSONObject(i).getString("id"),
                                                   items.getJSONObject(i).getString("name"),
                                                   items.getJSONObject(i).getString("description"),
                                                   image))
                        }
                    }
                    playlistAdapter.playlistList.addAll(playlists)
                    runOnUiThread { playlistAdapter.notifyItemRangeInserted(playlistAdapter.itemCount - playlists.size, playlists.size) }
                }
            } while (url != "null")
        }.start()
    }

    private fun reLoadPlaylists() {
        Thread {
            var j = -1
            var url = "https://api.spotify.com/v1/me/playlists?limit=50"
            do {
                val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val obj = JSONObject(response.body!!.string())
                    url = obj.getString("next")
                    val items = obj.getJSONArray("items")
                    for (i in 0 until items.length()) {
                        val images = items.getJSONObject(i).getJSONArray("images")
                        if (images.length() > 0) {
                            val image = images.getJSONObject(if (images.length() == 1) 0 else 1).getString("url")
                            val playlist = Playlist(items.getJSONObject(i).getString("id"),
                                                    items.getJSONObject(i).getString("name"),
                                                    items.getJSONObject(i).getString("description"),
                                                    image)
                            if (++j >= playlistAdapter.itemCount) {
                                playlistAdapter.playlistList.add(playlist)
                                runOnUiThread { playlistAdapter.notifyItemInserted(j) }
                            } else {
                                playlistAdapter.playlistList[j] = playlist
                                runOnUiThread { playlistAdapter.notifyItemRangeChanged(0, playlistAdapter.itemCount) }
                            }
                        }
                    }
                }
            } while (url != "null")
            if (++j < playlistAdapter.itemCount) {
                val d = playlistAdapter.itemCount - j
                playlistAdapter.playlistList.subList(j, playlistAdapter.itemCount).clear()
                runOnUiThread { playlistAdapter.notifyItemRangeRemoved(j, d) }
            }
        }.start()
    }
}