package rs.moma.spotifyshuffle.playlists

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.dp2px
import rs.moma.spotifyshuffle.global.getToken
import rs.moma.spotifyshuffle.global.showToast
import rs.moma.spotifyshuffle.songs.SongActivity

var selectable = false

class PlaylistAdapter(val playlistList: ArrayList<Playlist>, private val activity: PlaylistActivity) : RecyclerView.Adapter<PlaylistViewHolder>() {
    override fun getItemCount() = playlistList.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bindData(playlistList[position], activity, this)
    }
}

class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var playlistTitle = itemView.findViewById<TextView>(R.id.playlist_text)
    private var playlistImage = itemView.findViewById<ImageView>(R.id.playlist_image)
    private var playlistCard = itemView.findViewById<MaterialCardView>(R.id.playlist_card)

    fun bindData(playlist: Playlist, activity: PlaylistActivity, playlistAdapter: PlaylistAdapter) {
        val logoutButton = activity.findViewById<ImageButton>(R.id.logout_button)
        val backButton = activity.findViewById<ImageButton>(R.id.back_button_playlist)
        val addButton = activity.findViewById<ImageButton>(R.id.playlist_add_button)
        val checkButton = activity.findViewById<ImageButton>(R.id.playlist_check_button)
        val arrow = itemView.findViewById<ImageView>(R.id.imageView)
        if (selectable) {
            backButton.visibility = View.VISIBLE
            logoutButton.visibility = View.INVISIBLE
            checkButton.visibility = View.VISIBLE
            addButton.visibility = View.INVISIBLE
            arrow.visibility = View.INVISIBLE
            activity.swipeContainer.isRefreshing = false
            activity.swipeContainer.isEnabled = false
        } else {
            for (i in playlistAdapter.playlistList.indices) {
                playlistAdapter.playlistList[i].num = i
                playlistAdapter.playlistList[i].selected = false
            }
            backButton.visibility = View.INVISIBLE
            logoutButton.visibility = View.VISIBLE
            checkButton.visibility = View.INVISIBLE
            addButton.visibility = View.VISIBLE
            arrow.visibility = View.VISIBLE
            activity.swipeContainer.isEnabled = true
        }
        playlistCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (playlist.selected) R.color.card_color_selected else R.color.card_color))
        playlistCard.strokeWidth = if (playlist.selected) dp2px(1.5, activity) else 0
        Glide.with(activity).load(playlist.imageUrl).placeholder(R.drawable.ic_placeholder).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).dontTransform().into(playlistImage)
        playlistTitle.text = playlist.title
        playlistCard.setOnClickListener {
            if (selectable) {
                playlist.selected = !playlist.selected
                playlistCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (playlist.selected) R.color.card_color_selected else R.color.card_color))
                playlistCard.strokeWidth = if (playlist.selected) dp2px(1.5, activity) else 0
            } else {
                activity.startActivity(Intent(activity, SongActivity::class.java)
                                           .putExtra("playlistTitle", playlist.title)
                                           .putExtra("playlistId", playlist.id)
                                           .putExtra("playlistsTitles", playlistAdapter.playlistList.map { it.title }.toTypedArray())
                                           .putExtra("playlistsIds", playlistAdapter.playlistList.map { it.id }.toTypedArray()))
            }
        }
        playlistCard.setOnLongClickListener {
            if (selectable) {
                playlist.selected = !playlist.selected
                playlistCard.setCardBackgroundColor(ContextCompat.getColor(activity, if (playlist.selected) R.color.card_color_selected else R.color.card_color))
                playlistCard.strokeWidth = if (playlist.selected) dp2px(1.5, activity) else 0
            } else {
                val dp = dp2px(8, activity).toFloat()
                val textInputLayout = TextInputLayout(activity)
                val editText = TextInputEditText(textInputLayout.context)
                editText.setTextColor(activity.resources.getColor(R.color.white, null))
                editText.setHintTextColor(activity.resources.getColor(R.color.hint, null))
                editText.addTextChangedListener { textInputLayout.isErrorEnabled = false }
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                textInputLayout.setBoxCornerRadii(dp, dp, dp, dp)
                textInputLayout.setBoxBackgroundColorResource(R.color.card_color)
                val colors = intArrayOf(activity.resources.getColor(R.color.error, null))
                val states = arrayOf(intArrayOf(android.R.attr.state_enabled))
                val colorList = ColorStateList(states, colors)
                textInputLayout.boxStrokeErrorColor = colorList
                textInputLayout.setErrorIconTintList(colorList)
                textInputLayout.setPadding(dp2px(24, activity))
                textInputLayout.setErrorTextColor(colorList)
                editText.setPadding(dp2px(16, activity))
                textInputLayout.addView(editText)
                editText.setText(playlist.title)
                editText.hint = "Playlist name"
                val dialog = MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme)
                    .setTitle("Edit playlist")
                    .setView(textInputLayout)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Apply", null)
                    .setNeutralButton("Delete") { dialog, _ ->
                        dialog.dismiss()
                        MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme)
                            .setTitle("Delete " + playlist.title + "?")
                            .setMessage("")
                            .setPositiveButton("Delete") { _, _ -> deletePlaylist(playlist, activity, playlistAdapter) }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    .show()
                editText.onFocusChangeListener = OnFocusChangeListener { _, _ ->
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val newTitle = editText.text.toString().trim()
                    if (newTitle.isNotEmpty()) {
                        if (newTitle != playlist.title)
                            changeTitle(newTitle, playlist, activity, playlistAdapter)
                        dialog.dismiss()
                    } else
                        textInputLayout.error = "Playlist name can't be empty!"
                }
                editText.requestFocus()
            }
            return@setOnLongClickListener true
        }
    }

    private fun deletePlaylist(playlist: Playlist, activity: PlaylistActivity, playlistAdapter: PlaylistAdapter) {
        playlistAdapter.playlistList.removeAt(playlist.num)
        playlistAdapter.notifyItemRemoved(playlist.num)
        Thread {
            val url = "https://api.spotify.com/v1/playlists/${playlist.id}/followers"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getToken(activity))
                .delete()
            val response = OkHttpClient().newCall(request.build()).execute()
            activity.runOnUiThread {
                if (response.isSuccessful)
                    showToast("Successfully deleted", activity)
                else
                    showToast("Error occurred", activity)
            }
        }.start()
    }

    private fun changeTitle(title: String, playlist: Playlist, activity: PlaylistActivity, playlistAdapter: PlaylistAdapter) {
        playlist.title = title
        playlistAdapter.notifyItemChanged(playlist.num)
        Thread {
            val url = "https://api.spotify.com/v1/playlists/${playlist.id}"
            val body = JSONObject().put("name", title).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getToken(activity))
                .put(body)
            val response = OkHttpClient().newCall(request.build()).execute()
            activity.runOnUiThread {
                if (response.isSuccessful)
                    showToast("Successfully updated", activity)
                else
                    showToast("Error occurred", activity)
            }
        }.start()
    }
}