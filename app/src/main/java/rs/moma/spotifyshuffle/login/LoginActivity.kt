package rs.moma.spotifyshuffle.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import rs.moma.spotifyshuffle.R

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.login_button).setOnClickListener(this)
        findViewById<TextView>(R.id.connect_label).text = HtmlCompat.fromHtml(resources.getString(R.string.connect_string), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    override fun onClick(view: View?) {
        startActivity(Intent(this, LoginView::class.java))
        overridePendingTransition(R.anim.inmid, R.anim.midout)
        finish()
    }
}