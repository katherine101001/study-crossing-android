package com.example.assignment1

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.TaskItem

// TaskAdapter - Quest Archive的RecyclerView adapter
// 每行: NPC图标 / quest标题 / subject chip / DONE stamp或> chevron
// 点行就打开StudyActivity重玩那个quest
class TaskAdapter(
    private val context: Context,
    private val taskList: ArrayList<TaskItem>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // ViewHolder: 缓存每个row的findViewById结果
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_task_title)
        val tvSubject: TextView = itemView.findViewById(R.id.tv_task_subject)
        val ivSubject: ImageView = itemView.findViewById(R.id.iv_subject_icon)
        val ivDone: ImageView = itemView.findViewById(R.id.iv_done_stamp)
        val tvChevron: TextView = itemView.findViewById(R.id.tv_chevron)
        val tvNotes: TextView = itemView.findViewById(R.id.tv_notes_btn)
    }

    // RecyclerView需要新的row时调用
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    // row滚到屏幕上时, 把对应position的数据填进去
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        // 基本文字
        holder.tvTitle.text = task.title
        // GK的全名"GENERAL KNOWLEDGE"太长会挡住Add Note按钮, chip上缩成"GK"
        if (task.subject.name == "GK") {
            holder.tvSubject.text = "GK"
        } else {
            holder.tvSubject.text = task.subject.displayName.uppercase()
        }

        // subject图标
        val iconRes = Utils.getSubjectIconForList(task.subject)
        holder.ivSubject.setImageResource(iconRes)

        // subject chip背景
        val chipBg = Utils.getSubjectChipBg(task.subject)
        holder.tvSubject.setBackgroundResource(chipBg)

        // 完成的显示DONE stamp, 没完成的显示> chevron
        // 只有学习过(done)的lesson才让写笔记, 所以notes按钮也跟着done走
        if (task.isDone) {
            holder.ivDone.visibility = View.VISIBLE
            holder.tvChevron.visibility = View.GONE
            holder.tvNotes.visibility = View.VISIBLE

            // 已经写过笔记的话按钮变成琥珀色"Edit", 没写过是绿色"Add"
            if (IslandDataStore.hasNote(task.id)) {
                holder.tvNotes.text = "Edit My Note"
                holder.tvNotes.setBackgroundResource(R.drawable.shape_pill_amber)
            } else {
                holder.tvNotes.text = "+ Add Note"
                holder.tvNotes.setBackgroundResource(R.drawable.shape_pill_green)
            }
        } else {
            holder.ivDone.visibility = View.GONE
            holder.tvChevron.visibility = View.VISIBLE
            // 还没学过的lesson不给写笔记
            holder.tvNotes.visibility = View.GONE
        }

        // 点row就打开StudyActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, StudyActivity::class.java)
            intent.putExtra("questionId", task.id)
            intent.putExtra("subject", task.subject.displayName)
            context.startActivity(intent)
        }

        // 点notes按钮单独打开笔记界面 (不会触发上面的row点击)
        holder.tvNotes.setOnClickListener {
            val intent = Intent(context, LessonNoteActivity::class.java)
            intent.putExtra("questionId", task.id)
            intent.putExtra("lessonTitle", task.title)
            intent.putExtra("subject", task.subject.displayName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }
}
