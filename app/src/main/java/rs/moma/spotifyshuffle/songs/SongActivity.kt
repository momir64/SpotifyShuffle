package rs.moma.spotifyshuffle.songs

import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue.*
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.View.SCROLLBARS_OUTSIDE_INSET
import android.view.WindowManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import rs.moma.spotifyshuffle.*
import rs.moma.spotifyshuffle.global.*
import java.util.*
import java.util.Collections.shuffle
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList


class SongActivity : AppCompatActivity() {
    private lateinit var playlistId: String
    private lateinit var playlistTitle: String
    private lateinit var songAdapter: SongAdapter
    private lateinit var recyclerView: RecyclerView
    lateinit var songTouchHelper: ItemTouchHelper
    private var semaphore = Semaphore(1)
    private var itemVisibleCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_songs)

        playlistId = intent.getStringExtra("playlistId")!!
        playlistTitle = intent.getStringExtra("playlistTitle")!!
        findViewById<ImageButton>(R.id.back_button_song).setOnClickListener { onBackPressed() }
        findViewById<TextView>(R.id.playlist_title).text = playlistTitle
        recyclerView = findViewById(R.id.songs_list)
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        recyclerView.layoutManager = PreCachingLayoutManager(this)
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(applyDimension(COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()))
        songAdapter = SongAdapter(this)
        recyclerView.adapter = songAdapter
        songTouchHelper = ItemTouchHelper(SongTouchHelperCallback(songAdapter))
        songTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.doOnLayout {
            itemVisibleCount = it.measuredHeight / applyDimension(COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt()
            loadSongs()
        }
        findViewById<ImageButton>(R.id.delete_button).setOnClickListener { deleteSelected(songAdapter) }
        findViewById<ImageButton>(R.id.copy_button).setOnClickListener { copySelected(songAdapter) }
        findViewById<ImageButton>(R.id.shuffle_button).setOnClickListener { shuffleSongs() }
        findViewById<FloatingActionButton>(R.id.done_button).setOnClickListener { updateSongs() }
    }

    private fun copySelected(songAdapter: SongAdapter) {
        val listView = ListView(this)
        val playlistsTitles = intent.getStringArrayExtra("playlistsTitles")!!.toMutableList()
        playlistsTitles.add(0, "Create new playlist...")
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, playlistsTitles)
        listView.overScrollMode = OVER_SCROLL_NEVER
        listView.scrollBarStyle = SCROLLBARS_OUTSIDE_INSET
        listView.divider = null
        listView.setPadding(dp2px(8, this), dp2px(11, this), dp2px(8, this), 0)
        val dialog = MaterialAlertDialogBuilder(this@SongActivity, R.style.AlertDialogTheme)
            .setTitle(if (songAdapter.songList.any { it.selected }) "Copy to" else "Append playlist to")
            .setView(listView)
            .setNegativeButton("Cancel", null)
            .show()
        listView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            dialog.dismiss()
            if (i == 0) {
                val dp = dp2px(8, this).toFloat()
                val textInputLayout = TextInputLayout(this)
                val editText = TextInputEditText(textInputLayout.context)
                editText.setTextColor(resources.getColor(R.color.white, null))
                editText.setHintTextColor(resources.getColor(R.color.hint, null))
                editText.addTextChangedListener { textInputLayout.isErrorEnabled = false }
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                textInputLayout.setBoxCornerRadii(dp, dp, dp, dp)
                textInputLayout.setBoxBackgroundColorResource(R.color.card_color)
                val states = arrayOf(intArrayOf(android.R.attr.state_enabled))
                val colors = intArrayOf(resources.getColor(R.color.error, null))
                val colorList = ColorStateList(states, colors)
                textInputLayout.boxStrokeErrorColor = colorList
                textInputLayout.setErrorIconTintList(colorList)
                textInputLayout.setPadding(dp2px(24, this))
                textInputLayout.setErrorTextColor(colorList)
                editText.setPadding(dp2px(16, this))
                textInputLayout.addView(editText)
                editText.hint = "Playlist name"
                val dialog2 = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                    .setTitle("Create playlist")
                    .setView(textInputLayout)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Create", null)
                    .show()
                editText.onFocusChangeListener = View.OnFocusChangeListener { _, _ ->
                    dialog2.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
                dialog2.getButton(BUTTON_POSITIVE).setOnClickListener {
                    val title = editText.text.toString().trim()
                    if (title.isNotEmpty()) {
                        Thread { addSongs2Playlist(songAdapter.songList.filter { it.selected } as ArrayList<Song>, createPlaylist(title)) }.start()
                        selectable = false
                        movePlaylistTitle()
                        songAdapter.notifyItemRangeChanged(0, songAdapter.itemCount)
                        dialog2.dismiss()
                    } else
                        textInputLayout.error = "Playlist name can't be empty!"
                }
                editText.requestFocus()
            } else {
                Thread {
                    if (songAdapter.songList.any { it.selected })
                        addSongs2Playlist(songAdapter.songList.filter { it.selected } as ArrayList<Song>,
                                          (intent.getStringArrayExtra("playlistsIds") as Array<String>)[i - 1])
                    else
                        addSongs2Playlist(songAdapter.songList, (intent.getStringArrayExtra("playlistsIds") as Array<String>)[i - 1])
                }.start()
                selectable = false
                movePlaylistTitle()
                songAdapter.notifyItemRangeChanged(0, songAdapter.itemCount)
            }
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

    private fun getUserId(): String {
        val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url("https://api.spotify.com/v1/me").build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.isSuccessful) JSONObject(response.body!!.string()).getString("id") else ""
    }

    private fun addSongs2Playlist(songs: ArrayList<Song>, playlistId: String) {
        var offset = 0
        while (offset < songs.size) {
            val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
            val sublist = ArrayList<String>()
            for (i in offset until (offset + 100).coerceAtMost(songs.size))
                sublist.add(songs[i].uri)
            val body = JSONObject().put("uris", JSONArray(sublist)).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            if (OkHttpClient().newCall(Request.Builder().url(url).addHeader("Authorization", "Bearer " + getToken(this)).post(body).build()).execute().isSuccessful)
                runOnUiThread { showToast("Successfully added", this) }
            else
                runOnUiThread { showToast("Error occurred", this) }
            offset += 100
        }
    }

    private fun shuffleSongs() {
        if (semaphore.availablePermits() == 1)
            Thread {
                semaphore.acquire()
                shuffle(songAdapter.songList)
                preloadFirst(itemVisibleCount.coerceAtMost(songAdapter.itemCount), songAdapter.songList)
                runOnUiThread {
                    recyclerView.scrollToPosition(0)
                    songAdapter.notifyDataSetChanged()
                }
                semaphore.release()
            }.start()
    }

    private fun updateSongs() {
        selectable = false
        movePlaylistTitle()
        for (i in songAdapter.songList.indices)
            songAdapter.notifyItemChanged(i)
        Thread {
            var offset = 0
            while (offset < songAdapter.songList.size) {
                val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"
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
        with(songAdapter) {
            if (songList.stream().allMatch { it.selected } || songList.stream().noneMatch { it.selected }) {
                MaterialAlertDialogBuilder(this@SongActivity, R.style.AlertDialogTheme)
                    .setTitle("Delete $playlistTitle?")
                    .setMessage("")
                    .setPositiveButton("Delete") { _, _ -> deletePlaylist() }
                    .setNegativeButton("Cancel") { _, _ -> notifyItemRangeChanged(0, itemCount) }
                    .show()
            } else {
                var i = 0
                while (i < songList.size) {
                    if (songList[i].selected && songList.size > 1) {
                        songList.removeAt(i)
                        notifyItemRemoved(i)
                    } else {
                        notifyItemChanged(i)
                        i++
                    }
                }
            }
        }
    }

    private fun deletePlaylist() {
        Thread {
            val url = "https://api.spotify.com/v1/playlists/$playlistId/followers"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getToken(this))
                .delete()
            val response = OkHttpClient().newCall(request.build()).execute()
            runOnUiThread {
                if (response.isSuccessful)
                    showToast("Successfully deleted", this)
                else
                    showToast("Error occurred", this)
                selectable = false
                onBackPressed()
            }
        }.start()
    }

    override fun onBackPressed() {
        if (selectable) {
            selectable = false
            movePlaylistTitle()
            songAdapter.notifyItemRangeChanged(0, songAdapter.itemCount)
        } else
            super.onBackPressed()
    }

    private fun loadSongs() {
        Thread {
            var x = 0
            var url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?market=from_token"
            do {
                val songs = ArrayList<Song>()
                val request = Request.Builder().addHeader("Authorization", "Bearer " + getToken(this)).url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    val obj = JSONObject(response.body!!.string())
                    url = obj.getString("next")
                    val items = obj.getJSONArray("items")
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
                                songs.add(Song(track.getString("uri"),
                                               track.getString("name"),
                                               artist,
                                               image))
                                if (++x == itemVisibleCount || (i + 1 == items.length() && x < itemVisibleCount))
                                    preloadFirst(x, songs)
                            }
                        }
                    songAdapter.songList.addAll(songs)
                    runOnUiThread { songAdapter.notifyItemRangeInserted(songAdapter.itemCount - songs.size, songs.size) }
                }
            } while (url != "null")
        }.start()
    }

    private fun preloadFirst(x: Int, songs: ArrayList<Song>) {
        val semaphore = Semaphore(1 - x)
        for (i in 0 until x)
            Glide.with(this).load(songs[i].imageUrl).diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
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

    private fun movePlaylistTitle() {
        val playlistTitle = findViewById<TextView>(R.id.playlist_title)
        val playlistTitleParams = playlistTitle.layoutParams as ConstraintLayout.LayoutParams
        playlistTitleParams.marginEnd = dp2px(55, this)
        playlistTitle.layoutParams = playlistTitleParams
    }
}