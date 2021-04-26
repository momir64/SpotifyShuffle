package rs.moma.spotifyshuffle.global

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.util.TypedValue.applyDimension
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

const val preferencePath = "com.example.spotifyshuffle.my_variables"
const val CLIENT_ID = "8edbcacd9213442a99c413fc606e057a"
const val REDIRECT_URI = "spotifyshuffle://callback"

class PreCachingLayoutManager(private val context: Context) : LinearLayoutManager(context) {
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int = dp2px(80, context) * 10
}

class VerticalSpaceItemDecoration(private val mVerticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) == 0)
            outRect.top = mVerticalSpaceHeight
    }
}

fun dp2px(dp: Int, context: Context): Int {
    return applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
}

fun dp2px(dp: Double, context: Context): Int {
    return applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
}

fun getToken(context: Context): String {
    if (System.currentTimeMillis() >= getExpireTime(context)) {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", getReToken(context)!!)
            .add("client_id", CLIENT_ID)
            .build()
        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(body)
            .build()
        val response = OkHttpClient().newCall(request).execute()
        if (response.isSuccessful) {
            val obj = JSONObject(response.body!!.string())
            setAcToken(obj.getString("access_token"), context)
            setReToken(obj.getString("refresh_token"), context)
            setExpireTime(System.currentTimeMillis() + obj.getLong("expires_in") * 1000, context)
        }
    }
    return getAcToken(context)!!
}

fun getAcToken(context: Context): String? {
    return context.getSharedPreferences(preferencePath, AppCompatActivity.MODE_PRIVATE).getString("acToken", null)
}

fun setAcToken(acToken: String, context: Context) {
    with(context.getSharedPreferences(preferencePath, AppCompatActivity.MODE_PRIVATE).edit()) {
        putString("acToken", acToken)
        apply()
    }
}

fun getReToken(context: Context): String? {
    return context.getSharedPreferences(preferencePath, AppCompatActivity.MODE_PRIVATE).getString("reToken", null)
}

fun setReToken(reToken: String, context: Context) {
    with(context.getSharedPreferences(preferencePath, AppCompatActivity.MODE_PRIVATE).edit()) {
        putString("reToken", reToken)
        apply()
    }
}

fun getExpireTime(context: Context): Long {
    return context.getSharedPreferences(preferencePath, AppCompatActivity.MODE_PRIVATE).getLong("expireTime", 0)
}

fun setExpireTime(expireTime: Long, context: Context) {
    with(context.getSharedPreferences(preferencePath, AppCompatActivity.MODE_PRIVATE).edit()) {
        putLong("expireTime", expireTime)
        apply()
    }
}

var currentToast: Toast? = null
fun showToast(text: String, context: Context) {
    currentToast?.cancel()
    currentToast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
    currentToast?.show()
}

fun encodeURI(s: String): String = android.net.Uri.encode(s)