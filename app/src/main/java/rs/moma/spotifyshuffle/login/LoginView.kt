package rs.moma.spotifyshuffle.login

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import rs.moma.spotifyshuffle.R
import rs.moma.spotifyshuffle.global.*
import rs.moma.spotifyshuffle.playlists.PlaylistActivity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class LoginView : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val scopes = "playlist-modify-public playlist-modify-private playlist-read-private playlist-read-collaborative user-library-modify user-library-read"
        webView = findViewById(R.id.webview)
        var code: String?
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                if (uri.scheme != "spotifyshuffle")
                    return false
                code = uri.getQueryParameter("code")
                if (code == null) {
                    uri.getQueryParameter("error")?.let {
                        if (it != "access_denied")
                            showToast(it, this@LoginView)
                    }
                    start(LoginActivity::class.java)
                } else
                    sendPostRequest(code!!, codeVerifier)
                return true
            }
        }
        webView.loadUrl("https://accounts.spotify.com/en/authorize" +
                                "?client_id=" + CLIENT_ID +
                                "&response_type=code" +
                                "&redirect_uri=" + encodeURI(REDIRECT_URI) +
                                "&code_challenge_method=S256" +
                                "&code_challenge=" + codeChallenge +
                                "&scope=" + encodeURI(scopes))
    }

    fun sendPostRequest(code: String, codeVerifier: String) {
        Thread {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", codeVerifier)
                .build()
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .build()
            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                val obj = JSONObject(response.body!!.string())
                setAcToken(obj.getString("access_token"), this)
                setReToken(obj.getString("refresh_token"), this)
                setExpireTime(System.currentTimeMillis() + obj.getLong("expires_in") * 1000, this)
                start(PlaylistActivity::class.java)
            }
        }.start()
    }

    private fun generateCodeVerifier(): String {
        val codeVerifier = ByteArray(64)
        SecureRandom().nextBytes(codeVerifier)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(charset("US-ASCII"))
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(messageDigest.digest())
    }

    override fun onBackPressed() {
        if (webView.canGoBack())
            webView.goBack()
        else
            start(LoginActivity::class.java)
    }

    private fun start(context: Class<*>) {
        startActivity(Intent(this, context))
        overridePendingTransition(R.anim.outmid, R.anim.midin)
        finish()
    }
}