package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// AdminLessonsActivity - 显示所有学习资料的list
// 顶上 + Add 新增lesson, 点某行去编辑
class AdminLessonsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_lessons)

        Utils.applyEdgeInsets(this, R.id.lessons_root, R.id.app_bar_container, 0)
        Utils.setupBackToolbar(this, R.id.toolbar_lessons)

        // + Add: 去editor新增lesson
        val llAdd = findViewById<LinearLayout>(R.id.ll_add_btn)
        llAdd.setOnClickListener {
            val intent = Intent(this, AdminEditLessonActivity::class.java)
            startActivity(intent)
        }
    }

    // 回来时重新load, 改完/删完/加完都会更新
    override fun onResume() {
        super.onResume()

        val lessonList = AdminDataStore.getAllLessons()

        val emptyHint = findViewById<TextView>(R.id.tv_empty_hint)
        if (lessonList.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
        } else {
            emptyHint.visibility = View.GONE
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_lessons)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = LessonAdapter(this, lessonList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
