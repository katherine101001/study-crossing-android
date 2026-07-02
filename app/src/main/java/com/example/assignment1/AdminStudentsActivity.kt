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

// AdminStudentsActivity - 显示所有学生的list
// 顶上有个 + Add 按钮新增学生
// 点list里的某行就去编辑那个学生
class AdminStudentsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_students)

        // status bar / nav bar padding
        Utils.applyEdgeInsets(this, R.id.students_root, R.id.app_bar_container, 0)

        // toolbar back chevron
        Utils.setupBackToolbar(this, R.id.toolbar_students)

        // + Add按钮: 去editor, 不传id表示新增
        val llAdd = findViewById<LinearLayout>(R.id.ll_add_btn)
        llAdd.setOnClickListener {
            val intent = Intent(this, AdminEditStudentActivity::class.java)
            // 不放studentId extra, editor那边会当成新增
            startActivity(intent)
        }
    }

    // 每次回来都重新load list, 改完/删完/加完返回时list就更新了
    override fun onResume() {
        super.onResume()

        val studentList = AdminDataStore.getAllStudents()

        // 空list显示提示文字
        val emptyHint = findViewById<TextView>(R.id.tv_empty_hint)
        if (studentList.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
        } else {
            emptyHint.visibility = View.GONE
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_students)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = StudentAdapter(this, studentList)
    }

    // toolbar back
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
