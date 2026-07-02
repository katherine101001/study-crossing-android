package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// LoginActivity - 玩家登入的界面
// 选身份(Student/Admin), 填username和password, 点login进去
// 没有真的后端, 数据都是假的, 只检查不为空就放行
// Student进Profile选villager, Admin直接进Main island
class LoginActivity : AppCompatActivity() {

    // 记住选了哪个身份, 默认Student
    var selectedRole = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.login_root, R.id.app_bar_container, 16)

        // Activity所以用findViewById
        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val llLogin = findViewById<LinearLayout>(R.id.ll_login_btn)
        val tvGoRegister = findViewById<TextView>(R.id.tv_go_register)

        // 三个身份card, parallel arrays: id和name一一对应
        val roleCardIds = intArrayOf(
            R.id.ll_role_student,
            R.id.ll_role_teacher,
            R.id.ll_role_admin
        )
        val roleNames = arrayOf("Student", "Teacher", "Admin")

        // 把两个card view找出来放list里, click时高亮一个取消另一个
        val roleCards = ArrayList<LinearLayout>()
        for (i in roleCardIds.indices) {
            val card = findViewById<LinearLayout>(roleCardIds[i])
            roleCards.add(card)
        }

        // 给每个card绑click listener
        for (i in roleCards.indices) {
            // 用local variable idx, 因为listener后面才执行
            val idx = i
            roleCards[idx].setOnClickListener {
                // 保存选中的身份
                selectedRole = roleNames[idx]

                // 全部重置成默认background
                for (j in roleCards.indices) {
                    roleCards[j].setBackgroundResource(R.drawable.shape_avatar_card)
                }
                // 高亮选中的那个
                roleCards[idx].setBackgroundResource(R.drawable.shape_avatar_selected)

                Toast.makeText(this, "${roleNames[idx]} selected!", Toast.LENGTH_SHORT).show()
            }
        }

        // login按钮: 检查username和password不为空, 然后根据身份跳不同界面
        llLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter your username!", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password!", Toast.LENGTH_SHORT).show()
            } else {
                if (selectedRole == "Admin") {
                    // Admin进管理后台, 不进岛, username+password要对上staff表
                    if (AdminDataStore.loginStaff(username, password, "Admin")) {
                        Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, AdminActivity::class.java)
                        intent.putExtra("name", username)
                        intent.putExtra("role", "Admin")
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Wrong username or password.", Toast.LENGTH_SHORT).show()
                    }
                } else if (selectedRole == "Teacher") {
                    // Teacher只管lesson, 不能管学生资料, 一样要对上staff表
                    if (AdminDataStore.loginStaff(username, password, "Teacher")) {
                        Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, TeacherActivity::class.java)
                        intent.putExtra("name", username)
                        intent.putExtra("role", "Teacher")
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Wrong username or password.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Student: 用username+password去对账号, 对上才进岛
                    val sid = AdminDataStore.loginStudent(username, password)
                    if (sid == -1) {
                        Toast.makeText(this, "Wrong username or password. Register first if you are new!", Toast.LENGTH_SHORT).show()
                    } else {
                        // 绑定当前学生, 之后所有进度/笔记/资料都指向这个id
                        IslandDataStore.setCurrentStudent(sid)
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        // 没有账号的话点这个去Register
        tvGoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
