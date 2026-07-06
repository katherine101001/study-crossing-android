package com.example.assignment1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignment1.data.model.GameType
import com.example.assignment1.data.model.Lesson
import com.example.assignment1.data.model.SessionRecord
import com.example.assignment1.data.model.Staff
import com.example.assignment1.data.model.Student
import com.example.assignment1.data.model.Subject

// DatabaseHelper - 全app的SQLite数据库, 以前的in-memory数据全部搬来这里
// 6张表: 学生 students / 班级 classes / 学习资料 lessons / 完成记录 progress / session记录 / 笔记 notes
// companion放常量, SQL keyword大写, 每个function本地拿connection, 手动close cursor
class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "StudyCrossing.db"
        private const val DATABASE_VERSION = 4

        // 学生表 (学生用户表, 学生登入就对这张)
        private const val TABLE_STUDENT = "students"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_FORM = "form"
        private const val COLUMN_COINS = "coins"
        private const val COLUMN_AVATAR = "avatar"

        // 职员表 (admin / teacher的账号, 登入对这张, 复用username/password)
        private const val TABLE_STAFF = "staff"
        private const val COLUMN_ROLE = "role"

        // 班级表
        private const val TABLE_CLASS = "classes"
        private const val COLUMN_CLASS_NAME = "name"

        // 学习资料表
        private const val TABLE_LESSON = "lessons"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_SUBJECT = "subject"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_LESSON_TEXT = "lesson_text"
        private const val COLUMN_NPC_NAME = "npc_name"
        private const val COLUMN_NPC_DIALOGUE = "npc_dialogue"
        private const val COLUMN_IMAGE_RES_ID = "image_res_id"
        private const val COLUMN_IMAGE_URI = "image_uri"
        private const val COLUMN_QUESTION_TEXT = "question_text"
        private const val COLUMN_ANSWER = "answer"
        private const val COLUMN_OPTIONS = "options"
        // 统一quest后加的: 玩法类型 + phase1配图的两个标记
        private const val COLUMN_GAME_TYPE = "game_type"
        private const val COLUMN_FILL_FRAME = "fill_frame"
        private const val COLUMN_SHOW_IMAGE = "show_lesson_image"

        // 完成记录表 (哪个学生做完哪个quest)
        private const val TABLE_PROGRESS = "progress"
        private const val COLUMN_STUDENT_ID = "student_id"
        private const val COLUMN_QUEST_ID = "quest_id"

        // session记录表 (复用 subject / coins / title / student_id 那些名字)
        private const val TABLE_SESSION = "sessions"
        private const val COLUMN_GRADE = "grade"

        // 笔记表 (复用 student_id / quest_id)
        private const val TABLE_NOTE = "notes"
        private const val COLUMN_NOTE_TEXT = "note_text"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // 学生表
        val createStudent = ("CREATE TABLE $TABLE_STUDENT(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USERNAME TEXT, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_NAME TEXT, " +
                "$COLUMN_FORM TEXT, " +
                "$COLUMN_COINS INTEGER, " +
                "$COLUMN_AVATAR TEXT)")
        db?.execSQL(createStudent)

        // 职员表 (admin / teacher)
        val createStaff = ("CREATE TABLE $TABLE_STAFF(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USERNAME TEXT, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_ROLE TEXT)")
        db?.execSQL(createStaff)

        // 班级表
        val createClass = ("CREATE TABLE $TABLE_CLASS(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_CLASS_NAME TEXT)")
        db?.execSQL(createClass)

        // 学习资料表
        val createLesson = ("CREATE TABLE $TABLE_LESSON(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TITLE TEXT, " +
                "$COLUMN_SUBJECT TEXT, " +
                "$COLUMN_DESCRIPTION TEXT, " +
                "$COLUMN_LESSON_TEXT TEXT, " +
                "$COLUMN_NPC_NAME TEXT, " +
                "$COLUMN_NPC_DIALOGUE TEXT, " +
                "$COLUMN_IMAGE_RES_ID INTEGER, " +
                "$COLUMN_IMAGE_URI TEXT, " +
                "$COLUMN_QUESTION_TEXT TEXT, " +
                "$COLUMN_ANSWER TEXT, " +
                "$COLUMN_OPTIONS TEXT, " +
                "$COLUMN_GAME_TYPE TEXT, " +
                "$COLUMN_FILL_FRAME INTEGER, " +
                "$COLUMN_SHOW_IMAGE INTEGER)")
        db?.execSQL(createLesson)

        // 完成记录表
        val createProgress = ("CREATE TABLE $TABLE_PROGRESS(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_STUDENT_ID INTEGER, " +
                "$COLUMN_QUEST_ID INTEGER)")
        db?.execSQL(createProgress)

        // session记录表
        val createSession = ("CREATE TABLE $TABLE_SESSION(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_STUDENT_ID INTEGER, " +
                "$COLUMN_SUBJECT TEXT, " +
                "$COLUMN_GRADE TEXT, " +
                "$COLUMN_COINS INTEGER, " +
                "$COLUMN_TITLE TEXT)")
        db?.execSQL(createSession)

        // 笔记表
        val createNote = ("CREATE TABLE $TABLE_NOTE(" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_STUDENT_ID INTEGER, " +
                "$COLUMN_QUEST_ID INTEGER, " +
                "$COLUMN_NOTE_TEXT TEXT)")
        db?.execSQL(createNote)

        // 第一次建好database就塞demo数据进去
        seedStaff(db)
        seedStudents(db)
        seedClasses(db)
        seedLessons(db)
        seedProgress(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 升级版本就把旧表全删了重建
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_STUDENT")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_STAFF")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CLASS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LESSON")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PROGRESS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SESSION")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NOTE")
        onCreate(db)
    }

    // ---------- seed: 第一次建库时跑 ----------

    // admin + teacher的账号, 全部password "1234"
    // 登入选Admin / Teacher身份时就对这张表
    private fun seedStaff(db: SQLiteDatabase?) {
        val usernames = arrayOf("admin", "teacher")
        val roles = arrayOf("Admin", "Teacher")
        for (i in usernames.indices) {
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, usernames[i])
                put(COLUMN_PASSWORD, "1234")
                put(COLUMN_ROLE, roles[i])
            }
            db?.insert(TABLE_STAFF, null, values)
        }
    }

    // 7个demo学生, 全部password "1234", 可以直接登入
    // 第一个"student"是方便测试的简单账号, 用AUTOINCREMENT所以id是 1..7
    private fun seedStudents(db: SQLiteDatabase?) {
        val names = arrayOf(
            "Student", "Muhammad Ali", "Wei Keong", "Kelly Lim",
            "Daniel Ahmad", "Lily Wong", "Nurul Aina"
        )
        val usernames = arrayOf(
            "student", "muhammad", "weikeong", "kelly",
            "daniel", "lily", "nurul"
        )
        val forms = arrayOf(
            "Classroom A", "Classroom A", "Classroom A", "Classroom A",
            "Classroom B", "Classroom B", "Classroom B"
        )
        val coins = intArrayOf(500, 950, 720, 880, 1100, 640, 530)
        val avatars = arrayOf(
            "Cat", "Squirrel", "Dog", "Frog",
            "Bear", "Cat", "Squirrel"
        )

        for (i in names.indices) {
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, usernames[i])
                put(COLUMN_PASSWORD, "1234")
                put(COLUMN_NAME, names[i])
                put(COLUMN_FORM, forms[i])
                put(COLUMN_COINS, coins[i])
                put(COLUMN_AVATAR, avatars[i])
            }
            db?.insert(TABLE_STUDENT, null, values)
        }
    }

    // 两个班级, 跟学生的form对得上
    private fun seedClasses(db: SQLiteDatabase?) {
        val classes = arrayOf("Classroom A", "Classroom B")
        for (i in classes.indices) {
            val values = ContentValues().apply {
                put(COLUMN_CLASS_NAME, classes[i])
            }
            db?.insert(TABLE_CLASS, null, values)
        }
    }

    // 14条seed lesson = 游戏里的14个quest (统一后学生玩的就是这些)
    // Math 5 + English 5 + GK 4; game_type决定phase2玩法, seed顺序就是行id 1..14
    private fun seedLessons(db: SQLiteDatabase?) {
        // ===== MATH (5) =====
        insertSeedLesson(
            db, "Smart Shopping", Subject.MATH,
            "Work out a sale price after a percentage discount.",
            "To find a percentage of an amount,\nturn the percent into a decimal\nand multiply.\n\nExample:\n  20% of RM 50\n  = 0.20 × 50\n  = RM 10\n\nA discount is taken OFF the\noriginal price, so subtract it.",
            "Tom Nook", "Read the lesson, then pick the right price!",
            R.drawable.art_price_tag, "",
            "A RM 80 jacket is discounted by 25% in a sale. What is the sale price?",
            "RM 60", "RM 55, RM 60, RM 20, RM 75",
            GameType.MULTIPLE_CHOICE
        )
        insertSeedLesson(
            db, "Order of Operations", Subject.MATH,
            "Use BODMAS to solve a mixed expression.",
            "Always solve in this order (BODMAS):\n  Brackets first\n  Orders (powers)\n  Division and Multiplication\n  Addition and Subtraction\n\nExample:\n  2 + 3 × 4\n  = 2 + 12\n  = 14",
            "Tom Nook", "Follow the order, then dial in the answer!",
            R.drawable.art_brackets, "",
            "12 × (8 + 5) − 4 = ?",
            "152", "",
            GameType.SLOT_MACHINE
        )
        insertSeedLesson(
            db, "Keeping the Ratio", Subject.MATH,
            "Scale a recipe ratio up to find the missing amount.",
            "A ratio compares two amounts.\nTo scale it up, multiply BOTH\nparts by the same number.\n\nExample:\n  flour : sugar = 3 : 2\n  Use 6 units of flour (×2)\n  Then sugar = 2 × 2 = 4 units",
            "Tom Nook", "Scale it up, then pick the answer!",
            R.drawable.art_measuring_cup, "",
            "A recipe mixes flour and sugar in the ratio 3 : 2. If 600 g of flour is used, how much sugar is needed?",
            "400 g", "300 g, 400 g, 900 g, 200 g",
            GameType.MULTIPLE_CHOICE
        )
        insertSeedLesson(
            db, "Area of a Rectangle", Subject.MATH,
            "Multiply length by width to find the area.",
            "The area of a rectangle is:\n  Area = length × width\n\nThe answer is in square units\n(e.g. cm²).\n\nExample:\n  A 5 cm by 4 cm card\n  = 5 × 4\n  = 20 cm²",
            "Tom Nook", "Multiply the sides, then dial the area!",
            R.drawable.art_ruler_rectangle, "",
            "Area = 18 cm × 14 cm = ?",
            "252", "",
            GameType.SLOT_MACHINE
        )
        insertSeedLesson(
            db, "Solving for x", Subject.MATH,
            "Solve a simple linear equation for x.",
            "To solve an equation, do the\nsame thing to both sides until\nx is alone.\n\nExample:\n  2x + 3 = 11\n  2x = 11 − 3 = 8\n  x = 8 ÷ 2 = 4",
            "Tom Nook", "Balance the equation, then pick x!",
            R.drawable.art_algebra_x, "",
            "If 3x − 7 = 20, what is the value of x?",
            "9", "7, 8, 9, 4",
            GameType.MULTIPLE_CHOICE
        )

        // ===== ENGLISH (5) =====
        insertSeedLesson(
            db, "Phrasal Verbs", Subject.ENGLISH,
            "Pick the phrasal verb that means to postpone.",
            "A phrasal verb is a verb plus a\nsmall word that together change\nits meaning.\n\n\"Put off\" means to postpone or\ndelay something to a later time.\n\nExample:\n  We put off the meeting\n  until next week.",
            "Isabelle", "Read the lesson, then drag the right phrase!",
            R.drawable.art_calendar, "",
            "\"The class had to ___ the field\ntrip because of the heavy storm.\"",
            "put off", "put off, put on, put up, put away",
            GameType.WORD_TRACK
        )
        insertSeedLesson(
            db, "Subject-Verb Agreement", Subject.ENGLISH,
            "Match the verb to the nearest noun with neither/nor.",
            "With \"neither ... nor\", the verb\nagrees with the noun CLOSEST\nto it.\n\nExample:\n  Neither the cat nor the dogs\n  ARE in the house.\n  (closest noun = dogs, plural)",
            "Isabelle", "Match the verb, then pick the answer!",
            R.drawable.art_grammar_pencil, "",
            "Neither the teacher nor the students ________ aware of the schedule change.",
            "were", "was, were, is, has been",
            GameType.MULTIPLE_CHOICE
        )
        insertSeedLesson(
            db, "Stronger Vocabulary", Subject.ENGLISH,
            "Pick the precise word that means extremely important.",
            "Using precise words makes your\nwriting stronger.\n\n\"Vital\" means extremely important\nor necessary - much stronger than\njust saying \"important\".\n\nExample:\n  Sleep is vital for good health.",
            "Isabelle", "Read the lesson, then drag the best word!",
            R.drawable.art_water_drop, "",
            "\"Clean drinking water is a ___\nresource for every community.\"",
            "vital", "vital, tiny, dull, weak, random",
            GameType.WORD_TRACK
        )
        insertSeedLesson(
            db, "Past Perfect Tense", Subject.ENGLISH,
            "Use had + verb for the earlier of two past actions.",
            "Use the past perfect (had + verb)\nfor an action that happened\nBEFORE another past action.\n\nExample:\n  The film had started before\n  we reached the cinema.\n  (started first, reached after)",
            "Isabelle", "Pick the tense that came first!",
            R.drawable.art_train_clock, "",
            "By the time we arrived at the station, the train ________ already left.",
            "had", "has, had, have, was",
            GameType.MULTIPLE_CHOICE
        )
        insertSeedLesson(
            db, "Linking Words", Subject.ENGLISH,
            "Choose the linking word that shows a contrast.",
            "Linking words connect ideas.\n\n\"However\" shows a CONTRAST - it\nsignals that the next idea is\ndifferent or surprising.\n\nExample:\n  It was raining. However, we\n  still went out to play.",
            "Isabelle", "Read the lesson, then drag the right linker!",
            R.drawable.art_signpost, "",
            "\"She studied very hard for the\nexam. ___, she still felt nervous.\"",
            "However", "However, Therefore, Moreover, Because, Also",
            GameType.WORD_TRACK
        )

        // ===== GENERAL KNOWLEDGE (4) =====
        // 化石入门: phase1不显示图(showLessonImage=false), 那张图只当挖出来的化石用
        insertSeedLesson(
            db, "Identify Blathers' Fossil", Subject.GK,
            "Brush off the dirt and identify the fossil.",
            "Long ago, ancient creatures roamed\nthe Earth. When they died, their\nbones turned to stone over millions\nof years.\n\nScientists call these stone remains\nFOSSILS - they help us learn about\ndinosaurs and prehistoric life!\n\nBrush the dirt and identify the\nshape below.",
            "Blathers", "Read the field notes, then go dig!",
            R.drawable.ic_gk_fossil, "",
            "Brush away the dirt and look closely.",
            "Ancient Fossil", "Ancient Fossil, Living Plant, Space Rock, Sea Shell",
            GameType.FOSSIL_EXCAVATION, false, false
        )
        insertSeedLesson(
            db, "Sounds of Malaysia", Subject.GK,
            "Identify the traditional Malaysian instrument shown.",
            "Malaysia has rich traditional music.\nThe Kompang is a hand drum used\nin Malay weddings and ceremonies.\nThe Sape is a wooden lute from\nthe Orang Ulu in Sarawak.\nGamelan is an orchestral ensemble.\n\nLook at the picture carefully!",
            "Blathers", "Study the picture, then pick the right one!",
            R.drawable.ic_gk_instruments, "",
            "Which traditional Malaysian instrument is shown in the picture?",
            "Kompang", "Kompang, Sape, Gamelan, Angklung",
            GameType.MULTIPLE_CHOICE
        )
        // 金字塔: 裁切填满(fillFrame=true), phase1正常显示
        insertSeedLesson(
            db, "Wonders of the Ancient World", Subject.GK,
            "Brush off the sand and name the ancient wonder.",
            "The Pyramids of Giza in Egypt were\nbuilt over 4,500 years ago as royal\ntombs for the pharaohs.\n\nThe Great Pyramid is the oldest and\nlargest of the three, and the only\nSeven Wonders of the Ancient World\nstill standing today.\n\nDust off the sand and identify\nthis ancient wonder!",
            "Blathers", "Brush off the sand and name this wonder!",
            R.drawable.ic_gk_pyramid, "",
            "Ancient stone tombs in Egypt",
            "Pyramids of Giza", "Pyramids of Giza, Great Wall of China, Roman Colosseum, Taj Mahal",
            GameType.FOSSIL_EXCAVATION, true, true
        )
        // 地球仪: 裁切填满(fillFrame=true)
        insertSeedLesson(
            db, "Reading the Globe", Subject.GK,
            "Read the globe and count the continents.",
            "A globe is a three-dimensional\nmodel of the Earth.\n\nIt shows the seven continents -\nAsia, Africa, North America, South\nAmerica, Antarctica, Europe and\nAustralia - and the five oceans.\n\nAsia is the largest continent,\nwhile the Pacific is the largest\nocean.",
            "Blathers", "Study the globe, then pick the right answer!",
            R.drawable.ic_gk_globe, "",
            "How many continents are there on Earth?",
            "Seven", "Five, Six, Seven, Eight",
            GameType.MULTIPLE_CHOICE, true, true
        )
    }

    // 塞一行seed lesson用的小helper (subject存displayName字符串)
    private fun insertSeedLesson(
        db: SQLiteDatabase?, title: String, subject: Subject, description: String,
        lessonText: String, npcName: String, npcDialogue: String,
        imageResId: Int, imageUri: String, questionText: String, answer: String, options: String,
        gameType: GameType, fillFrame: Boolean = false, showLessonImage: Boolean = true
    ) {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_SUBJECT, subject.displayName)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_LESSON_TEXT, lessonText)
            put(COLUMN_NPC_NAME, npcName)
            put(COLUMN_NPC_DIALOGUE, npcDialogue)
            put(COLUMN_IMAGE_RES_ID, imageResId)
            put(COLUMN_IMAGE_URI, imageUri)
            put(COLUMN_QUESTION_TEXT, questionText)
            put(COLUMN_ANSWER, answer)
            put(COLUMN_OPTIONS, options)
            put(COLUMN_GAME_TYPE, gameType.name)
            put(COLUMN_FILL_FRAME, if (fillFrame) 1 else 0)
            put(COLUMN_SHOW_IMAGE, if (showLessonImage) 1 else 0)
        }
        db?.insert(TABLE_LESSON, null, values)
    }

    // 给demo学生(id 1..7)塞一些已完成的quest, teacher一进去就看到不同进度
    // quest_id现在指向lessons表的行id (seed顺序 1..14)
    private fun seedProgress(db: SQLiteDatabase?) {
        val questOrder = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
        val seedCounts = intArrayOf(4, 8, 5, 11, 3, 7, 2)

        for (p in seedCounts.indices) {
            val studentId = p + 1
            var k = seedCounts[p]
            if (k > questOrder.size) {
                k = questOrder.size
            }
            for (j in 0 until k) {
                val values = ContentValues().apply {
                    put(COLUMN_STUDENT_ID, studentId)
                    put(COLUMN_QUEST_ID, questOrder[j])
                }
                db?.insert(TABLE_PROGRESS, null, values)
            }
        }
    }

    // ---------- 学生CRUD ----------

    // 把一行cursor读成Student
    private fun readStudentRow(cursor: android.database.Cursor): Student {
        return Student(
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORM)),
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COINS)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AVATAR))
        )
    }

    // 拿所有学生
    fun getAllStudents(): ArrayList<Student> {
        val list = ArrayList<Student>()
        val db = readableDatabase
        val cursor = db.query(TABLE_STUDENT, null, null, null, null, null, "$COLUMN_ID ASC")
        while (cursor.moveToNext()) {
            list.add(readStudentRow(cursor))
        }
        cursor.close()
        return list
    }

    // 用id找一个学生, 找不到return null
    fun getStudent(id: Int): Student? {
        val db = readableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        val cursor = db.query(TABLE_STUDENT, null, selection, selectionArgs, null, null, null)
        var student: Student? = null
        if (cursor.moveToFirst()) {
            student = readStudentRow(cursor)
        }
        cursor.close()
        return student
    }

    // 这个username是不是已经被用了
    fun isUsernameTaken(username: String): Boolean {
        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ?"
        val selectionArgs = arrayOf(username)
        val cursor = db.query(TABLE_STUDENT, null, selection, selectionArgs, null, null, null)
        val taken = cursor.count > 0
        cursor.close()
        return taken
    }

    // 新增一个学生, 全部字段都给, 返回新的row id (-1 = 失败)
    fun insertStudent(
        username: String, password: String, name: String,
        form: String, coins: Int, avatar: String
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NAME, name)
            put(COLUMN_FORM, form)
            put(COLUMN_COINS, coins)
            put(COLUMN_AVATAR, avatar)
        }
        val db = writableDatabase
        return db.insert(TABLE_STUDENT, null, values)
    }

    // 登入: username + password对上return那个学生的id, 不然return -1
    fun loginStudent(username: String, password: String): Int {
        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)
        val cursor = db.query(TABLE_STUDENT, null, selection, selectionArgs, null, null, null)
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
        }
        cursor.close()
        return id
    }

    // ---------- 职员 (admin / teacher) ----------

    // admin / teacher登入: username + password + role三个都对上return true
    fun loginStaff(username: String, password: String, role: String): Boolean {
        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ? AND $COLUMN_ROLE = ?"
        val selectionArgs = arrayOf(username, password, role)
        val cursor = db.query(TABLE_STAFF, null, selection, selectionArgs, null, null, null)
        val ok = cursor.count > 0
        cursor.close()
        return ok
    }

    // 拿某个role的所有职员 (admin那边列teacher用, role传"Teacher")
    fun getAllStaff(role: String): ArrayList<Staff> {
        val list = ArrayList<Staff>()
        val db = readableDatabase
        val selection = "$COLUMN_ROLE = ?"
        val selectionArgs = arrayOf(role)
        val cursor = db.query(TABLE_STAFF, null, selection, selectionArgs, null, null, "$COLUMN_ID ASC")
        while (cursor.moveToNext()) {
            list.add(
                Staff(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
                )
            )
        }
        cursor.close()
        return list
    }

    // 这个username在staff表是不是已经被用了
    fun isStaffUsernameTaken(username: String): Boolean {
        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ?"
        val selectionArgs = arrayOf(username)
        val cursor = db.query(TABLE_STAFF, null, selection, selectionArgs, null, null, null)
        val taken = cursor.count > 0
        cursor.close()
        return taken
    }

    // 新增一个职员, 返回新的row id (-1 = 失败)
    fun insertStaff(username: String, password: String, role: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_ROLE, role)
        }
        val db = writableDatabase
        return db.insert(TABLE_STAFF, null, values)
    }

    // delete一个职员
    fun deleteStaff(id: Int) {
        val db = writableDatabase
        db.delete(TABLE_STAFF, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // admin改学生: name / form / coins
    fun updateStudent(id: Int, name: String, form: String, coins: Int) {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_FORM, form)
            put(COLUMN_COINS, coins)
        }
        val db = writableDatabase
        db.update(TABLE_STUDENT, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // 学生改自己的profile: name + avatar
    fun updateStudentProfile(id: Int, name: String, avatar: String) {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_AVATAR, avatar)
        }
        val db = writableDatabase
        db.update(TABLE_STUDENT, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // 玩游戏赚coins, 先读出来加上去再写回去
    fun addCoins(id: Int, amount: Int) {
        val student = getStudent(id) ?: return
        val values = ContentValues().apply {
            put(COLUMN_COINS, student.coins + amount)
        }
        val db = writableDatabase
        db.update(TABLE_STUDENT, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // 把学生分到某个班级 (只改form)
    fun assignStudentToClass(id: Int, className: String) {
        val values = ContentValues().apply {
            put(COLUMN_FORM, className)
        }
        val db = writableDatabase
        db.update(TABLE_STUDENT, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // 某个班级里的所有学生
    fun getStudentsByClass(className: String): ArrayList<Student> {
        val list = ArrayList<Student>()
        val db = readableDatabase
        val selection = "$COLUMN_FORM = ?"
        val selectionArgs = arrayOf(className)
        val cursor = db.query(TABLE_STUDENT, null, selection, selectionArgs, null, null, "$COLUMN_ID ASC")
        while (cursor.moveToNext()) {
            list.add(readStudentRow(cursor))
        }
        cursor.close()
        return list
    }

    // delete一个学生
    fun deleteStudent(id: Int) {
        val db = writableDatabase
        db.delete(TABLE_STUDENT, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // ---------- 班级CRUD ----------

    fun getAllClasses(): ArrayList<String> {
        val list = ArrayList<String>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CLASS, null, null, null, null, null, "$COLUMN_ID ASC")
        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_NAME)))
        }
        cursor.close()
        return list
    }

    // 开新班级, 空的或者重名就不加
    fun addClass(name: String) {
        if (name.isEmpty()) {
            return
        }
        val db = writableDatabase
        // 先查有没有同名的
        val cursor = db.query(TABLE_CLASS, null, "$COLUMN_CLASS_NAME = ?", arrayOf(name), null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        if (exists) {
            return
        }
        val values = ContentValues().apply {
            put(COLUMN_CLASS_NAME, name)
        }
        db.insert(TABLE_CLASS, null, values)
    }

    // ---------- 学习资料CRUD ----------

    // 把一行cursor读成Lesson
    private fun readLessonRow(cursor: android.database.Cursor): Lesson {
        return Lesson(
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            Subject.fromString(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT))),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LESSON_TEXT)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NPC_NAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NPC_DIALOGUE)),
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_RES_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URI)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_TEXT)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPTIONS)),
            gameTypeFromString(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_TYPE))),
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FILL_FRAME)) == 1,
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SHOW_IMAGE)) == 1
        )
    }

    // game_type字符串转回enum, 认不得就当MCQ
    private fun gameTypeFromString(s: String?): GameType {
        return when (s) {
            "SLOT_MACHINE" -> GameType.SLOT_MACHINE
            "WORD_TRACK" -> GameType.WORD_TRACK
            "FOSSIL_EXCAVATION" -> GameType.FOSSIL_EXCAVATION
            else -> GameType.MULTIPLE_CHOICE
        }
    }

    fun getAllLessons(): ArrayList<Lesson> {
        val list = ArrayList<Lesson>()
        val db = readableDatabase
        val cursor = db.query(TABLE_LESSON, null, null, null, null, null, "$COLUMN_ID ASC")
        while (cursor.moveToNext()) {
            list.add(readLessonRow(cursor))
        }
        cursor.close()
        return list
    }

    fun getLesson(id: Int): Lesson? {
        val db = readableDatabase
        val cursor = db.query(TABLE_LESSON, null, "$COLUMN_ID = ?", arrayOf(id.toString()), null, null, null)
        var lesson: Lesson? = null
        if (cursor.moveToFirst()) {
            lesson = readLessonRow(cursor)
        }
        cursor.close()
        return lesson
    }

    // admin/teacher上载一份新资料 (上载的图走imageUri, imageResId给0)
    fun insertLesson(
        title: String, subject: Subject, description: String,
        lessonText: String, npcName: String, npcDialogue: String,
        imageUri: String, questionText: String, answer: String, options: String,
        gameType: GameType = GameType.MULTIPLE_CHOICE
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_SUBJECT, subject.displayName)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_LESSON_TEXT, lessonText)
            put(COLUMN_NPC_NAME, npcName)
            put(COLUMN_NPC_DIALOGUE, npcDialogue)
            put(COLUMN_IMAGE_RES_ID, 0)
            put(COLUMN_IMAGE_URI, imageUri)
            put(COLUMN_QUESTION_TEXT, questionText)
            put(COLUMN_ANSWER, answer)
            put(COLUMN_OPTIONS, options)
            put(COLUMN_GAME_TYPE, gameType.name)
            // 新建的资料: phase1显示上载图, 不裁切(fit)
            put(COLUMN_FILL_FRAME, 0)
            put(COLUMN_SHOW_IMAGE, 1)
        }
        val db = writableDatabase
        return db.insert(TABLE_LESSON, null, values)
    }

    // update一份资料 (imageResId / fill_frame / show_lesson_image不动, 内置图和显示设定保留)
    fun updateLesson(
        id: Int, title: String, subject: Subject, description: String,
        lessonText: String, npcName: String, npcDialogue: String,
        imageUri: String, questionText: String, answer: String, options: String,
        gameType: GameType
    ) {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_SUBJECT, subject.displayName)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_LESSON_TEXT, lessonText)
            put(COLUMN_NPC_NAME, npcName)
            put(COLUMN_NPC_DIALOGUE, npcDialogue)
            put(COLUMN_IMAGE_URI, imageUri)
            put(COLUMN_QUESTION_TEXT, questionText)
            put(COLUMN_ANSWER, answer)
            put(COLUMN_OPTIONS, options)
            put(COLUMN_GAME_TYPE, gameType.name)
        }
        val db = writableDatabase
        db.update(TABLE_LESSON, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteLesson(id: Int) {
        val db = writableDatabase
        db.delete(TABLE_LESSON, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // ---------- 完成记录 (progress) ----------

    // 某个学生完成的所有questId
    fun getCompletedIds(studentId: Int): HashSet<Int> {
        val set = HashSet<Int>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PROGRESS, null, "$COLUMN_STUDENT_ID = ?",
            arrayOf(studentId.toString()), null, null, null
        )
        while (cursor.moveToNext()) {
            set.add(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUEST_ID)))
        }
        cursor.close()
        return set
    }

    // 标记一个quest为完成 (重复的就不再加)
    fun markDone(studentId: Int, questId: Int) {
        val db = writableDatabase
        val selection = "$COLUMN_STUDENT_ID = ? AND $COLUMN_QUEST_ID = ?"
        val selectionArgs = arrayOf(studentId.toString(), questId.toString())
        val cursor = db.query(TABLE_PROGRESS, null, selection, selectionArgs, null, null, null)
        val already = cursor.count > 0
        cursor.close()
        if (already) {
            return
        }
        val values = ContentValues().apply {
            put(COLUMN_STUDENT_ID, studentId)
            put(COLUMN_QUEST_ID, questId)
        }
        db.insert(TABLE_PROGRESS, null, values)
    }

    // 某个学生完成了几个quest
    fun getCompletedCount(studentId: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PROGRESS, null, "$COLUMN_STUDENT_ID = ?",
            arrayOf(studentId.toString()), null, null, null
        )
        val count = cursor.count
        cursor.close()
        return count
    }

    // 清掉某个学生的完成记录
    fun clearProgress(studentId: Int) {
        val db = writableDatabase
        db.delete(TABLE_PROGRESS, "$COLUMN_STUDENT_ID = ?", arrayOf(studentId.toString()))
    }

    // ---------- session记录 ----------

    // 加一条session
    fun insertSession(studentId: Int, subject: Subject, grade: String, coins: Int, title: String) {
        val values = ContentValues().apply {
            put(COLUMN_STUDENT_ID, studentId)
            put(COLUMN_SUBJECT, subject.displayName)
            put(COLUMN_GRADE, grade)
            put(COLUMN_COINS, coins)
            put(COLUMN_TITLE, title)
        }
        val db = writableDatabase
        db.insert(TABLE_SESSION, null, values)
    }

    // 某个学生的session记录, 最新的在前面 (id DESC)
    fun getSessions(studentId: Int): ArrayList<SessionRecord> {
        val list = ArrayList<SessionRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SESSION, null, "$COLUMN_STUDENT_ID = ?",
            arrayOf(studentId.toString()), null, null, "$COLUMN_ID DESC"
        )
        while (cursor.moveToNext()) {
            val subject = Subject.fromString(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)))
            val grade = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GRADE))
            val coins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COINS))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            list.add(SessionRecord(subject, grade, coins, title))
        }
        cursor.close()
        return list
    }

    // 清掉某个学生的session
    fun clearSessions(studentId: Int) {
        val db = writableDatabase
        db.delete(TABLE_SESSION, "$COLUMN_STUDENT_ID = ?", arrayOf(studentId.toString()))
    }

    // ---------- 笔记 (notes) ----------

    // 拿某个lesson的笔记, 没有就回空字符串
    fun getNote(studentId: Int, questId: Int): String {
        val db = readableDatabase
        val selection = "$COLUMN_STUDENT_ID = ? AND $COLUMN_QUEST_ID = ?"
        val selectionArgs = arrayOf(studentId.toString(), questId.toString())
        val cursor = db.query(TABLE_NOTE, null, selection, selectionArgs, null, null, null)
        var text = ""
        if (cursor.moveToFirst()) {
            text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TEXT))
        }
        cursor.close()
        return text
    }

    // 存/更新笔记, 空的就当删掉
    fun setNote(studentId: Int, questId: Int, text: String) {
        if (text.isEmpty()) {
            deleteNote(studentId, questId)
            return
        }
        val db = writableDatabase
        val selection = "$COLUMN_STUDENT_ID = ? AND $COLUMN_QUEST_ID = ?"
        val selectionArgs = arrayOf(studentId.toString(), questId.toString())
        val cursor = db.query(TABLE_NOTE, null, selection, selectionArgs, null, null, null)
        val exists = cursor.count > 0
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_STUDENT_ID, studentId)
            put(COLUMN_QUEST_ID, questId)
            put(COLUMN_NOTE_TEXT, text)
        }
        if (exists) {
            db.update(TABLE_NOTE, values, selection, selectionArgs)
        } else {
            db.insert(TABLE_NOTE, null, values)
        }
    }

    // 删掉某个lesson的笔记
    fun deleteNote(studentId: Int, questId: Int) {
        val db = writableDatabase
        db.delete(
            TABLE_NOTE, "$COLUMN_STUDENT_ID = ? AND $COLUMN_QUEST_ID = ?",
            arrayOf(studentId.toString(), questId.toString())
        )
    }

    // 清掉某个学生的所有笔记
    fun clearNotes(studentId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NOTE, "$COLUMN_STUDENT_ID = ?", arrayOf(studentId.toString()))
    }
}
