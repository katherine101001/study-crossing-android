package com.example.assignment1

import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// QuestArchiveActivity - 显示所有quest (无论done没done)
// 玩家可以点任意一个重玩, 和Notice Board不同
// (Notice Board只显示已完成的)
class QuestArchiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quest_archive)

        // status bar / nav bar padding
        Utils.applyEdgeInsets(this, R.id.archive_root, R.id.app_bar_container, 0)

        // toolbar + back chevron
        Utils.setupBackToolbar(this, R.id.toolbar_archive)
    }

    // 放onResume里, 这样写完笔记返回时按钮文字(Add/Edit)会更新
    override fun onResume() {
        super.onResume()

        // 获取全部quest, 转成TaskItem给adapter用
        val taskList = IslandDataStore.buildTaskList()

        // 数一下完成了几个, plain loop最简单
        var doneCount = 0
        for (i in taskList.indices) {
            if (taskList[i].isDone) {
                doneCount = doneCount + 1
            }
        }

        // NPC气泡, 根据完成数量换说法
        val npcMsg = findViewById<TextView>(R.id.tv_npc_message)
        if (doneCount == 0) {
            npcMsg.text = "Nothing here yet! Go study some subjects and your quests will appear, yes yes!"
        } else {
            var questWord = "quests"
            if (doneCount == 1) {
                questWord = "quest"
            }
            npcMsg.text = "$doneCount $questWord ready to replay. Tap any one to jump back in, yes yes!"
        }

        // RecyclerView + TaskAdapter
        val recyclerView = findViewById<RecyclerView>(R.id.rv_archive)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TaskAdapter(this, taskList)

        // 内容少就缩小cork board
        val corkBoard = findViewById<FrameLayout>(R.id.cork_board)
        recyclerView.post {
            Utils.shrinkCorkIfFits(recyclerView, corkBoard)
        }
    }

    // toolbar back按钮
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
