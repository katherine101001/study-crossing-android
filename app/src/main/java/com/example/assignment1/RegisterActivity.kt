package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// RegisterActivity - 新玩家注册的界面
// 只有student能注册(admin/teacher的账号是staff那边给的), 填username和password再确认一次
// 一样没有后端, 注册成功只是Toast一下然后回Login
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.register_root, R.id.app_bar_container, 16)

        // Activity所以用findViewById
        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirm = findViewById<EditText>(R.id.et_confirm_password)
        val llRegister = findViewById<LinearLayout>(R.id.ll_register_btn)
        val tvGoLogin = findViewById<TextView>(R.id.tv_go_login)

        // register按钮: 检查都填了, 两次密码一样, 然后开student账号回Login
        llRegister.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please pick a username!", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Please make a password!", Toast.LENGTH_SHORT).show()
            } else if (password != confirm) {
                // 两次密码不一样不行
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            } else {
                // 只有student能注册, 直接开账号存进AdminDataStore
                val ok = AdminDataStore.registerStudent(username, password)
                if (!ok) {
                    // username被人用了
                    Toast.makeText(this, "That username is taken, pick another!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Account made! Please log in, $username.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        // 已经有账号的话点这个回Login
        tvGoLogin.setOnClickListener {
            // 直接finish回到上一个Login界面就好
            finish()
        }
    }
}
