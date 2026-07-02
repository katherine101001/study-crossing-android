package com.example.assignment1

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Staff

// AdminTeachersActivity - admin看所有老师的list
// 顶上 + Add 开新老师账号, 每行的Remove删老师
// 老师账号没有自助注册, 只能admin在这里加 (admin自己只有一个, 不在这里管)
class AdminTeachersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_teachers)

        // status bar / nav bar padding
        Utils.applyEdgeInsets(this, R.id.teachers_root, R.id.app_bar_container, 0)

        // toolbar back chevron
        Utils.setupBackToolbar(this, R.id.toolbar_teachers)

        // + Add按钮: 弹dialog输入username + password
        val llAdd = findViewById<LinearLayout>(R.id.ll_add_btn)
        llAdd.setOnClickListener {
            showAddTeacherDialog()
        }
    }

    // 每次回来都重新load list, 加完/删完返回时list就更新了
    override fun onResume() {
        super.onResume()
        loadTeachers()
    }

    // 把老师list灌进RecyclerView, 空的就显示提示
    private fun loadTeachers() {
        val teacherList = AdminDataStore.getAllTeachers()

        val emptyHint = findViewById<TextView>(R.id.tv_empty_hint)
        if (teacherList.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
        } else {
            emptyHint.visibility = View.GONE
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_teachers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TeacherAdapter(this, teacherList) { teacher ->
            confirmRemove(teacher)
        }
    }

    // 新增老师的dialog, 两个EditText: username + password
    private fun showAddTeacherDialog() {
        // 竖排放两个输入框
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = Utils.dpToPx(this, 20)
        layout.setPadding(pad, pad, pad, pad)

        val etUsername = EditText(this)
        etUsername.hint = "Username"
        etUsername.inputType = InputType.TYPE_CLASS_TEXT
        layout.addView(etUsername)

        val etPassword = EditText(this)
        etPassword.hint = "Password"
        etPassword.inputType = InputType.TYPE_CLASS_TEXT
        layout.addView(etPassword)

        AlertDialog.Builder(this)
            .setTitle("Add New Teacher")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill in username and password!", Toast.LENGTH_SHORT).show()
                } else {
                    val ok = AdminDataStore.addTeacher(username, password)
                    if (ok) {
                        Toast.makeText(this, "Teacher \"$username\" added!", Toast.LENGTH_SHORT).show()
                        loadTeachers()
                    } else {
                        Toast.makeText(this, "That username is already taken.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 删老师前弹确认框
    private fun confirmRemove(teacher: Staff) {
        AlertDialog.Builder(this)
            .setTitle("Remove Teacher")
            .setMessage("Remove \"${teacher.username}\"? They will not be able to log in anymore.")
            .setPositiveButton("Remove") { _, _ ->
                AdminDataStore.deleteTeacher(teacher.id)
                Toast.makeText(this, "Teacher removed.", Toast.LENGTH_SHORT).show()
                loadTeachers()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
