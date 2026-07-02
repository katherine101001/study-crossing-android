package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.assignment1.data.model.Subject

// StudyActivity - 主要的quest界面, 上下分成两个fragment
//   TopFragment    -> NPC + lesson内容
//   BottomFragment -> mini game (点了Start Challenge之后才出现)
// 另外管理apple生命值和顶部toolbar的颜色
class StudyActivity : AppCompatActivity() {

    // onCreate之后listener需要用到的变量
    private var questionId = 0
    private var subject = "Math"
    private var applesLeft = 3
    private var storedTopFrag: TopFragment? = null
    private var appleIcons = ArrayList<ImageView>()

    // generation counter: 每次mistake都+1, timer只有match时才会执行
    // 用来防止旧的NPC恢复timer把新的mistake覆盖掉
    private var faceRevertGen = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_study)

        // status bar和nav bar padding
        Utils.applyEdgeInsets(this, R.id.study_root, R.id.app_bar_container, 0)

        // 从MainActivity(或archive list)传过来的参数
        questionId = intent.getIntExtra("questionId", 0)
        subject = intent.getStringExtra("subject") ?: "Math"

        // 设置toolbar: back chevron + subject颜色
        Utils.setupBackToolbar(this, R.id.toolbar_study)

        val subj = Subject.fromString(subject)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_study)

        // toolbar和app bar都染成subject的颜色
        val toolbarColor = Utils.getSubjectToolbarColor(subj)
        toolbar.setBackgroundColor(getColor(toolbarColor))
        findViewById<View>(R.id.app_bar_container).setBackgroundColor(getColor(toolbarColor))

        // toolbar下面一条细细的木头色装饰线
        findViewById<View>(R.id.trim_study).setBackgroundColor(getColor(R.color.deep_wood))

        // 设置toolbar里的内容: subject图标、科目名、challenge标题
        val ivToolbarSubj  = findViewById<ImageView>(R.id.iv_toolbar_subject)
        val tvToolbarSubj  = findViewById<TextView>(R.id.tv_toolbar_subject)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        // NPC的小头像 (toolbar左边)
        var subjIcon = R.drawable.ic_npc_owl_angry
        if (subj == Subject.MATH) {
            subjIcon = R.drawable.ic_npc_tanuki_normal
        } else if (subj == Subject.ENGLISH) {
            subjIcon = R.drawable.ic_npc_isabelle_happy
        }
        ivToolbarSubj.setImageResource(subjIcon)

        // 科目名 (上面那行)
        var subjLabel = "MUSEUM QUEST"
        if (subj == Subject.MATH) {
            subjLabel = "MATH QUEST"
        } else if (subj == Subject.ENGLISH) {
            subjLabel = "ENGLISH QUEST"
        }
        tvToolbarSubj.text = subjLabel

        // Challenge标题 (下面那行)
        var challengeTitle = "Discovery Challenge"
        if (subj == Subject.MATH) {
            challengeTitle = "Vault Challenge"
        } else if (subj == Subject.ENGLISH) {
            challengeTitle = "Letter Challenge"
        }
        tvToolbarTitle.text = challengeTitle

        // 收集3个apple icon, deductApple()会一个个隐藏
        appleIcons.clear()
        appleIcons.add(findViewById(R.id.iv_apple_1))
        appleIcons.add(findViewById(R.id.iv_apple_2))
        appleIcons.add(findViewById(R.id.iv_apple_3))

        // 把TopFragment (lesson + NPC) 放到top container里
        val topFrag = TopFragment()
        storedTopFrag = topFrag

        val topArgs = Bundle()
        topArgs.putInt("questionId", questionId)
        topArgs.putString("subject", subject)
        topFrag.arguments = topArgs

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_top, topFrag)
            .commit()

        // "Start Challenge"按钮 - Phase 1的底部
        // 每个subject有不同的图标和文字
        val llStart = findViewById<LinearLayout>(R.id.btn_start_challenge)
        val ivIcon  = findViewById<ImageView>(R.id.iv_challenge_icon)
        val btnText = findViewById<TextView>(R.id.btn_challenge_text)

        var startIcon = R.drawable.ic_brush_hint
        if (subj == Subject.MATH) {
            startIcon = R.drawable.ic_stamp_confirm
        } else if (subj == Subject.ENGLISH) {
            startIcon = R.drawable.ic_confirm_glove
        }
        ivIcon.setImageResource(startIcon)

        var startLabel = "Grab the Brush"
        if (subj == Subject.MATH) {
            startLabel = "Confirm Blueprint"
        } else if (subj == Subject.ENGLISH) {
            startLabel = "Start Sorting"
        }
        btnText.text = startLabel

        // 点Start Challenge -> 切换到Phase 2
        //   隐藏start按钮, 显示divider和bottom container
        //   加载BottomFragment
        val divider         = findViewById<View>(R.id.divider_study)
        val containerTop    = findViewById<FrameLayout>(R.id.container_top)
        val containerBottom = findViewById<FrameLayout>(R.id.container_bottom)

        llStart.setOnClickListener { view ->
            // post到下一帧, 等touch-up完成后再改layout
            // 不然press animation还在跑的时候layout就变了, 手感很怪
            view.post {
                llStart.visibility = View.GONE
                divider.visibility = View.VISIBLE
                containerBottom.visibility = View.VISIBLE

                // 创建BottomFragment, 传同样的参数
                val bottomFrag = BottomFragment()
                val bottomArgs = Bundle()
                bottomArgs.putInt("questionId", questionId)
                bottomArgs.putString("subject", subject)
                bottomFrag.arguments = bottomArgs

                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_bottom, bottomFrag)
                    .commitNow()

                // Phase 2: 隐藏TopFragment里的lesson panel
                storedTopFrag?.hideLessonPanel()

                // 把top half缩小给mini game腾空间, 所有game type都一样
                // 之前MC保留weight=1, 结果bottom太挤MC tile塞不下
                val topP = containerTop.layoutParams as LinearLayout.LayoutParams
                topP.weight = 0f
                topP.height = LinearLayout.LayoutParams.WRAP_CONTENT
                containerTop.layoutParams = topP
            }
        }
    }

    // BottomFragment答错时调用这个:
    //   - 隐藏一个apple
    //   - NPC显示worried表情2.5秒
    //   - 3个apple都没了就结束quest (score=0)
    fun deductApple() {
        applesLeft = applesLeft - 1

        // 每次mistake藏一个apple, 从右往左 (第3个先消失)
        if (applesLeft >= 0 && applesLeft <= 2) {
            val hideIndex = 2 - applesLeft
            if (hideIndex < appleIcons.size) {
                appleIcons[hideIndex].visibility = View.INVISIBLE
            }
        }

        // NPC变worried脸
        storedTopFrag?.showWrongFace()

        // 2.5秒后恢复normal, 用generation counter取消旧的timer
        val myGen = faceRevertGen + 1
        faceRevertGen = myGen
        Utils.runAfterDelay(this, 2500) {
            if (faceRevertGen == myGen) {
                storedTopFrag?.showNormalFace()
            }
        }

        // apple用完 -> 直接结束, score=0
        if (applesLeft <= 0) {
            val intent = Intent(this, RewardActivity::class.java)
            intent.putExtra("score", 0)
            intent.putExtra("subject", subject)
            intent.putExtra("questionId", questionId)
            startActivity(intent)
            finish()
        }
    }

    // toolbar的back箭头
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
