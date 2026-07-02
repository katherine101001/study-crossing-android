package com.example.assignment1

import android.os.Bundle
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

// TeacherClassesActivity - teacher看所有班级的list
// 顶上 + New Class 开新班级, 点某个班级进去看学生和进度
class TeacherClassesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher_classes)

        Utils.applyEdgeInsets(this, R.id.classes_root, R.id.app_bar_container, 0)
        Utils.setupBackToolbar(this, R.id.toolbar_classes)

        // + New Class: 弹个dialog输入班级名字
        val llAdd = findViewById<LinearLayout>(R.id.ll_add_btn)
        llAdd.setOnClickListener {
            showAddClassDialog()
        }
    }

    // 回来时重新load (新开班级/分了学生之后人数会变)
    override fun onResume() {
        super.onResume()

        val classList = AdminDataStore.getAllClasses()

        val emptyHint = findViewById<TextView>(R.id.tv_empty_hint)
        if (classList.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
        } else {
            emptyHint.visibility = View.GONE
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_classes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ClassAdapter(this, classList)
    }

    // 开新班级的dialog, 里面塞一个EditText让teacher打名字
    private fun showAddClassDialog() {
        val input = EditText(this)
        input.hint = "e.g. 4 Dedikasi"
        // 给点padding不然贴边很难看
        val pad = Utils.dpToPx(this, 20)
        input.setPadding(pad, pad, pad, pad)

        AlertDialog.Builder(this)
            .setTitle("Open New Class")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please type a class name!", Toast.LENGTH_SHORT).show()
                } else {
                    AdminDataStore.addClass(name)
                    Toast.makeText(this, "Class \"$name\" opened!", Toast.LENGTH_SHORT).show()
                    // 刷新list
                    onResume()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
