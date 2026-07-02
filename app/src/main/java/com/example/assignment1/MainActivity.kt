package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.assignment1.data.model.Subject

// MainActivity - 主界面(island hub)
// 显示玩家头像、欢迎语、3个学科建筑(Math/English/GK)
// 还有Notice Board和Quest Archive的快捷入口
// 点建筑物会打开那个subject第一个还没玩过的quest
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // status bar和nav bar padding
        Utils.applyEdgeInsets(this, R.id.main_root, R.id.app_bar_container, 0)

        // 顶部绿色toolbar, 用自定义title所以隐藏默认的
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 点欢迎卡片去Profile改自己的资料 (名字/avatar)
        val llProfileCard = findViewById<LinearLayout>(R.id.ll_profile_card)
        llProfileCard.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            // editMode = 从Main进来改资料, 不是第一次填
            intent.putExtra("editMode", true)
            startActivity(intent)
        }

        // 3个学科建筑按钮
        val btnMath    = findViewById<LinearLayout>(R.id.btn_math_building)
        val btnEnglish = findViewById<LinearLayout>(R.id.btn_english_building)
        val btnGk      = findViewById<LinearLayout>(R.id.btn_gk_building)

        btnMath.setOnClickListener {
            openStudy(Subject.MATH)
        }
        btnEnglish.setOnClickListener {
            openStudy(Subject.ENGLISH)
        }
        btnGk.setOnClickListener {
            openStudy(Subject.GK)
        }

        // Notice Board和Quest Archive的快捷入口
        val llBoard = findViewById<LinearLayout>(R.id.ll_notice_board)
        val llArchive = findViewById<LinearLayout>(R.id.ll_quest_archive)

        llBoard.setOnClickListener {
            val intent = Intent(this, NoticeBoardActivity::class.java)
            startActivity(intent)
        }
        llArchive.setOnClickListener {
            val intent = Intent(this, QuestArchiveActivity::class.java)
            startActivity(intent)
        }
    }

    // 每次回到Main都刷新一下名字和avatar
    // 这样从Profile改完资料返回时, 这里会显示新的
    override fun onResume() {
        super.onResume()

        val name = IslandDataStore.getPlayerName()
        val avatar = IslandDataStore.getPlayerAvatar()

        val tvWelcome = findViewById<TextView>(R.id.tv_welcome)
        tvWelcome.text = "Hi $name!"

        // avatar name -> drawable的映射, parallel arrays
        val avatarNames = arrayOf("Cat", "Dog", "Bear", "Squirrel", "Frog")
        val avatarDrawables = intArrayOf(
            R.drawable.ic_avatar_cat,
            R.drawable.ic_avatar_dog,
            R.drawable.ic_avatar_bear,
            R.drawable.ic_avatar_squirrel,
            R.drawable.ic_avatar_frog
        )

        // 默认用cat
        var avatarRes = R.drawable.ic_avatar_cat
        for (i in avatarNames.indices) {
            if (avatarNames[i] == avatar) {
                avatarRes = avatarDrawables[i]
            }
        }
        val ivAvatar = findViewById<ImageView>(R.id.iv_avatar)
        ivAvatar.setImageResource(avatarRes)

        // 顶上的bell余额, 玩游戏赚了coins回来这里会更新
        val tvCoins = findViewById<TextView>(R.id.tv_coins)
        tvCoins.text = IslandDataStore.getTotalCoins().toString()
    }

    // 找到对应subject的第一个quest, 打开StudyActivity
    private fun openStudy(subject: Subject) {
        val quests = IslandDataStore.getQuestsBySubject(subject)
        if (quests.isEmpty()) {
            // 正常情况不会发生, 3个subject一共8个quest
            Toast.makeText(this, "No quests for ${subject.displayName} yet!", Toast.LENGTH_SHORT).show()
            return
        }

        // 默认开第一个, 然后loop整个array找一个还没玩过的
        // 玩过的(isDone)就跳去下一个, 全部玩过了就还是开第一个
        var chosenQuest = quests[0]
        for (i in quests.indices) {
            if (!quests[i].isDone) {
                chosenQuest = quests[i]
                break
            }
        }

        val intent = Intent(this, StudyActivity::class.java)
        intent.putExtra("questionId", chosenQuest.id)
        intent.putExtra("subject", subject.displayName)
        startActivity(intent)
    }

    // 加载overflow menu (Badges + Settings)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // 强制overflow popup显示icon, 原生Android默认是隐藏的
        // 用reflection, 如果失败了menu照样能用(只是没icon)
        try {
            val menuClass = menu?.javaClass
            val method = menuClass?.getDeclaredMethod(
                "setOptionalIconsVisible",
                Boolean::class.javaPrimitiveType
            )
            method?.isAccessible = true
            method?.invoke(menu, true)
        } catch (e: Exception) {
            // reflection失败就算了, 不影响功能
        }
        return true
    }

    // overflow menu的点击处理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_badges) {
            val intent = Intent(this, BadgesActivity::class.java)
            startActivity(intent)
            return true
        } else if (item.itemId == R.id.menu_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
