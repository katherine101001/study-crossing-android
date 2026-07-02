package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// SettingsActivity - 设置页面
// 可以编辑profile、重置进度、看credits、看版本号
// 背景音乐开关还没做 (SOON)
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // status bar + gesture bar padding
        Utils.applyEdgeInsets(this, R.id.settings_root, R.id.app_bar_container, 0)

        // wood header上的back按钮
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        // Edit Profile - 打开Profile screen重新设置名字/头像
        val rowEditProfile = findViewById<View>(R.id.ll_edit_profile)
        rowEditProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // 背景音乐开关: 点一下切ON/OFF, 状态存SharedPreferences, 全app生效
        val rowMusic = findViewById<View>(R.id.ll_music_toggle)
        val tvMusicState = findViewById<TextView>(R.id.tv_music_state)
        refreshMusicState(tvMusicState)
        rowMusic.setOnClickListener {
            val nowOn = !MusicManager.isEnabled()
            MusicManager.setEnabled(this, nowOn)
            refreshMusicState(tvMusicState)
            if (nowOn) {
                Toast.makeText(this, "Music on!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Music off.", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset Progress - 弹确认框, 确认后清空所有数据
        val rowReset = findViewById<View>(R.id.ll_reset_progress)
        rowReset.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Reset Progress")
            builder.setMessage("This will clear all your session history and quest progress. This cannot be undone.")
            builder.setPositiveButton("Reset") { _, _ ->
                IslandDataStore.resetAll()
                Toast.makeText(this, "Progress reset!", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }

        // Log Out - 弹确认框, 确认后清掉当前登入的学生, 回去Login界面
        val rowLogout = findViewById<View>(R.id.ll_logout)
        rowLogout.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Log Out")
            builder.setMessage("Are you sure you want to log out?")
            builder.setPositiveButton("Log Out") { _, _ ->
                // 清掉当前学生session (-1 = 没人登入)
                IslandDataStore.setCurrentStudent(-1)
                Toast.makeText(this, "Logged out!", Toast.LENGTH_SHORT).show()
                // 回Login, 清掉整个back stack, 不让按返回键再进去
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }

        // Credits - 弹dialog显示制作信息
        val rowCredits = findViewById<View>(R.id.ll_credits)
        rowCredits.setOnClickListener {
            val msg = "Game design by PE Assignment 1 Team\n\n" +
                      "Icons adapted from open-source sets\n" +
                      "Fonts: Fredoka (Hafidz Maulana) + Nunito (Vernon Adams)\n\n" +
                      "Built with Kotlin + Android SDK"

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Credits")
            builder.setMessage(msg)
            builder.setPositiveButton("OK", null)
            builder.show()
        }

        // App Version那行只是展示信息, 不需要click处理
    }

    // 更新音乐开关那个小标签 (ON / OFF)
    private fun refreshMusicState(tv: TextView) {
        if (MusicManager.isEnabled()) {
            tv.text = "ON"
        } else {
            tv.text = "OFF"
        }
    }
}
