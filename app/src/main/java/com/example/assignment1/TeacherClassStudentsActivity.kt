package com.example.assignment1

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Student

// TeacherClassStudentsActivity - 看某个班级里的学生 + 他们的进度
// 顶上 + Add Student 把别的学生分进这个班级 (这就是分类学生)
// 每个学生row有进度条, 还有Move按钮可以分到别的班级
class TeacherClassStudentsActivity : AppCompatActivity() {

    // 现在看的是哪个班级
    var className = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher_class_students)

        Utils.applyEdgeInsets(this, R.id.class_students_root, R.id.app_bar_container, 0)
        Utils.setupBackToolbar(this, R.id.toolbar_class_students)

        // 拿班级名字, 显示在toolbar上
        className = intent.getStringExtra("className") ?: ""
        val tvTitle = findViewById<TextView>(R.id.tv_class_title)
        tvTitle.text = className

        // + Add Student: 把别的班的学生分进来
        val llAdd = findViewById<LinearLayout>(R.id.ll_add_btn)
        llAdd.setOnClickListener {
            showAddStudentDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadList()
    }

    // load这个班级的学生进list, 改完班级也call这个刷新
    private fun loadList() {
        val students = AdminDataStore.getStudentsByClass(className)

        val emptyHint = findViewById<TextView>(R.id.tv_empty_hint)
        if (students.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
        } else {
            emptyHint.visibility = View.GONE
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_class_students)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // onChanged传loadList, Move之后这个学生会从当前班级消失
        recyclerView.adapter = StudentProgressAdapter(this, students) {
            loadList()
        }
    }

    // 弹dialog列出不在这个班的学生, 选一个分进来
    private fun showAddStudentDialog() {
        val allStudents = AdminDataStore.getAllStudents()

        // 只要不在这个班级的学生
        val others = ArrayList<Student>()
        for (i in allStudents.indices) {
            if (allStudents[i].form != className) {
                others.add(allStudents[i])
            }
        }

        if (others.isEmpty()) {
            Toast.makeText(this, "Everyone is already in this class!", Toast.LENGTH_SHORT).show()
            return
        }

        // dialog显示学生名字 + 他原本的班级, 方便认
        val labels = ArrayList<String>()
        for (i in others.indices) {
            labels.add("${others[i].name}  (${others[i].form})")
        }
        val labelArr = labels.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Add Student to $className")
            .setItems(labelArr) { _, which ->
                val picked = others[which]
                AdminDataStore.assignStudentToClass(picked.id, className)
                Toast.makeText(this, "${picked.name} added to $className.", Toast.LENGTH_SHORT).show()
                loadList()
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
