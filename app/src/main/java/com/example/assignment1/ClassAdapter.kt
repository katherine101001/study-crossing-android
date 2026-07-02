package com.example.assignment1

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// ClassAdapter - teacher的班级list
// 每行: 班级名字 / 班里有几个学生 / > chevron
// 点行就进那个班级看学生 + 进度
class ClassAdapter(
    private val context: Context,
    private val classList: ArrayList<String>
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_class_name)
        val tvCount: TextView = itemView.findViewById(R.id.tv_class_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val className = classList[position]
        holder.tvName.text = className

        // 数一下这个班级有几个学生
        val count = AdminDataStore.getStudentsByClass(className).size
        var word = "students"
        if (count == 1) {
            word = "student"
        }
        holder.tvCount.text = "$count $word"

        // 点进去看这个班的学生 + 进度
        holder.itemView.setOnClickListener {
            val intent = Intent(context, TeacherClassStudentsActivity::class.java)
            intent.putExtra("className", className)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return classList.size
    }
}
