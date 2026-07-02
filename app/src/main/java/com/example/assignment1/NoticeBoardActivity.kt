package com.example.assignment1

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// NoticeBoardActivity - 显示所有已完成的quest记录
// 像软木板上钉着的卡片, NPC气泡显示统计数据
class NoticeBoardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_board)

        // status bar / nav bar padding
        Utils.applyEdgeInsets(this, R.id.notice_root, R.id.app_bar_container, 0)

        // toolbar with back chevron
        Utils.setupBackToolbar(this, R.id.toolbar_notice)

        // 从data store拿session列表
        val sessionList = IslandDataStore.getSessionHistory()

        // 算total: quest数量和coins (用loop不用.sumOf)
        val totalQuests = sessionList.size
        var totalCoins = 0
        for (i in sessionList.indices) {
            totalCoins = totalCoins + sessionList[i].coins
        }

        // NPC气泡文字, 根据数量单复数变化
        val npcMsg = findViewById<TextView>(R.id.tv_npc_message)

        if (sessionList.isEmpty()) {
            // 还没有完成任何quest
            npcMsg.text = "Your bulletin board is empty! Come back after finishing a quest, and I'll pin it right here~"
        } else {
            // 根据数量选单复数
            var questWord = "quests"
            if (totalQuests == 1) {
                questWord = "quest"
            }
            var coinWord = "Coins"
            if (totalCoins == 1) {
                coinWord = "Coin"
            }
            npcMsg.text = "You've finished $totalQuests $questWord and earned $totalCoins $coinWord! Each one is pinned below~"
        }

        // 空的提示文字, 没有session时才显示
        val emptyHint = findViewById<TextView>(R.id.tv_empty_hint)
        if (sessionList.isEmpty()) {
            emptyHint.visibility = View.VISIBLE
        } else {
            emptyHint.visibility = View.GONE
        }

        // RecyclerView + SessionAdapter
        val recyclerView = findViewById<RecyclerView>(R.id.rv_tasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SessionAdapter(this, sessionList)

        // 如果卡片很少, 缩小cork board让它刚好包住内容
        // post { } 等RecyclerView layout完再measure
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
