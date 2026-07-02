package com.example.assignment1

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class BadgesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_badges)

        // status bar和nav bar padding
        Utils.applyEdgeInsets(this, R.id.badges_root, R.id.app_bar_container, 0)

        // wood header上的custom back按钮
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // 从data store拿数据
        val history = IslandDataStore.getSessionHistory()
        val questions = IslandDataStore.getAllQuests()

        // 遍历session history, 统计:
        //   有没有拿过S? / S有几个? / 总coins?
        var hasS = false
        var sCount = 0
        var totalCoins = 0
        for (i in history.indices) {
            totalCoins = totalCoins + history[i].coins
            if (history[i].grade == "S") {
                hasS = true
                sCount = sCount + 1
            }
        }
        val completedCount = history.size

        // "全部quest完成"检查: 先假设true, 找到一个没done的就变false
        var allDone = questions.isNotEmpty()
        for (i in questions.indices) {
            if (!questions[i].isDone) {
                allDone = false
            }
        }

        // 5个badge的判断条件, 和下面的badge name/icon数组一一对应
        // 0.S-Rank Explorer   - 拿过任意S grade
        // 1.Triple Scholar    - 完成3+个quest
        // 2.Coin Collector    - 500+ coins
        // 3.Completionist     - 8个quest全做完
        // 4.S-Tier Master     - 3+个S grade
        val earned = booleanArrayOf(
            hasS,
            completedCount >= 3,
            totalCoins >= 500,
            allDone && questions.size == 8,
            sCount >= 3
        )

        // 数有几个earned了
        var earnedCount = 0
        for (i in earned.indices) {
            if (earned[i]) {
                earnedCount = earnedCount + 1
            }
        }

        // 更新进度文字和绿色进度条
        val tvProgressText = findViewById<TextView>(R.id.tv_progress_text)
        tvProgressText.text = "$earnedCount / 5"

        val progressFill = findViewById<View>(R.id.view_progress_fill)
        // display case宽度 = 屏幕宽度 - 两边各28dp(屏幕边缘) - 两边各28dp(case内部) = 64dp
        val sideInsetsPx = Utils.dpToPx(this, 64)
        val displayWidth = resources.displayMetrics.widthPixels - sideInsetsPx
        val fillWidth = (displayWidth * earnedCount) / 5

        val params = progressFill.layoutParams
        params.width = fillWidth
        progressFill.layoutParams = params

        // 根据收集进度显示不同的hint文字
        val tvHint = findViewById<TextView>(R.id.tv_progress_hint)
        if (earnedCount == 5) {
            tvHint.text = "All badges collected! Magnificent!"
        } else if (earnedCount >= 3) {
            tvHint.text = "Over halfway there - keep questing!"
        } else if (earnedCount > 0) {
            tvHint.text = "Keep questing to unlock more badges!"
        }

        // 5个badge的定义: parallel arrays, 同一个index对应同一个badge
        val badgeNames = arrayOf(
            "S-Rank Explorer",
            "Triple Scholar",
            "Coin Collector",
            "Completionist",
            "S-Tier Master"
        )
        val badgeReqs = arrayOf(
            "Earn S grade once (score >= 90)",
            "Complete 3 quests (any grade)",
            "Collect 500 total Coins",
            "Finish all 8 quests",
            "Earn S grade 3 times"
        )
        val badgeIcons = intArrayOf(
            R.drawable.ic_grade_s,
            R.drawable.ic_grade_a,
            R.drawable.ic_coin_bag,
            R.drawable.ic_stamp_done,
            R.drawable.ic_grade_star
        )
        // 每个badge有rarity等级, 用一个colored chip显示
        val rarityLabels = arrayOf("RARE", "COMMON", "RARE", "EPIC", "LEGENDARY")
        val rarityColors = arrayOf("#3F7EA8", "#8D6E63", "#3F7EA8", "#7E57C2", "#F9A825")

        // 给每个badge建一个card, 加到list里, card之间加个10dp的小spacer
        val llBadgeList = findViewById<LinearLayout>(R.id.ll_badge_list)
        for (i in badgeNames.indices) {
            val card = createBadgeCard(
                badgeNames[i],
                badgeReqs[i],
                badgeIcons[i],
                rarityLabels[i],
                rarityColors[i],
                earned[i]
            )
            llBadgeList.addView(card)

            // card之间加spacer (最后一个不加)
            if (i < badgeNames.size - 1) {
                val spacer = View(this)
                val spacerHeight = Utils.dpToPx(this, 10)
                val sp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    spacerHeight
                )
                spacer.layoutParams = sp
                llBadgeList.addView(spacer)
            }
        }
    }

    // 构建一个badge card, 结构是:
    //   左边: icon + 彩色halo (earned=金色, locked=灰色)
    //   中间: name + requirement文字 + rarity chip
    //   右边: 状态pill (绿色Earned或棕色Locked)
    private fun createBadgeCard(
        name: String,
        requirement: String,
        iconResId: Int,
        rarityLabel: String,
        rarityColorHex: String,
        earned: Boolean
    ): LinearLayout {

        // 外层card
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL

        val padH = Utils.dpToPx(this, 14)
        val padV = Utils.dpToPx(this, 12)
        card.setPadding(padH, padV, padH, padV)
        card.setBackgroundResource(R.drawable.shape_paper_card)
        card.elevation = 3f

        // 左边icon halo: 56x56dp圆圈
        val haloSize = Utils.dpToPx(this, 56)
        val iconFrame = FrameLayout(this)
        val frameLp = LinearLayout.LayoutParams(haloSize, haloSize)
        frameLp.marginEnd = Utils.dpToPx(this, 14)
        iconFrame.layoutParams = frameLp

        if (earned) {
            // earned: 金色halo + 轻微elevation
            iconFrame.setBackgroundResource(R.drawable.shape_grade_halo)
            iconFrame.elevation = Utils.dpToPxF(this, 2f)
        } else {
            // locked: 普通灰色圆圈
            iconFrame.setBackgroundResource(R.drawable.shape_avatar_circle)
        }

        // halo里面的icon (32dp), locked就半透明
        val iv = ImageView(this)
        val iconInner = Utils.dpToPx(this, 32)
        val ivLp = FrameLayout.LayoutParams(iconInner, iconInner)
        ivLp.gravity = Gravity.CENTER
        iv.layoutParams = ivLp
        iv.setImageResource(iconResId)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER

        if (!earned) {
            iv.alpha = 0.45f
        }
        iconFrame.addView(iv)

        // 中间文字列: name + requirement + rarity chip
        val textCol = LinearLayout(this)
        textCol.orientation = LinearLayout.VERTICAL
        textCol.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        // badge名字
        val tvName = TextView(this)
        tvName.text = name
        tvName.textSize = 15f
        tvName.setTextColor(getColor(R.color.deep_wood))
        tvName.setTypeface(resources.getFont(R.font.fredoka_bold))
        if (!earned) {
            tvName.alpha = 0.6f
        }

        // 获得条件说明
        val tvReq = TextView(this)
        tvReq.text = requirement
        tvReq.textSize = 11f
        tvReq.setTextColor(getColor(R.color.wood_brown))
        tvReq.setTypeface(resources.getFont(R.font.nunito_bold))
        val reqLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reqLp.topMargin = Utils.dpToPx(this, 2)
        tvReq.layoutParams = reqLp

        // rarity chip (RARE/COMMON/EPIC/LEGENDARY)
        // 在代码里用GradientDrawable画背景, 因为每个rarity颜色不同
        val tvRarity = TextView(this)
        tvRarity.text = rarityLabel
        tvRarity.textSize = 8f
        tvRarity.setTextColor(Color.WHITE)
        tvRarity.setTypeface(resources.getFont(R.font.fredoka_bold))
        tvRarity.letterSpacing = 0.18f
        val chipPadH = Utils.dpToPx(this, 8)
        val chipPadV = Utils.dpToPx(this, 2)
        tvRarity.setPadding(chipPadH, chipPadV, chipPadH, chipPadV)

        val chipBg = GradientDrawable()
        chipBg.shape = GradientDrawable.RECTANGLE
        chipBg.cornerRadius = Utils.dpToPxF(this, 8f)
        chipBg.setColor(Color.parseColor(rarityColorHex))
        tvRarity.background = chipBg

        if (!earned) {
            tvRarity.alpha = 0.55f
        }
        val rarityLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rarityLp.topMargin = Utils.dpToPx(this, 6)
        tvRarity.layoutParams = rarityLp

        textCol.addView(tvName)
        textCol.addView(tvReq)
        textCol.addView(tvRarity)

        // 右边状态pill: earned=绿色★, locked=棕色
        val tvStatus = TextView(this)
        val statusPadH = Utils.dpToPx(this, 12)
        val statusPadV = Utils.dpToPx(this, 6)
        tvStatus.setPadding(statusPadH, statusPadV, statusPadH, statusPadV)
        tvStatus.textSize = 11f
        tvStatus.setTextColor(Color.WHITE)
        tvStatus.setTypeface(resources.getFont(R.font.fredoka_bold))
        tvStatus.letterSpacing = 0.08f

        if (earned) {
            tvStatus.text = "Earned ★"
            tvStatus.setBackgroundResource(R.drawable.shape_pill_green)
        } else {
            tvStatus.text = "Locked"
            tvStatus.setBackgroundResource(R.drawable.shape_pill_brown)
        }

        // 三部分拼起来
        card.addView(iconFrame)
        card.addView(textCol)
        card.addView(tvStatus)

        return card
    }
}
