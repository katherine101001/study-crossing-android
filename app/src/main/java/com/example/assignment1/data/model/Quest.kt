package com.example.assignment1.data.model

data class Quest(
    val id: Int,
    val isDone: Boolean,
    val learning: LearningContent,
    val game: GameData
)
