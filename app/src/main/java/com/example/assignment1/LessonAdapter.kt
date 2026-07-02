package com.example.assignment1

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Lesson

// LessonAdapter - admin学习资料list的RecyclerView adapter
// 每行: subject图标 / 标题 / 简介 / subject chip / > chevron
// 点行就打开AdminEditLessonActivity改这个lesson
class LessonAdapter(
    private val context: Context,
    private val lessonList: ArrayList<Lesson>
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_lesson_title)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_lesson_desc)
        val tvSubject: TextView = itemView.findViewById(R.id.tv_lesson_subject)
        val ivSubject: ImageView = itemView.findViewById(R.id.iv_subject_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessonList[position]

        holder.tvTitle.text = lesson.title
        holder.tvDesc.text = lesson.description
        holder.tvSubject.text = lesson.subject.displayName.uppercase()

        // 圆圈里优先显示资料的图: 上载的 > 内置的 > 没有就用subject的NPC图标
        if (lesson.imageUri.isNotEmpty()) {
            holder.ivSubject.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.ivSubject.setImageURI(android.net.Uri.parse(lesson.imageUri))
        } else if (lesson.imageResId != 0) {
            holder.ivSubject.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.ivSubject.setImageResource(lesson.imageResId)
        } else {
            holder.ivSubject.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.ivSubject.setImageResource(Utils.getSubjectIconForList(lesson.subject))
        }

        // chip颜色用现成的Utils helper
        holder.tvSubject.setBackgroundResource(Utils.getSubjectChipBg(lesson.subject))

        // 点row就去编辑这个lesson
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdminEditLessonActivity::class.java)
            intent.putExtra("lessonId", lesson.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return lessonList.size
    }
}
