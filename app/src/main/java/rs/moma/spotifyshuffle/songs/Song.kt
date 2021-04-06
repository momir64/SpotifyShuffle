package rs.moma.spotifyshuffle.songs

data class Song(
    var num: Int,
    var uri: String,
    var title: String,
    var artist: String,
    var imageUrl: String,
    var selected: Boolean = false
)