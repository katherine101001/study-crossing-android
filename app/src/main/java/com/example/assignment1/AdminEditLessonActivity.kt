package com.example.assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment1.data.model.GameType
import com.example.assignment1.data.model.Subject

// AdminEditLessonActivity - 新增 / 编辑一份学习资料 (admin和teacher共用)
// 字段照着每个科目真正需要的数据来设计:
//   共用: 标题 / 科目 / 上载图片 / 教学内容 / 简介 / NPC名字+台词
//   科目专属: 题目 + 答案 (+ English/GK还有选项, Math没有)
//   有lessonId = 编辑(prefill+显示delete), 没有 = 新增
class AdminEditLessonActivity : AppCompatActivity() {

    // 选图片的request code
    private val PICK_IMAGE = 1001

    // -1表示新增
    var editingId = -1

    // 现在选的subject, 默认Math
    var selectedSubject = Subject.MATH

    // 现在选的game type, 默认MCQ (决定phase2玩法)
    var selectedGameType = GameType.MULTIPLE_CHOICE

    // 上载的图片URI字符串, ""表示没上载(或者用内置图)
    private var pickedImageUri = ""

    // 三个chip缓存一下, 切换高亮要用
    private var chipMath: TextView? = null
    private var chipEnglish: TextView? = null
    private var chipGk: TextView? = null
    private var chipGameMcq: TextView? = null
    private var chipGameSlot: TextView? = null
    private var chipGameWord: TextView? = null
    private var chipGameFossil: TextView? = null

    // 几个会随科目变label/显示的view
    private var tvQuestionLabel: TextView? = null
    private var tvAnswerLabel: TextView? = null
    private var tvQuestionHint: TextView? = null
    private var llOptionsGroup: LinearLayout? = null
    private var etAnswer: EditText? = null

