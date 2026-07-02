package com.example.assignment1.data.model

// Subject - app里的三个学科
// displayName显示在UI上, shortName显示在toolbar
enum class Subject(val displayName: String, val shortName: String) {
    MATH("Math", "MATH QUEST"),
    ENGLISH("English", "ENGLISH QUEST"),
    GK("General Knowledge", "MUSEUM QUEST");

    companion object {
        // Activity之间通过Intent string传subject, 所以要转回enum
        // 不认识的string就默认GK
        fun fromString(s: String): Subject {
            if (s == "Math") {
                return MATH
            } else if (s == "English") {
                return ENGLISH
            } else {
                return GK
            }
        }
    }
}
