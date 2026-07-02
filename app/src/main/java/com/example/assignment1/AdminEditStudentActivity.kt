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

// AdminEditStudentActivity - 新增 / 编辑一个学生
// 通过Intent的studentId判断是add还是edit:
//   没有studentId (默认-1) = 新增, delete按钮藏起来
//   有studentId = 编辑, prefill资料, 显示delete按钮
class AdminEditStudentActivity : AppCompatActivity() {

    // -1表示新增模式
    var editingId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_edit_student)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.edit_student_root, R.id.app_bar_container, 16)

        val etName = findViewById<EditText>(R.id.et_name)
        val etForm = findViewById<EditText>(R.id.et_form)
        val etCoins = findViewById<EditText>(R.id.et_coins)
        val llSave = findViewById<LinearLayout>(R.id.ll_save_btn)
        val llDelete = findViewById<LinearLayout>(R.id.ll_delete_btn)
        val tvTitle = findViewById<TextView>(R.id.tv_screen_title)
        val ivBack = findViewById<ImageView>(R.id.iv_back)

        // header上的back箭头: 不存直接走, 回到list
        ivBack.setOnClickListener {
            finish()
        }

        // 看有没有传studentId, -1就是新增
        editingId = intent.getIntExtra("studentId", -1)

        if (editingId != -1) {
            // 编辑模式: 把现有资料填进去
            val student = AdminDataStore.getStudent(editingId)
            if (student != null) {
                tvTitle.text = "Edit Student"
                etName.setText(student.name)
                etForm.setText(student.form)
                etCoins.setText(student.coins.toString())
                // 编辑才有得删
                llDelete.visibility = LinearLayout.VISIBLE
            }
        } else {
            // 新增模式
            tvTitle.text = "Add Student"
            llDelete.visibility = LinearLayout.GONE
        }

        // save: 检查名字不为空, coins空的话当0
        llSave.setOnClickListener {
            val name = etName.text.toString()
            val form = etForm.text.toString()
            val coinsStr = etCoins.text.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name!", Toast.LENGTH_SHORT).show()
            } else {
                // coins可能是空的或者乱填, toIntOrNull顶住, 不行就0
                var coins = coinsStr.toIntOrNull() ?: 0
                if (coins < 0) {
                    coins = 0
                }

                if (editingId == -1) {
                    // 新增
                    AdminDataStore.addStudent(name, form, coins)
                    Toast.makeText(this, "Student added!", Toast.LENGTH_SHORT).show()
                } else {
                    // 更新
                    AdminDataStore.updateStudent(editingId, name, form, coins)
                    Toast.makeText(this, "Student updated!", Toast.LENGTH_SHORT).show()
                }
                // 改完回list
                finish()
            }
        }

        // delete: 弹个确认dialog再删, 防止手误
        llDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Remove Student")
                .setMessage("Are you sure you want to remove this student?")
                .setPositiveButton("Remove") { _, _ ->
                    AdminDataStore.deleteStudent(editingId)
                    Toast.makeText(this, "Student removed.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
