package com.example.assignment1.data.model

// Lesson - admin/teacher上载的学习资料, 字段照着每个科目真正需要的数据来设计
// 跟游戏里的Quest结构对得上: 教学内容 + NPC + 图片 + 题目数据
//
// 共用字段:
//   title        : 资料标题
//   subject      : 科目 (Math/English/GK), chip颜色和图标都靠它
//   description  : 一句话简介 (list上显示)
//   lessonText   : 学生要读的教学内容正文
//   npcName      : 哪个NPC教 (Tom Nook / Isabelle / Blathers)
//   npcDialogue  : NPC的开场白
//   imageResId   : 内置drawable图 (seed资料用), 0 = 没有
//   imageUri     : admin/teacher上载的图片URI字符串, "" = 没上载
//
// 科目专属的题目数据 (同样三个字段, 不同科目意思不同):
//   questionText : Math=算式题(12 × 24 = ?), English=句子/题干, GK=提示/问题
//   answer       : Math=数字答案(288), English=正确单词, GK=正确名称
//   options      : English/GK的选项(逗号分隔), Math用不到就留空
data class Lesson(
    var id: Int,
    var title: String,
    var subject: Subject,
    var description: String,
    var lessonText: String,
    var npcName: String,
    var npcDialogue: String,
    var imageResId: Int,
    var imageUri: String,
    var questionText: String,
    var answer: String,
    var options: String,
    // 统一之后lesson就是quest, 这3个决定phase2玩法 + phase1配图
    var gameType: GameType = GameType.MULTIPLE_CHOICE,
    var fillFrame: Boolean = false,        // true = 裁切填满(金字塔/地球仪), false = 透明图fit居中
    var showLessonImage: Boolean = true    // false = phase1不显示图(化石入门), 图只当挖出来用
)
