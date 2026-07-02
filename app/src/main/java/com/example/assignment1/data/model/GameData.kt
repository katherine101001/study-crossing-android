package com.example.assignment1.data.model

sealed class GameData {
    abstract val gameType: GameType
    abstract val instructions: String

    data class MultipleChoiceData(
        val prompt: String,
        val options: List<McOption>,
        val correctIndex: Int,
        override val instructions: String = "Pick the correct answer!"
    ) : GameData() {
        override val gameType = GameType.MULTIPLE_CHOICE
    }

    data class SlotMachineData(
        val targetNumber: String,
        // Phase 2显示的算式, e.g. "40 + 5 = ?". 只是展示, 不参与逻辑
        val questionText: String = "",
        override val instructions: String = "Spin the reels to match the code!"
    ) : GameData() {
        override val gameType = GameType.SLOT_MACHINE
    }

    data class WordTrackData(
        val sentence: String,
        val correctWord: String,
        val options: List<String>,
        override val instructions: String = "Drag the correct word into the blank!"
    ) : GameData() {
        override val gameType = GameType.WORD_TRACK
    }

    data class FossilExcavationData(
        val correctTag: String,
        val options: List<String>,
        val fossilImageRes: Int,
        val hintText: String = "",
        val dirtTextureRes: Int = 0,
        override val instructions: String = "Scratch the dirt and identify the fossil!"
    ) : GameData() {
        override val gameType = GameType.FOSSIL_EXCAVATION
    }
}

data class McOption(val letter: String, val text: String)
