package com.example.assignment1

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// ProfileActivity - 玩家选nickname和avatar的界面
// 填好名字, 选个头像, 点submit就去Main island
// 数据不持久化, 通过Intent extras传给MainActivity
class ProfileActivity : AppCompatActivity() {

    // 记住选了哪个avatar, 默认Cat
    var selectedAvatar = "Cat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // status bar + nav bar padding, header下多16dp
        Utils.applyEdgeInsets(this, R.id.profile_root, R.id.app_bar_container, 16)

        // Activity所以用findViewById
        val etName = findViewById<EditText>(R.id.et_nickname)
        val llSubmit = findViewById<LinearLayout>(R.id.ll_submit_btn)

        // 5个avatar card, parallel arrays: id和name一一对应
        val avatarCardIds = intArrayOf(
            R.id.ll_cat_card,
            R.id.ll_dog_card,
            R.id.ll_bear_card,
            R.id.ll_squirrel_card,
            R.id.ll_frog_card
        )
        val avatarNames = arrayOf("Cat", "Dog", "Bear", "Squirrel", "Frog")

        // 把所有card view找出来放到list里, click时高亮一个取消其他的
        val avatarCards = ArrayList<LinearLayout>()
        for (i in avatarCardIds.indices) {
            val card = findViewById<LinearLayout>(avatarCardIds[i])
            avatarCards.add(card)
        }

        // 给每个card绑click listener
        for (i in avatarCards.indices) {
            // 用local variable idx, 因为listener后面才执行
            val idx = i
            avatarCards[idx].setOnClickListener {
                // 保存选中的avatar
                selectedAvatar = avatarNames[idx]

                // 全部重置成默认background
                for (j in avatarCards.indices) {
                    avatarCards[j].setBackgroundResource(R.drawable.shape_avatar_card)
                }
                // 高亮选中的那个
                avatarCards[idx].setBackgroundResource(R.drawable.shape_avatar_selected)

                Toast.makeText(this, "${avatarNames[idx]} selected!", Toast.LENGTH_SHORT).show()
            }
        }

        // editMode = 从Main进来改自己资料, 不是第一次填
        // true的话: 把现有资料prefill进去, 按钮文字也改一下
        val editMode = intent.getBooleanExtra("editMode", false)
        if (editMode) {
            etName.setText(IslandDataStore.getPlayerName())
            selectedAvatar = IslandDataStore.getPlayerAvatar()

            // 先全部重置, 再高亮现在选的avatar
            for (j in avatarCards.indices) {
                avatarCards[j].setBackgroundResource(R.drawable.shape_avatar_card)
            }
            for (j in avatarNames.indices) {
                if (avatarNames[j] == selectedAvatar) {
                    avatarCards[j].setBackgroundResource(R.drawable.shape_avatar_selected)
                }
            }

            // 按钮文字从"Stamp My Passport!"改成"Save Changes"
            val btnSubmit = findViewById<TextView>(R.id.btn_submit)
            btnSubmit.text = "Save Changes"
        }

        // submit按钮: 检查名字不为空, 然后存资料
        llSubmit.setOnClickListener {
            val name = etName.text.toString()
            if (name.isEmpty()) {
                // 空名字不行
                Toast.makeText(this, "Please enter your nickname!", Toast.LENGTH_SHORT).show()
            } else {
                // 不管第一次填还是编辑, 都先把资料存进data store
                IslandDataStore.setPlayer(name, selectedAvatar)

                if (editMode) {
                    // 从Main进来改的, 存好就返回, Main会自己刷新
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // 第一次填, 进岛
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("name", name)
                    intent.putExtra("avatar", selectedAvatar)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
