package com.example.assignment1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.SessionRecord

// SessionAdapter - Notice Board的RecyclerView adapter
// 每行显示: NPC头像 / 完成了什么 / subject chip / grade / coins
class SessionAdapter(
    private val context: Context,
    private val records: ArrayList<SessionRecord>
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    // ViewHolder: 缓存findViewById, 和TaskAdapter一样做法
    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_session_title)
        val tvSubject: TextView = itemView.findViewById(R.id.tv_session_subject)
        val tvGrade: TextView = itemView.findViewById(R.id.tv_session_grade)
        val tvCoins: TextView = itemView.findViewById(R.id.tv_session_coins)
        val ivSubject: ImageView = itemView.findViewById(R.id.iv_subject_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    // 把SessionRecord的数据填到row里
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val r = records[position]

        // 基本文字
        holder.tvTitle.text = r.title
        holder.tvSubject.text = r.subject.displayName.uppercase()
        holder.tvGrade.text = r.grade
        holder.tvCoins.text = "${r.coins} Coins"

        // subject图标 - 用Utils helper
        val iconRes = Utils.getSubjectIconForList(r.subject)
        holder.ivSubject.setImageResource(iconRes)

        // subject chip背景色 - 用Utils helper
        val chipBg = Utils.getSubjectChipBg(r.subject)
        holder.tvSubject.setBackgroundResource(chipBg)
    }

    override fun getItemCount(): Int {
        return records.size
    }
}
