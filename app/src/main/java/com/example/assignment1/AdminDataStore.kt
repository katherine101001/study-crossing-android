package com.example.assignment1

import android.content.Context
import com.example.assignment1.data.model.GameType
import com.example.assignment1.data.model.Lesson
import com.example.assignment1.data.model.Staff
import com.example.assignment1.data.model.Student
import com.example.assignment1.data.model.Subject

// AdminDataStore - admin那边管理的数据 (学生 / 班级 / 学习资料)
// 以前是in-memory的list, 现在全部搬去SQLite了
// 这个object只是个facade: 各screen还是照样call AdminDataStore.xxx(), 里面转去DatabaseHelper做CRUD
// seed那些假数据(6学生/3班级/6 lesson)在DatabaseHelper第一次建库时就塞好了
object AdminDataStore {

    // database connection, app启动时MyApp call一次init()就有了
    private lateinit var db: DatabaseHelper

    // MyApp.onCreate里call, 把DatabaseHelper建起来
    fun init(context: Context) {
        db = DatabaseHelper(context.applicationContext)
    }

    // ---------- 学生的CRUD ----------

    // 返回所有学生
    fun getAllStudents(): ArrayList<Student> {
        return db.getAllStudents()
    }

    // 用id找一个学生, 找不到return null
    fun getStudent(id: Int): Student? {
        return db.getStudent(id)
    }

    // 这个username是不是已经有人用了
    fun isUsernameTaken(username: String): Boolean {
        return db.isUsernameTaken(username)
    }

    // admin从后台add学生: 表单只有name/form/coins
    // username自动从名字生成(去空格小写, 撞名就加号码), password默认"1234", avatar默认Cat
    fun addStudent(name: String, form: String, coins: Int) {
        val base = name.replace(" ", "").lowercase()
        var username = base
        var n = 1
        // 撞名就一直加号码直到不重复
        while (isUsernameTaken(username)) {
            username = base + n
            n = n + 1
        }
        db.insertStudent(username, "1234", name, form, coins, "Cat")
    }

    // 学生自己注册一个新账号 (Register界面用)
    // username撞了就return false, 成功return true
    // name先用username顶着, 进去之后可以自己改
    fun registerStudent(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            return false
        }
        if (isUsernameTaken(username)) {
            return false
        }
        val newId = db.insertStudent(username, password, username, "Unassigned", 0, "Cat")
        return newId != -1L
    }

    // 学生登入: username + password对上就return那个学生的id, 不然return -1
    fun loginStudent(username: String, password: String): Int {
        return db.loginStudent(username, password)
    }

    // admin / teacher登入: username + password + role对上才return true
    fun loginStaff(username: String, password: String, role: String): Boolean {
        return db.loginStaff(username, password, role)
    }

    // ---------- 老师管理 (admin那边用) ----------

    // 返回所有老师
    fun getAllTeachers(): ArrayList<Staff> {
        return db.getAllStaff("Teacher")
    }

    // admin新增一个老师账号, username撞了return false, 成功return true
    fun addTeacher(username: String, password: String): Boolean {
        if (username.isEmpty() || password.isEmpty()) {
            return false
        }
        // 不能跟现有职员(admin/teacher)或学生撞username
        if (db.isStaffUsernameTaken(username) || db.isUsernameTaken(username)) {
            return false
        }
        val newId = db.insertStaff(username, password, "Teacher")
        return newId != -1L
    }

    // delete一个老师
    fun deleteTeacher(id: Int) {
        db.deleteStaff(id)
    }

    // admin update学生: 改name/form/coins (不动username/password/avatar)
    fun updateStudent(id: Int, name: String, form: String, coins: Int) {
        db.updateStudent(id, name, form, coins)
    }

    // 学生自己改profile: 只动name + avatar (Profile界面用)
    fun updateStudentProfile(id: Int, name: String, avatar: String) {
        db.updateStudentProfile(id, name, avatar)
    }

    // 学生玩游戏赚coins, 加到他的余额上
    fun addCoins(id: Int, amount: Int) {
        db.addCoins(id, amount)
    }

    // delete一个学生
    fun deleteStudent(id: Int) {
        db.deleteStudent(id)
    }

    // ---------- 班级 (teacher开班分类学生用) ----------

    // 返回所有班级名字
    fun getAllClasses(): ArrayList<String> {
        return db.getAllClasses()
    }

    // 开一个新班级, 空的或者重复的就不加
    fun addClass(name: String) {
        db.addClass(name)
    }

    // 把某个学生分到某个班级 (只改form, 不动名字/coins那些admin的资料)
    fun assignStudentToClass(studentId: Int, className: String) {
        db.assignStudentToClass(studentId, className)
    }

    // 拿某个班级里的所有学生
    fun getStudentsByClass(className: String): ArrayList<Student> {
        return db.getStudentsByClass(className)
    }

    // ---------- 学习资料的CRUD ----------

    fun getAllLessons(): ArrayList<Lesson> {
        return db.getAllLessons()
    }

    fun getLesson(id: Int): Lesson? {
        return db.getLesson(id)
    }

    // admin/teacher上载一份新资料
    fun addLesson(
        title: String, subject: Subject, description: String,
        lessonText: String, npcName: String, npcDialogue: String,
        imageUri: String, questionText: String, answer: String, options: String,
        gameType: GameType
    ) {
        db.insertLesson(
            title, subject, description, lessonText, npcName, npcDialogue,
            imageUri, questionText, answer, options, gameType
        )
    }

    // update一份资料, imageResId不动(内置图保留), 其他全部更新
    fun updateLesson(
        id: Int, title: String, subject: Subject, description: String,
        lessonText: String, npcName: String, npcDialogue: String,
        imageUri: String, questionText: String, answer: String, options: String,
        gameType: GameType
    ) {
        db.updateLesson(
            id, title, subject, description, lessonText, npcName, npcDialogue,
            imageUri, questionText, answer, options, gameType
        )
    }

    fun deleteLesson(id: Int) {
        db.deleteLesson(id)
    }
}
