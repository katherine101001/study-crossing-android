package com.example.assignment1

import android.app.Activity
import android.app.Application
import android.os.Bundle

// MyApp - app的Application class, 比任何Activity都早跑
// 在这里把两个DataStore的SQLite connection建起来 + 初始化背景音乐
// 这样LoginActivity一进来call AdminDataStore.loginStudent()就已经有database了
class MyApp : Application() {

    // 前台activity计数: 0->1代表app回到前台, 1->0代表退到后台
    private var startedCount = 0

    override fun onCreate() {
        super.onCreate()
        // 第一次跑会建库 + 塞demo数据 (DatabaseHelper.onCreate)
        AdminDataStore.init(this)
        IslandDataStore.init(this)
        // 读出背景音乐的开关状态 (默认开)
        MusicManager.init(this)

        // 背景音乐: app回前台播 / 退后台暂停, 全app统一在这里管, 不用碰每个Activity
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedCount = startedCount + 1
                if (startedCount == 1) {
                    // app回到前台
                    MusicManager.start(activity)
                }
            }
            override fun onActivityStopped(activity: Activity) {
                startedCount = startedCount - 1
                if (startedCount == 0) {
                    // app退到后台
                    MusicManager.pause()
                }
            }
            // 其余lifecycle callback用不上, 留空
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
