package com.example.assignment1

import android.content.Context
import android.media.MediaPlayer

// MusicManager - 全app共用一个MediaPlayer循环播背景音乐
// 开关状态存SharedPreferences, 记住用户上次的选择 (默认开)
// 谁来控制播放: MyApp的lifecycle callback, app回前台start / 退后台pause
object MusicManager {

    private const val PREFS = "study_crossing_settings"
    private const val KEY_MUSIC_ON = "music_on"

    private var player: MediaPlayer? = null
    private var enabled = true   // 默认开, init()里会被SharedPreferences覆盖

    // MyApp.onCreate里call一次, 读出上次的开关状态
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(KEY_MUSIC_ON, true)
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    // Settings那边切换开关用: 存状态 + 立刻开/关声音
    fun setEnabled(context: Context, on: Boolean) {
        enabled = on
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MUSIC_ON, on).apply()
        if (on) {
            start(context)
        } else {
            stop()
        }
    }

    // 开始/继续播放. 没player就建一个(loop), 暂停过的就接着播
    fun start(context: Context) {
        if (!enabled) {
            return
        }
        if (player == null) {
            player = MediaPlayer.create(context.applicationContext, R.raw.bg_music)
            player?.isLooping = true
        }
        // create可能失败(返回null), 所以judge一下
        if (player != null && !player!!.isPlaying) {
            player!!.start()
        }
    }

    // 切后台时暂停, 保留player下次接着播
    fun pause() {
        if (player != null && player!!.isPlaying) {
            player!!.pause()
        }
    }

    // 关掉音乐: 释放player, 下次开再重建
    fun stop() {
        if (player != null) {
            player!!.stop()
            player!!.release()
            player = null
        }
    }
}
