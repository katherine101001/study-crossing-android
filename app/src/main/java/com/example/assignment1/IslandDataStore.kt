package com.example.assignment1

import android.content.Context
import com.example.assignment1.data.model.GameData
import com.example.assignment1.data.model.GameType
import com.example.assignment1.data.model.LearningContent
import com.example.assignment1.data.model.Lesson
import com.example.assignment1.data.model.McOption
import com.example.assignment1.data.model.MediaItem
import com.example.assignment1.data.model.MediaType
import com.example.assignment1.data.model.Quest
import com.example.assignment1.data.model.SessionRecord
import com.example.assignment1.data.model.Subject
import com.example.assignment1.data.model.TaskItem

// IslandDataStore - 学生这边的游戏内容 + 每个学生的进度/笔记
// quest pool现在从SQLite lessons表读出来(admin/teacher能改), 每次访问refresh一次
// 每个学生分开记的东西(完成记录/session/笔记)也在SQLite (DatabaseHelper)
// 这个object当facade: 各screen照样call IslandDataStore.xxx(), CRUD转去DatabaseHelper做
object IslandDataStore {

    // quest pool是从lessons表转出来的游戏内容, refreshQuestPool()每次重建
    private val questPool = ArrayList<Quest>()

    // database connection, app启动时MyApp call一次init()就有了
    private lateinit var db: DatabaseHelper

    // 现在登入的是哪个学生 (-1 = 还没登入)
    // login时set, 之后所有"当前学生"的method都看这个
    private var currentStudentId = -1

    // quest内容现在存在SQLite lessons表, 不在这里建 (init时db还没好)
    // 改成各accessor第一次用时call refreshQuestPool()从DB读

    // MyApp.onCreate里call, 把DatabaseHelper建起来
    fun init(context: Context) {
        db = DatabaseHelper(context.applicationContext)
    }

    // ---------- 登入session ----------
    // login时绑定当前学生, 之后所有"当前学生"的进度/笔记/资料都指向他

    fun setCurrentStudent(studentId: Int) {
        currentStudentId = studentId
    }

    fun getCurrentStudentId(): Int {
        return currentStudentId
    }

    // 从lessons表把全部quest重新读出来 (Manage Lessons改完, 这里马上反映)
    // 每个game type按存的字段还原成对应的GameData subclass
    private fun refreshQuestPool() {
        questPool.clear()
        val lessons = db.getAllLessons()
        for (i in lessons.indices) {
            questPool.add(lessonToQuest(lessons[i]))
        }
    }

    // 一条lesson转成游戏用的Quest
    private fun lessonToQuest(lesson: Lesson): Quest {
        // phase1配图: 要showLessonImage且有图才放 (化石入门题show=false, 那张图只当挖出来用)
        val media = ArrayList<MediaItem>()
        if (lesson.showLessonImage && (lesson.imageResId != 0 || lesson.imageUri.isNotEmpty())) {
            media.add(
                MediaItem(
                    MediaType.IMAGE,
                    resourceId = lesson.imageResId,
                    url = lesson.imageUri,
                    fillFrame = lesson.fillFrame
                )
            )
        }

        val learning = LearningContent(
            title = lesson.title,
            subject = lesson.subject,
            npcName = lesson.npcName,
            npcDialogue = lesson.npcDialogue,
            lessonText = lesson.lessonText,
            mediaItems = media
        )

        return Quest(
            id = lesson.id,
            isDone = false,
            learning = learning,
            game = buildGameData(lesson)
        )
    }

    // 按game_type把题目/答案/选项还原成对应玩法的GameData
    private fun buildGameData(lesson: Lesson): GameData {
        val opts = splitOptions(lesson.options)
        if (lesson.gameType == GameType.SLOT_MACHINE) {
            return GameData.SlotMachineData(
                targetNumber = lesson.answer,
                questionText = lesson.questionText
            )
        } else if (lesson.gameType == GameType.WORD_TRACK) {
            return GameData.WordTrackData(
                sentence = lesson.questionText,
                correctWord = lesson.answer,
                options = opts
            )
        } else if (lesson.gameType == GameType.FOSSIL_EXCAVATION) {
            return GameData.FossilExcavationData(
                correctTag = lesson.answer,
                options = opts,
                fossilImageRes = lesson.imageResId,
                hintText = lesson.questionText,
                dirtTextureRes = R.drawable.soil
            )
        } else {
            // MULTIPLE_CHOICE: 选项配字母A/B/C..., 哪个文字跟answer一样就是correctIndex
            val letters = arrayOf("A", "B", "C", "D", "E", "F")
            val mcOptions = ArrayList<McOption>()
            var correctIndex = 0
            for (j in opts.indices) {
                var letter = "?"
                if (j < letters.size) {
                    letter = letters[j]
                }
                mcOptions.add(McOption(letter, opts[j]))
                if (opts[j] == lesson.answer) {
                    correctIndex = j
                }
            }
            return GameData.MultipleChoiceData(
                prompt = lesson.questionText,
                options = mcOptions,
                correctIndex = correctIndex
            )
        }
    }

    // 逗号分隔的options串拆成list, 顺手trim掉空格, 空串就空list
    private fun splitOptions(raw: String): List<String> {
        val list = ArrayList<String>()
        if (raw.isEmpty()) {
            return list
        }
        val parts = raw.split(",")
        for (i in parts.indices) {
            val t = parts[i].trim()
            if (t.isNotEmpty()) {
                list.add(t)
            }
        }
        return list
    }

