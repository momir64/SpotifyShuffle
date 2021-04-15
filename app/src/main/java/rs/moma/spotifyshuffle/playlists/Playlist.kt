package rs.moma.spotifyshuffle.playlists

data class Playlist(
    var id: String,
    var title: String,
    var imageUrl: String,
    var selected: Boolean = false,
    var num: Int = -1
)