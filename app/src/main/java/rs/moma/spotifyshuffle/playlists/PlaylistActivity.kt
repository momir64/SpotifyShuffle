package rs.moma.spotifyshuffle.playlists

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.PreCachingLayoutManager
import rs.moma.spotifyshuffle.global.VerticalSpaceItemDecoration
import rs.moma.spotifyshuffle.global.getToken
import java.util.concurrent.Semaphore

class PlaylistActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter
    private var reLoad: Boolean = true
    private var itemVisibleCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)

        recyclerView = findViewById(R.id.playlists_list)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = PreCachingLayoutManager(this)
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(applyDimension(COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()))
        playlistAdapter = PlaylistAdapter(ArrayList(), this)
        recyclerView.adapter = playlistAdapter
        recyclerView.doOnLayout {
            itemVisibleCount = it.measuredHeight / applyDimension(COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt()
            loadPlaylists()
        }
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
            var x = 0
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
                            if (++x == itemVisibleCount || (i + 1 == items.length() && x < itemVisibleCount))
                                preloadFirst(x, playlists)
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
                            if (++j >= playlistAdapter.itemCount)
                                playlistAdapter.playlistList.add(playlist)
                            else
                                playlistAdapter.playlistList[j] = playlist
                        }
                    }
                }
            } while (url != "null")
            if (++j < playlistAdapter.itemCount)
                playlistAdapter.playlistList.subList(j, playlistAdapter.itemCount).clear()
            preloadFirst(itemVisibleCount.coerceAtMost(playlistAdapter.itemCount), playlistAdapter.playlistList)
            runOnUiThread { playlistAdapter.notifyItemRangeChanged(0, playlistAdapter.itemCount) }
        }.start()
    }

    private fun preloadFirst(x: Int, playlists: ArrayList<Playlist>) {
        val semaphore = Semaphore(1 - x)
        for (i in 0 until x)
            Glide.with(this).load(playlists[i].imageUrl).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                    semaphore.release()
                    return false
                }

                override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                    semaphore.release()
                    return false
                }
            }).preload()
        semaphore.acquire()
    }
}