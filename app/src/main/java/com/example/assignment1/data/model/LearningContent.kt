package com.example.assignment1.data.model

data class LearningContent(
    val title: String,
    val subject: Subject,
    val npcName: String,
    val npcDialogue: String,
    val lessonText: String,
    val mediaItems: List<MediaItem> = emptyList()
)
