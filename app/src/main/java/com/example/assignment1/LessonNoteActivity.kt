package com.example.assignment1

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// LessonNoteActivity - 学生给学习过的lesson写自己的笔记
// 从Quest Archive的"Add Note / Edit My Note"按钮进来
// 注意: 学生只能改自己的笔记, 不能动admin那边的lesson内容
//   已经有笔记 = prefill进去 + 显示delete按钮
//   没有笔记 = 空白, delete藏起来
class LessonNoteActivity : AppCompatActivity() {

    // 这个笔记是属于哪个lesson(quest)的
    var questId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lesson_note)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.note_root, R.id.app_bar_container, 16)

        val tvLessonTitle = findViewById<TextView>(R.id.tv_lesson_title)
        val etNote = findViewById<EditText>(R.id.et_note)
        val llSave = findViewById<LinearLayout>(R.id.ll_save_btn)
        val llDelete = findViewById<LinearLayout>(R.id.ll_delete_btn)
        val ivBack = findViewById<ImageView>(R.id.iv_back)

        // 顶上的back箭头: 不存直接回去
        ivBack.setOnClickListener {
            finish()
        }

        // 从Intent拿quest id和lesson标题
        questId = intent.getIntExtra("questionId", -1)
        val lessonTitle = intent.getStringExtra("lessonTitle") ?: "This Lesson"
        tvLessonTitle.text = lessonTitle

        // 把已有的笔记load出来 (没有就是空字符串)
        val existing = IslandDataStore.getNote(questId)
        etNote.setText(existing)

        // 有笔记才显示delete按钮
        if (IslandDataStore.hasNote(questId)) {
            llDelete.visibility = LinearLayout.VISIBLE
        } else {
            llDelete.visibility = LinearLayout.GONE
        }

        // save: 空的就当没有笔记 (setNote里text空会自动删掉旧的), 然后回去
        llSave.setOnClickListener {
            val text = etNote.text.toString().trim()
            IslandDataStore.setNote(questId, text)
            if (text.isEmpty()) {
                Toast.makeText(this, "No note saved.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        // delete: 确认一下再删
        llDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete My Note")
                .setMessage("Are you sure you want to delete your note for this lesson?")
                .setPositiveButton("Delete") { _, _ ->
                    IslandDataStore.deleteNote(questId)
                    Toast.makeText(this, "Note deleted.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
