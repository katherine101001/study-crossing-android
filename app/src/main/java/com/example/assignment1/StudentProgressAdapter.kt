package com.example.assignment1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Student

// StudentProgressAdapter - teacher看某个班级学生的进度
// 每行: 头像 / 名字 / coins / 进度条 / Move按钮
// 进度 = 学过几个lesson / lesson总数
// Move按钮可以把学生分到别的班级 (这就是"分类学生")
// onChanged: 改完班级后通知activity重新load list
class StudentProgressAdapter(
    private val context: Context,
    private val studentList: ArrayList<Student>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<StudentProgressAdapter.ProgressViewHolder>() {

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_student_name)
        val tvCoins: TextView = itemView.findViewById(R.id.tv_student_coins)
        val tvLabel: TextView = itemView.findViewById(R.id.tv_progress_label)
        val vFill: View = itemView.findViewById(R.id.v_progress_fill)
        val vEmpty: View = itemView.findViewById(R.id.v_progress_empty)
        val tvMove: TextView = itemView.findViewById(R.id.tv_move_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val student = studentList[position]

        holder.tvName.text = student.name
        holder.tvCoins.text = "${student.coins} Bells"

        // 进度 = 这个学生完成的quest数 / quest总数
        // 完成数从IslandDataStore按studentId查, 是真实进度不是写死的
        val total = IslandDataStore.getTotalQuests()
        var done = IslandDataStore.getCompletedCount(student.id)
        if (done > total) {
            done = total
        }

        holder.tvLabel.text = "$done / $total lessons"

        // 用layout_weight做进度条: 学过的占done, 剩下占(total-done)
        val fillParams = holder.vFill.layoutParams as LinearLayout.LayoutParams
        val emptyParams = holder.vEmpty.layoutParams as LinearLayout.LayoutParams
        if (total <= 0) {
            // 万一lesson全删光了, 进度条就全空
            fillParams.weight = 0f
            emptyParams.weight = 1f
        } else {
            fillParams.weight = done.toFloat()
            emptyParams.weight = (total - done).toFloat()
        }
        holder.vFill.layoutParams = fillParams
        holder.vEmpty.layoutParams = emptyParams

        // Move按钮: 把这个学生分到别的班级
        holder.tvMove.setOnClickListener {
            showMoveDialog(student)
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    // 弹个dialog列出别的班级, 选一个就把学生分过去
    private fun showMoveDialog(student: Student) {
        val allClasses = AdminDataStore.getAllClasses()

        // 排除学生现在的班级, 只显示别的
        val others = ArrayList<String>()
        for (i in allClasses.indices) {
            if (allClasses[i] != student.form) {
                others.add(allClasses[i])
            }
        }

        if (others.isEmpty()) {
            Toast.makeText(context, "No other class to move to yet.", Toast.LENGTH_SHORT).show()
            return
        }

        // setItems要个array
        val names = others.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Move ${student.name} to...")
            .setItems(names) { _, which ->
                val target = names[which]
                AdminDataStore.assignStudentToClass(student.id, target)
                Toast.makeText(context, "${student.name} moved to $target.", Toast.LENGTH_SHORT).show()
                // 通知activity重新load (这个学生会从当前班级消失)
                onChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
