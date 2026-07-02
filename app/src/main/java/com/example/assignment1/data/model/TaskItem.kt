package com.example.assignment1.data.model

data class TaskItem(
    val id: Int,
    val title: String,
    val subject: Subject,
    val isDone: Boolean
)
