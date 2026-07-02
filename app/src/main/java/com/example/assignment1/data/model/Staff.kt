package com.example.assignment1.data.model

// Staff - 一个职员账号 (admin 或 teacher), 登入选身份时对staff表
//   username + password : 登入用
//   role                : "Admin" 或 "Teacher", admin只有一个, teacher可以很多个
// 学生不放这里, 学生有自己的Student记录
data class Staff(
    var id: Int,
    var username: String,
    var password: String,
    var role: String
)