    // questPool里的Quest isDone永远是false, 真正的done状态按学生分开存在SQLite progress表
    // 这个function配合传进来的完成集合, 返回isDone正确的Quest copy
    private fun applyDoneState(q: Quest, doneSet: HashSet<Int>): Quest {
        if (doneSet.contains(q.id)) {
            return q.copy(isDone = true)
        } else {
            return q.copy(isDone = false)
        }
    }

    // 根据id查quest, StudyActivity/TopFragment/BottomFragment都用这个
    fun getQuestion(id: Int): Quest? {
        refreshQuestPool()
        val doneSet = db.getCompletedIds(currentStudentId)
        // 用loop不用.find {}, 好读
        for (i in questPool.indices) {
            if (questPool[i].id == id) {
                return applyDoneState(questPool[i], doneSet)
            }
        }
        return null
    }

    // 返回所有quest (Badges screen和buildTaskList用)
    fun getAllQuests(): ArrayList<Quest> {
        refreshQuestPool()
        val doneSet = db.getCompletedIds(currentStudentId)
        val result = ArrayList<Quest>()
        for (i in questPool.indices) {
            result.add(applyDoneState(questPool[i], doneSet))
        }
        return result
    }

    // 根据subject筛选quest, MainActivity点建筑物时用
    fun getQuestsBySubject(subject: Subject): ArrayList<Quest> {
        refreshQuestPool()
        val doneSet = db.getCompletedIds(currentStudentId)
        val result = ArrayList<Quest>()
        for (i in questPool.indices) {
            val q = questPool[i]
            if (q.learning.subject == subject) {
                result.add(applyDoneState(q, doneSet))
            }
        }
        return result
    }

    // Quest Archive用的list, 把Quest转成轻量的TaskItem给RecyclerView
    fun buildTaskList(): ArrayList<TaskItem> {
        refreshQuestPool()
        val doneSet = db.getCompletedIds(currentStudentId)
        val list = ArrayList<TaskItem>()
        for (i in questPool.indices) {
            val q = applyDoneState(questPool[i], doneSet)
            val item = TaskItem(q.id, q.learning.title, q.learning.subject, q.isDone)
            list.add(item)
        }
        return list
    }

    // 标记一个quest为当前学生已完成 (RewardActivity答对了会call)
    fun markDone(id: Int) {
        db.markDone(currentStudentId, id)
    }

    // 某个学生完成了几个quest (teacher看进度用, 可以查任意学生)
    fun getCompletedCount(studentId: Int): Int {
        return db.getCompletedCount(studentId)
    }

    // quest总数 (进度条的分母)
    fun getTotalQuests(): Int {
        refreshQuestPool()
        return questPool.size
    }

    // RewardActivity完成后调用, Notice Board就能显示新卡片了
    // 同时把赚到的coins加到当前学生的余额
    fun recordSession(subjectStr: String, grade: String, coins: Int) {
        val subject = Subject.fromString(subjectStr)

        var title = "Made a discovery"
        if (subject == Subject.MATH) {
            title = "Solved a math problem"
        } else if (subject == Subject.ENGLISH) {
            title = "Completed an English exercise"
        }
        // GK就用default的 "Made a discovery"

        // 记在当前学生名下, getSessions按id DESC读回来最新的就在最前面
        db.insertSession(currentStudentId, subject, grade, coins, title)
        // 赚的coins直接加到学生的bell余额, admin/teacher看到的就是更新后的数字
        AdminDataStore.addCoins(currentStudentId, coins)
    }

    // 返回当前学生的session记录 (最新在前)
    fun getSessionHistory(): ArrayList<SessionRecord> {
        return db.getSessions(currentStudentId)
    }

    // 当前学生的bell余额 (Main/Badges显示用)
    // 余额直接拿Student记录的coins: admin给的初始值 + 玩游戏赚的都算在里面
    fun getTotalCoins(): Int {
        val student = AdminDataStore.getStudent(currentStudentId)
        if (student == null) {
            return 0
        }
        return student.coins
    }

    // ---------- 当前学生自己的资料 (名字 + avatar) ----------
    // 现在直接读写AdminDataStore里那条Student记录
    // 所以学生改名字/avatar, admin和teacher那边的list马上同步

    fun setPlayer(name: String, avatar: String) {
        AdminDataStore.updateStudentProfile(currentStudentId, name, avatar)
    }

    fun getPlayerName(): String {
        val student = AdminDataStore.getStudent(currentStudentId)
        if (student == null) {
            return "Islander"
        }
        return student.name
    }

    fun getPlayerAvatar(): String {
        val student = AdminDataStore.getStudent(currentStudentId)
        if (student == null) {
            return "Cat"
        }
        return student.avatar
    }

    // ---------- 当前学生lesson笔记的CRUD ----------
    // 笔记按studentId分开存在SQLite notes表, 每个学生只看到自己的

    // 拿某个lesson的笔记, 没有就回空字符串
    fun getNote(id: Int): String {
        return db.getNote(currentStudentId, id)
    }

    // 这个lesson有没有写过笔记 (Quest Archive按钮文字要用)
    fun hasNote(id: Int): Boolean {
        val n = db.getNote(currentStudentId, id)
        return n.isNotEmpty()
    }

    // 存/更新笔记, 空的就当删掉
    fun setNote(id: Int, text: String) {
        db.setNote(currentStudentId, id, text)
    }

    // 删掉某个lesson的笔记
    fun deleteNote(id: Int) {
        db.deleteNote(currentStudentId, id)
    }

    // Settings > Reset Progress: 只清当前学生的进度(完成记录/session/笔记)
    // coins余额不动, 那是admin管的资料
    fun resetAll() {
        db.clearProgress(currentStudentId)
        db.clearSessions(currentStudentId)
        db.clearNotes(currentStudentId)
    }
}
