package com.example.assignment1.data.model

// Student - 一个学生账号, 也是整个app唯一的"用户"记录
// 学生登入后就绑到这里其中一条, 改名字/赚coins/做进度全部指向同一条
// 没有真的database, 先用data class放在AdminDataStore的list里
//   username + password : 登入用 (以后SQLite就是users表的两个column)
//   name + avatar       : 学生自己的profile, 可以从Main进去改
//   form                : 班级, teacher用来分类学生
//   coins               : bell余额, admin给初始值, 学生玩游戏会增加
// 进度(学过几个quest)不放这里, 存在IslandDataStore按studentId分开记
data class Student(
    var id: Int,
    var username: String,
    var password: String,
    var name: String,
    var form: String,
    var coins: Int,
    var avatar: String
)
