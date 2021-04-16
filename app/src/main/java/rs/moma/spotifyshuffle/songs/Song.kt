package rs.moma.spotifyshuffle.songs

data class Song(
    var uri: String,
    var title: String,
    var artist: String,
    var imageUrl: String,
    var duration: Int = -1,
    var selected: Boolean = false,
    var num: Int = -1
)