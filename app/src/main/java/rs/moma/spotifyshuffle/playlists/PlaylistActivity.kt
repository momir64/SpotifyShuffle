package rs.moma.spotifyshuffle.playlists

import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.PreCachingLayoutManager
import rs.moma.spotifyshuffle.global.VerticalSpaceItemDecoration
import rs.moma.spotifyshuffle.global.getToken
import rs.moma.spotifyshuffle.global.preferencePath
import rs.moma.spotifyshuffle.login.LoginActivity
import rs.moma.spotifyshuffle.songs.Song
import java.util.concurrent.Semaphore

class PlaylistActivity : AppCompatActivity() {
    lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter
    private var reLoad: Boolean = true
    private var itemVisibleCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)

        swipeContainer = findViewById(R.id.swipe_container)
        swipeContainer.setColorSchemeResources(R.color.green)
        swipeContainer.setOnRefreshListener { reLoadPlaylists() }
        findViewById<ImageButton>(R.id.back_button_playlist).setOnClickListener { onBackPressed() }
        findViewById<ImageButton>(R.id.logout_button).setOnClickListener { logOut() }
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
        findViewById<ImageButton>(R.id.playlist_add_button).setOnClickListener { selectable = true; playlistAdapter.notifyItemRangeChanged(0, playlistAdapter.itemCount) }
        findViewById<ImageButton>(R.id.playlist_check_button).setOnClickListener { createPlaylist() }
        reLoad = false
    }

    override fun onResume() {
        if (reLoad)
            reLoadPlaylists()
        else
            reLoad = true
        super.onResume()
    }

    override fun onBackPressed() {
        if (selectable) {
            selectable = false
            playlistAdapter.notifyItemRangeChanged(0, playlistAdapter.itemCount)
        } else
            super.onBackPressed()
    }

    private fun logOut() {
        getSharedPreferences(preferencePath, MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(R.anim.inmid, R.anim.midout)
        finish()
    }

    private fun createPlaylist() {
        val playlists = playlistAdapter.playlistList.filter { it.selected }
        selectable = false
        playlistAdapter.notifyItemRangeChanged(0, playlistAdapter.itemCount)
        if (playlists.isNotEmpty()) {
            val dp = applyDimension(COMPLEX_UNIT_DIP, 1F, resources.displayMetrics)
            val textInputLayout = TextInputLayout(this)
            val editText = TextInputEditText(textInputLayout.context)
            editText.setTextColor(resources.getColor(R.color.white, null))
            editText.setHintTextColor(resources.getColor(R.color.hint, null))
            editText.addTextChangedListener { textInputLayout.isErrorEnabled = false }
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            textInputLayout.setBoxCornerRadii(8 * dp, 8 * dp, 8 * dp, 8 * dp)
            textInputLayout.setBoxBackgroundColorResource(R.color.card_color)
            val states = arrayOf(intArrayOf(android.R.attr.state_enabled))
            val colors = intArrayOf(resources.getColor(R.color.error, null))
            val colorList = ColorStateList(states, colors)
            textInputLayout.boxStrokeErrorColor = colorList
            textInputLayout.setErrorIconTintList(colorList)
            textInputLayout.setPadding((24 * dp).toInt())
            textInputLayout.setErrorTextColor(colorList)
            editText.setPadding((16 * dp).toInt())
            textInputLayout.addView(editText)
            editText.hint = "Playlist name"
            val dialog = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                .setTitle("Create playlist")
                .setView(textInputLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", null)
                .show()
            editText.onFocusChangeListener = View.OnFocusChangeListener { _, _ ->
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
            dialog.getButton(BUTTON_POSITIVE).setOnClickListener {
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    val songs = ArrayList<Song>()
                    Thread {
                        for (playlist in playlists)
                            songs.addAll(getSongs(playlist.id))
                        addSongs2Playlist(songs.distinctBy { listOf(it.title, it.artist, it.duration) } as ArrayList<Song>, createPlaylist(title))
                        reLoadPlaylists()
                    }.start()
                    dialog.dismiss()
                } else
                    textInputLayout.error = "Playlist name can't be empty!"
            }
            editText.requestFocus()
        }
    }

    private fun addSongs2Playlist(songs: ArrayList<Song>, playlistId: String) {
        var offset = 0
        while (offset < songs.size) {
            val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
            val sublist = ArrayList<String>()
            for (i in offset until (offset + 100).coerceAtMost(songs.size))
                sublist.add(songs[i].uri)
            val body = JSONObject().put("uris", JSONArray(sublist)).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            OkHttpClient().newCall(Request.Builder().url(url).addHeader("Authorization", "Bearer " + getToken(this)).post(body).build()).execute()
            offset += 100
        }
    }

    private fun createPlaylist(title: String): String {
        val body = JSONObject().put("name", title).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/users/${getUserId()}/playlists")
            .addHeader("Authorization", "Bearer " + getToken(this))
            .post(body)
            .build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.isSuccessful) JSONObject(response.body!!.string()).getString("id") else ""
    }

    private fun getSongs(playlistId: String): ArrayList<Song> {
        val songs = ArrayList<Song>()
        var url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?market=from_token"
        do {
            val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url(url).build()
            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                val obj = JSONObject(response.body!!.string())
                url = obj.getString("next")
                val items = obj.getJSONArray("items")
                for (i in 0 until items.length())
                    if (!items.getJSONObject(i).isNull("track")) {
                        val track = items.getJSONObject(i).getJSONObject("track")
                        val artists = track.getJSONArray("artists")
                        var artist = artists.getJSONObject(0).getString("name")
                        for (j in 1 until artists.length())
                            artist += ", " + artists.getJSONObject(j).getString("name")
                        songs.add(Song(track.getString("uri"),
                                       track.getString("name"),
                                       artist, "",
                                       track.getInt("duration_ms")))
                    }
            }
        } while (url != "null")
        return songs
    }

    private fun getUserId(): String {
        val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url("https://api.spotify.com/v1/me").build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.isSuccessful) JSONObject(response.body!!.string()).getString("id") else ""
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
            runOnUiThread { playlistAdapter.notifyDataSetChanged() }
            swipeContainer.isRefreshing = false
        }.start()
    }

    private fun preloadFirst(x: Int, playlists: ArrayList<Playlist>) {
        val semaphore = Semaphore(1 - x)
        for (i in 0 until x)
            Glide.with(this).load(playlists[i].imageUrl).diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .dontTransform().listener(object : RequestListener<Drawable> {
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