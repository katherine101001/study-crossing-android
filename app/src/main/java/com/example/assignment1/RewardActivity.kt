package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// RewardActivity - quest完成后的结算界面
// 根据score算出grade, 显示对应主题的badge/halo/NPC表情
// 可以share成绩或者回Main island
class RewardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reward)

        // header下多16dp让wood header看起来平衡
        Utils.applyEdgeInsets(this, R.id.reward_root, R.id.app_bar_container, 16)

        // 读取StudyActivity传的score和subject
        val score = intent.getIntExtra("score", 0)
        val subject = intent.getStringExtra("subject") ?: "Math"
        // 是哪个quest, 答对了要markDone
        val questionId = intent.getIntExtra("questionId", -1)

        // 根据score算grade (project定的分数线, 不要改)
        //   90+ -> S, 75+ -> A, 60+ -> B, 其他 -> C
        var grade = "C"
        if (score >= 90) {
            grade = "S"
        } else if (score >= 75) {
            grade = "A"
        } else if (score >= 60) {
            grade = "B"
        }

        // 每分=10 coins
        val coins = score * 10

        // 保存session, Notice Board就能显示了 (顺便把coins加到学生余额)
        IslandDataStore.recordSession(subject, grade, coins)

        // 答对拿到S(score>=90)才算这个quest完成, 记到当前学生名下
        // 这样Quest Archive的DONE stamp / 笔记按钮 / teacher看到的进度都是真的
        if (score >= 90 && questionId != -1) {
            IslandDataStore.markDone(questionId)
        }

        // 找出所有要更新的view
        val flHalo      = findViewById<FrameLayout>(R.id.fl_grade_halo)
        val ivGrade     = findViewById<ImageView>(R.id.iv_grade)
        val tvLetter    = findViewById<TextView>(R.id.tv_grade_letter)
        val tvSubtitle  = findViewById<TextView>(R.id.tv_result_subtitle)
        val tvGrade     = findViewById<TextView>(R.id.tv_grade)
        val tvNpcDialog = findViewById<TextView>(R.id.tv_npc_dialogue)
        val ivNpc       = findViewById<ImageView>(R.id.iv_tom_nook)
        val tvCoins     = findViewById<TextView>(R.id.tv_coins_earned)
        val llShareWrap = findViewById<LinearLayout>(R.id.btn_share_wrapper)
        val llBackWrap  = findViewById<LinearLayout>(R.id.ll_back_hub_wrapper)

        // 每个grade有自己的一套视觉风格
        if (grade == "S") {
            // S = 完美! 金色halo, S badge, 胜利NPC
            flHalo.setBackgroundResource(R.drawable.shape_grade_halo)
            ivGrade.setImageResource(R.drawable.ic_grade_s)
            ivGrade.visibility = View.VISIBLE
            tvLetter.visibility = View.GONE

            tvSubtitle.text = "★  STELLAR PERFORMANCE  ★"
            tvSubtitle.setTextColor(getColor(R.color.gold_badge))

            tvGrade.text = "Grade: S"
            tvGrade.setBackgroundResource(R.drawable.shape_grade_plaque_green)

            tvNpcDialog.text = "Magnificent, islander! A perfect quest, yes yes!"
            ivNpc.setImageResource(R.drawable.ic_npc_tanuki_win_fullbody)

        } else if (grade == "A") {
            // A = 很棒! 金色halo, A badge
            flHalo.setBackgroundResource(R.drawable.shape_grade_halo)
            ivGrade.setImageResource(R.drawable.ic_grade_a)
            ivGrade.visibility = View.VISIBLE
            tvLetter.visibility = View.GONE

            tvSubtitle.text = "GREAT WORK"
            tvSubtitle.setTextColor(getColor(R.color.gold_badge))

            tvGrade.text = "Grade: A"
            tvGrade.setBackgroundResource(R.drawable.shape_grade_plaque_green)

            tvNpcDialog.text = "Splendid work! Most impressive, hm?"
            ivNpc.setImageResource(R.drawable.ic_npc_tanuki_normal)

        } else if (grade == "B") {
            // B = 不错! 铜色halo, 普通字母(没badge图)
            flHalo.setBackgroundResource(R.drawable.shape_grade_halo_bronze)
            ivGrade.visibility = View.GONE
            tvLetter.visibility = View.VISIBLE
            tvLetter.text = "B"
            tvLetter.setBackgroundResource(R.drawable.shape_grade_badge_b)

            tvSubtitle.text = "SOLID EFFORT"
            tvSubtitle.setTextColor(getColor(R.color.wood_brown))

            tvGrade.text = "Grade: B"
            tvGrade.setBackgroundResource(R.drawable.shape_grade_plaque_bronze)

            tvNpcDialog.text = "Not bad at all! Keep practising and you'll get there."
            ivNpc.setImageResource(R.drawable.ic_npc_tanuki_normal)

        } else {
            // C = 继续加油! 石头halo, 红色plaque, 流汗NPC
            flHalo.setBackgroundResource(R.drawable.shape_grade_halo_stone)
            ivGrade.visibility = View.GONE
            tvLetter.visibility = View.VISIBLE
            tvLetter.text = "C"
            tvLetter.setBackgroundResource(R.drawable.shape_grade_badge_c)

            tvSubtitle.text = "KEEP STUDYING"
            tvSubtitle.setTextColor(getColor(R.color.chip_red))

            tvGrade.text = "Grade: C"
            tvGrade.setBackgroundResource(R.drawable.shape_grade_plaque_red)

            tvNpcDialog.text = "Don't give up, islander!  Re-read the lesson and try again."
            ivNpc.setImageResource(R.drawable.ic_npc_tanuki_sweat)
        }

        tvCoins.text = "$coins Coins"

        // Share按钮 - 打开系统share sheet, 玩家可以share成绩
        llShareWrap.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "I got Grade $grade and earned $coins Coins in Study Crossing!"
            )
            startActivity(Intent.createChooser(shareIntent, "Mail to a Friend"))
        }

        // Back to Hub - 清掉StudyActivity, 防止back press回到这里
        llBackWrap.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}
