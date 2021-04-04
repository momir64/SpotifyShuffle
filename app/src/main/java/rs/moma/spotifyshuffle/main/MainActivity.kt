package rs.moma.spotifyshuffle.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.getReToken
import rs.moma.spotifyshuffle.login.LoginActivity
import rs.moma.spotifyshuffle.playlists.PlaylistActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, if (getReToken(this) == null) LoginActivity::class.java else PlaylistActivity::class.java))
        overridePendingTransition(R.anim.hold, R.anim.hold)
        finish()
    }
}