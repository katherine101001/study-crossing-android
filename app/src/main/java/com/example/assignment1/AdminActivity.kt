package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// AdminActivity - admin登入后的主界面(管理后台)
// 两个大card: 管理学生 / 管理学习资料
// 还有个logout回Login
// 没有真后端, 数据都在AdminDataStore里
class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.admin_root, R.id.app_bar_container, 16)

        // 读Login传过来的admin名字, 没有就用默认
        val name = intent.getStringExtra("name") ?: "Admin"
        val tvHello = findViewById<TextView>(R.id.tv_admin_hello)
        tvHello.text = "Hi $name!"

        val cardStudents = findViewById<LinearLayout>(R.id.card_students)
        val cardTeachers = findViewById<LinearLayout>(R.id.card_teachers)
        val cardLessons = findViewById<LinearLayout>(R.id.card_lessons)
        val llLogout = findViewById<LinearLayout>(R.id.ll_logout_btn)

        // 点card去对应的管理界面
        cardStudents.setOnClickListener {
            val intent = Intent(this, AdminStudentsActivity::class.java)
            startActivity(intent)
        }
        cardTeachers.setOnClickListener {
            val intent = Intent(this, AdminTeachersActivity::class.java)
            startActivity(intent)
        }
        cardLessons.setOnClickListener {
            val intent = Intent(this, AdminLessonsActivity::class.java)
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
            // 清掉登入session (admin没有student id, 顺手清-1保险)
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

    // 每次回到这个界面都刷新一下count (改完数据返回时数字会更新)
    override fun onResume() {
        super.onResume()

        val students = AdminDataStore.getAllStudents()
        val teachers = AdminDataStore.getAllTeachers()
        val lessons = AdminDataStore.getAllLessons()

        val tvStudentCount = findViewById<TextView>(R.id.tv_student_count)
        val tvTeacherCount = findViewById<TextView>(R.id.tv_teacher_count)
        val tvLessonCount = findViewById<TextView>(R.id.tv_lesson_count)

        // 数量单复数处理一下, 看起来比较自然
        var sWord = "residents on the island"
        if (students.size == 1) {
            sWord = "resident on the island"
        }
        tvStudentCount.text = "${students.size} $sWord"

        var tWord = "teachers"
        if (teachers.size == 1) {
            tWord = "teacher"
        }
        tvTeacherCount.text = "${teachers.size} $tWord"

        var lWord = "learning materials"
        if (lessons.size == 1) {
            lWord = "learning material"
        }
        tvLessonCount.text = "${lessons.size} $lWord"
    }
}
