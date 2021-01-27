package com.odukle.ytnotes

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

fun getVideoId(url: String): String {
    val startIndex1 = url.indexOf("?v=")
    val startIndex2 = url.indexOf(".be/")
    val endIndex = url.indexOf("?t=")
    if (startIndex1 < 0) {
        if (startIndex2 > 0) {
            if (endIndex < 0) {
                return url.substring(startIndex2 + 4, url.length)
            }
            return url.substring(startIndex2 + 4, endIndex)
        } else {
            return "0"
        }
    }
    if (endIndex < 0) {
        return url.substring(startIndex1 + 3, url.length)
    }
    return url.substring(startIndex1 + 3, endIndex)
}

@Parcelize
data class Note(
    var title: List<String>? = listOf(),
    var titleNoCase: List<String>? = listOf(),
    var timestamp: String? = "",
    var note: List<String>? = listOf(),
    var noteNoCase: MutableList<String>? = mutableListOf(),
    var dateAdded: String? = "0",
    var dateAddedLong: Long? = 0L,
    var url: String? = "",
    var id: String = "0"
) : Parcelable