    // 图片预览相关
    private var ivPreview: ImageView? = null
    private var tvNoImage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_edit_lesson)

        Utils.applyEdgeInsets(this, R.id.edit_lesson_root, R.id.app_bar_container, 16)

        val etTitle = findViewById<EditText>(R.id.et_title)
        val etLessonText = findViewById<EditText>(R.id.et_lesson_text)
        val etDesc = findViewById<EditText>(R.id.et_desc)
        val etNpcName = findViewById<EditText>(R.id.et_npc_name)
        val etNpcDialogue = findViewById<EditText>(R.id.et_npc_dialogue)
        val etQuestion = findViewById<EditText>(R.id.et_question)
        val etOptions = findViewById<EditText>(R.id.et_options)
        val llSave = findViewById<LinearLayout>(R.id.ll_save_btn)
        val llDelete = findViewById<LinearLayout>(R.id.ll_delete_btn)
        val llPickImage = findViewById<LinearLayout>(R.id.ll_pick_image)
        val tvTitle = findViewById<TextView>(R.id.tv_screen_title)
        val ivBack = findViewById<ImageView>(R.id.iv_back)

        // header上的back箭头: 不存直接走, 回到list
        ivBack.setOnClickListener {
            finish()
        }

        etAnswer = findViewById(R.id.et_answer)
        chipMath = findViewById(R.id.chip_math)
        chipEnglish = findViewById(R.id.chip_english)
        chipGk = findViewById(R.id.chip_gk)
        chipGameMcq = findViewById(R.id.chip_game_mcq)
        chipGameSlot = findViewById(R.id.chip_game_slot)
        chipGameWord = findViewById(R.id.chip_game_word)
        chipGameFossil = findViewById(R.id.chip_game_fossil)
        tvQuestionLabel = findViewById(R.id.tv_question_label)
        tvAnswerLabel = findViewById(R.id.tv_answer_label)
        tvQuestionHint = findViewById(R.id.tv_question_hint)
        llOptionsGroup = findViewById(R.id.ll_options_group)
        ivPreview = findViewById(R.id.iv_preview)
        tvNoImage = findViewById(R.id.tv_no_image)

        // subject chip: 只决定主题色/NPC, 不再管题目字段
        chipMath!!.setOnClickListener {
            selectedSubject = Subject.MATH
            refreshChips()
        }
        chipEnglish!!.setOnClickListener {
            selectedSubject = Subject.ENGLISH
            refreshChips()
        }
        chipGk!!.setOnClickListener {
            selectedSubject = Subject.GK
            refreshChips()
        }

        // game type chip: 决定phase2玩法 + 题目区显示哪些字段
        chipGameMcq!!.setOnClickListener {
            selectedGameType = GameType.MULTIPLE_CHOICE
            refreshGameChips()
            applyGameTypeFields()
        }
        chipGameSlot!!.setOnClickListener {
            selectedGameType = GameType.SLOT_MACHINE
            refreshGameChips()
            applyGameTypeFields()
        }
        chipGameWord!!.setOnClickListener {
            selectedGameType = GameType.WORD_TRACK
            refreshGameChips()
            applyGameTypeFields()
        }
        chipGameFossil!!.setOnClickListener {
            selectedGameType = GameType.FOSSIL_EXCAVATION
            refreshGameChips()
            applyGameTypeFields()
        }

        // 上载图片按钮: 开系统的文件选择器选一张图
        llPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        // 看有没有lessonId
        editingId = intent.getIntExtra("lessonId", -1)

        if (editingId != -1) {
            // 编辑模式: 把现有资料填进去
            val lesson = AdminDataStore.getLesson(editingId)
            if (lesson != null) {
                tvTitle.text = "Edit Lesson"
                etTitle.setText(lesson.title)
                etLessonText.setText(lesson.lessonText)
                etDesc.setText(lesson.description)
                etNpcName.setText(lesson.npcName)
                etNpcDialogue.setText(lesson.npcDialogue)
                etQuestion.setText(lesson.questionText)
                etAnswer!!.setText(lesson.answer)
                etOptions.setText(lesson.options)
                selectedSubject = lesson.subject
                selectedGameType = lesson.gameType

                // 图片: 先记住现有的uri, 再决定preview显示什么
                pickedImageUri = lesson.imageUri
                if (lesson.imageUri.isNotEmpty()) {
                    // 之前上载过的图
                    showPreviewUri(Uri.parse(lesson.imageUri))
                } else if (lesson.imageResId != 0) {
                    // seed资料的内置图
                    showPreviewRes(lesson.imageResId)
                }

                llDelete.visibility = LinearLayout.VISIBLE
            }
        } else {
            // 新增模式
            tvTitle.text = "Add Lesson"
            llDelete.visibility = LinearLayout.GONE
        }

        // 把subject / game type的高亮和题目字段都刷新一遍
        refreshChips()
        refreshGameChips()
        applyGameTypeFields()

        // save: title不为空就存, 选项整理一下
        llSave.setOnClickListener {
            val title = etTitle.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a lesson title!", Toast.LENGTH_SHORT).show()
            } else {
                val lessonText = etLessonText.text.toString()
                val desc = etDesc.text.toString()
                val npcName = etNpcName.text.toString()
                val npcDialogue = etNpcDialogue.text.toString()
                val question = etQuestion.text.toString()
                val answer = etAnswer!!.text.toString()
                // slot machine没有选项, 其余玩法才存选项
                var options = ""
                if (selectedGameType != GameType.SLOT_MACHINE) {
                    options = etOptions.text.toString()
                }

                if (editingId == -1) {
                    AdminDataStore.addLesson(
                        title, selectedSubject, desc,
                        lessonText, npcName, npcDialogue,
                        pickedImageUri, question, answer, options, selectedGameType
                    )
                    Toast.makeText(this, "Lesson uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    AdminDataStore.updateLesson(
                        editingId, title, selectedSubject, desc,
                        lessonText, npcName, npcDialogue,
                        pickedImageUri, question, answer, options, selectedGameType
                    )
                    Toast.makeText(this, "Lesson updated!", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }

        // delete: 确认一下再删
        llDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Lesson")
                .setMessage("Are you sure you want to delete this learning material?")
                .setPositiveButton("Delete") { _, _ ->
                    AdminDataStore.deleteLesson(editingId)
                    Toast.makeText(this, "Lesson deleted.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // 文件选择器选完图回来
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                // 拿persistable权限, 不然换个session就读不到这张图了
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // 拿不到就算了, 这个session内还是看得到
                }
                pickedImageUri = uri.toString()
                showPreviewUri(uri)
            }
        }
    }

    // 预览显示一张上载的图
    private fun showPreviewUri(uri: Uri) {
        ivPreview!!.setImageURI(uri)
        ivPreview!!.visibility = View.VISIBLE
        tvNoImage!!.visibility = View.GONE
    }

    // 预览显示一张内置drawable图 (seed资料用)
    private fun showPreviewRes(resId: Int) {
        ivPreview!!.setImageResource(resId)
        ivPreview!!.visibility = View.VISIBLE
        tvNoImage!!.visibility = View.GONE
    }

    // 选中的chip不透明, 其他两个调暗
    private fun refreshChips() {
        chipMath!!.alpha = 0.45f
        chipEnglish!!.alpha = 0.45f
        chipGk!!.alpha = 0.45f

        if (selectedSubject == Subject.MATH) {
            chipMath!!.alpha = 1.0f
        } else if (selectedSubject == Subject.ENGLISH) {
            chipEnglish!!.alpha = 1.0f
        } else {
            chipGk!!.alpha = 1.0f
        }
    }

    // game type chip: 选中的不透明, 其它调暗
    private fun refreshGameChips() {
        chipGameMcq!!.alpha = 0.45f
        chipGameSlot!!.alpha = 0.45f
        chipGameWord!!.alpha = 0.45f
        chipGameFossil!!.alpha = 0.45f

        if (selectedGameType == GameType.SLOT_MACHINE) {
            chipGameSlot!!.alpha = 1.0f
        } else if (selectedGameType == GameType.WORD_TRACK) {
            chipGameWord!!.alpha = 1.0f
        } else if (selectedGameType == GameType.FOSSIL_EXCAVATION) {
            chipGameFossil!!.alpha = 1.0f
        } else {
            chipGameMcq!!.alpha = 1.0f
        }
    }

    // 根据game type换题目区的label/提示, slot machine没有选项就藏起来
    private fun applyGameTypeFields() {
        if (selectedGameType == GameType.SLOT_MACHINE) {
            tvQuestionLabel!!.text = "QUESTION (e.g. 12 × 24 = ?)"
            tvAnswerLabel!!.text = "ANSWER (the number)"
            etAnswer!!.hint = "e.g. 288"
            llOptionsGroup!!.visibility = View.GONE
            tvQuestionHint!!.text = "Slot machine: the answer is a number, no options needed."
        } else if (selectedGameType == GameType.WORD_TRACK) {
            tvQuestionLabel!!.text = "SENTENCE (use ___ for the blank)"
            tvAnswerLabel!!.text = "CORRECT WORD"
            etAnswer!!.hint = "e.g. put off"
            llOptionsGroup!!.visibility = View.VISIBLE
            tvQuestionHint!!.text = "Word track: list the word choices (comma separated) and include the correct one."
        } else if (selectedGameType == GameType.FOSSIL_EXCAVATION) {
            tvQuestionLabel!!.text = "HINT (shown while digging)"
            tvAnswerLabel!!.text = "CORRECT NAME / TAG"
            etAnswer!!.hint = "e.g. Pyramids of Giza"
            llOptionsGroup!!.visibility = View.VISIBLE
            tvQuestionHint!!.text = "Fossil dig: the uploaded image is revealed; list the name choices (comma separated)."
        } else {
            // MULTIPLE_CHOICE
            tvQuestionLabel!!.text = "QUESTION"
            tvAnswerLabel!!.text = "CORRECT ANSWER (must match one option)"
            etAnswer!!.hint = "e.g. RM 60"
            llOptionsGroup!!.visibility = View.VISIBLE
            tvQuestionHint!!.text = "Multiple choice: list the options (comma separated); the answer must match one exactly."
        }
    }
}
