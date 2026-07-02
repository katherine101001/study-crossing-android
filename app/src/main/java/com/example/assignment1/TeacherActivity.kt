package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// TeacherActivity - teacher登入后的主界面
// teacher只能发布/管理lesson, 不能像admin那样管学生资料
// 所以这里只有一个 Manage Lessons card, 没有学生那块
// lesson数据跟admin共用同一个AdminDataStore
class TeacherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.teacher_root, R.id.app_bar_container, 16)

        // 读Login传过来的名字
        val name = intent.getStringExtra("name") ?: "Teacher"
        val tvHello = findViewById<TextView>(R.id.tv_teacher_hello)
        tvHello.text = "Hi $name!"

        val cardLessons = findViewById<LinearLayout>(R.id.card_lessons)
        val cardClasses = findViewById<LinearLayout>(R.id.card_classes)
        val llLogout = findViewById<LinearLayout>(R.id.ll_logout_btn)

        // 点card去lesson管理界面 (跟admin共用同一个)
        cardLessons.setOnClickListener {
            val intent = Intent(this, AdminLessonsActivity::class.java)
            startActivity(intent)
        }

        // 点card去班级 + 学生进度界面
        cardClasses.setOnClickListener {
            val intent = Intent(this, TeacherClassesActivity::class.java)
            startActivity(intent)
        }

        // logout: 弹确认框, 确认后才回Login
        llLogout.setOnClickListener {
            confirmLogout()
        }
    }

    // 弹确认框, 按Log Out才真的登出
    private fun confirmLogout() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Log Out")
        builder.setMessage("Are you sure you want to log out?")
        builder.setPositiveButton("Log Out") { _, _ ->
            // 清掉登入session (teacher没有student id, 顺手清-1保险)
            IslandDataStore.setCurrentStudent(-1)
            Toast.makeText(this, "Logged out!", Toast.LENGTH_SHORT).show()
            goBackToLogin()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // 回Login界面 (logout / 系统back键都用这个)
    private fun goBackToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // 系统back键也回Login, 不要直接退出app
    override fun onBackPressed() {
        goBackToLogin()
    }

    // 回来时刷新lesson数量
    override fun onResume() {
        super.onResume()

        val lessons = AdminDataStore.getAllLessons()
        val tvLessonCount = findViewById<TextView>(R.id.tv_lesson_count)

        var lWord = "learning materials"
        if (lessons.size == 1) {
            lWord = "learning material"
        }
        tvLessonCount.text = "${lessons.size} $lWord"

        // 班级数量也刷新一下
        val classes = AdminDataStore.getAllClasses()
        val tvClassCount = findViewById<TextView>(R.id.tv_class_count)

        var cWord = "classes"
        if (classes.size == 1) {
            cWord = "class"
        }
        tvClassCount.text = "${classes.size} $cWord"
    }
}
