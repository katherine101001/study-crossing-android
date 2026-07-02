package com.example.assignment1

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Student

// StudentAdapter - admin学生list的RecyclerView adapter
// 每行: 头像 / 名字 / 班级chip / coins / > chevron
// 点行就打开AdminEditStudentActivity改这个学生
class StudentAdapter(
    private val context: Context,
    private val studentList: ArrayList<Student>
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    // ViewHolder: 缓存每行的findViewById
    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_student_name)
        val tvForm: TextView = itemView.findViewById(R.id.tv_student_form)
        val tvCoins: TextView = itemView.findViewById(R.id.tv_student_coins)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]

        holder.tvName.text = student.name
        holder.tvForm.text = student.form
        holder.tvCoins.text = "${student.coins} Bells"

        // 点row就去编辑这个学生, 把id传过去
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdminEditStudentActivity::class.java)
            intent.putExtra("studentId", student.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }
}
