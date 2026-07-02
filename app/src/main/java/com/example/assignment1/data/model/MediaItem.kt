package com.example.assignment1.data.model

data class MediaItem(
    val type: MediaType,
    val resourceId: Int = 0,
    val url: String = "",
    val caption: String = "",
    // true = 图片有实色背景, crop填满frame
    // false = 透明插图, fit居中不拉伸
    val fillFrame: Boolean = false
)

enum class MediaType { IMAGE, VIDEO }
