package com.example.assignment1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Staff

// TeacherAdapter - admin老师list的RecyclerView adapter
// 每行: 头像 / username / Teacher chip / Remove按钮
// 点Remove就call回去activity弹确认框删这个老师
class TeacherAdapter(
    private val context: Context,
    private val teacherList: ArrayList<Staff>,
    private val onRemove: (Staff) -> Unit
) : RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

    // ViewHolder: 缓存每行的findViewById
    class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_teacher_name)
        val tvRole: TextView = itemView.findViewById(R.id.tv_teacher_role)
        val tvRemove: TextView = itemView.findViewById(R.id.tv_teacher_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false)
        return TeacherViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        val teacher = teacherList[position]

        holder.tvName.text = teacher.username
        holder.tvRole.text = teacher.role

        // 点Remove按钮就交给activity处理 (弹确认框)
        holder.tvRemove.setOnClickListener {
            onRemove(teacher)
        }
    }

    override fun getItemCount(): Int {
        return teacherList.size
    }
}
