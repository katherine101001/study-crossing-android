package com.example.assignment1

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment1.data.model.Subject

// Utils - 把项目里到处copy-paste的小工具函数集中到这里
// 每个Activity就不用重复写一样的代码了
object Utils {

    // dp转px: android用px, 但design给的是dp
    // px = dp * density
    fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun dpToPxF(context: Context, dp: Float): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }

    // applyEdgeInsets - 处理status bar和nav bar的padding
    // 每个screen都要写一遍这段, 所以抽出来
    // rootId = 最外层的LinearLayout id
    // appBarId = wood/green header的id, 没有就传0
    // extraTopDp = 有些screen需要在header下面额外加padding, 不需要就传0
    fun applyEdgeInsets(activity: Activity, rootId: Int, appBarId: Int, extraTopDp: Int) {
        val root = activity.findViewById<View>(rootId)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // root view用left/right/bottom insets, top留给app bar处理
            v.setPadding(bars.left, 0, bars.right, bars.bottom)

            if (appBarId != 0) {
                val appBar = v.findViewById<View>(appBarId)
                if (appBar != null) {
                    if (extraTopDp > 0) {
                        // 有extra padding时保留原有的left/right/bottom, 只改top
                        val basePx = dpToPx(activity, extraTopDp)
                        appBar.setPadding(
                            appBar.paddingLeft,
                            bars.top + basePx,
                            appBar.paddingRight,
                            appBar.paddingBottom
                        )
                    } else {
                        appBar.setPadding(0, bars.top, 0, 0)
                    }
                }
            }

            insets
        }
    }

    // 设置toolbar: 绑back箭头, 隐藏默认title (因为xml里有自定义title)
    fun setupBackToolbar(activity: AppCompatActivity, toolbarId: Int) {
        val toolbar = activity.findViewById<Toolbar>(toolbarId)
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // 用自定义title, 隐藏默认的
        activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        // 奶油色back chevron
        toolbar.setNavigationIcon(R.drawable.ic_back_chevron)
    }

    // runAfterDelay - Thread.sleep + runOnUiThread 实现delay
    // 用来做NPC表情恢复、自动跳reward、splash延迟之类的
    // 传activity是为了在screen被关掉时跳过callback
    fun runAfterDelay(activity: Activity?, delayMs: Long, action: () -> Unit) {
        Thread {
            Thread.sleep(delayMs)
            // activity可能是null (screen已经destroy了)
            if (activity != null && !activity.isFinishing) {
                activity.runOnUiThread {
                    action()
                }
            }
        }.start()
    }

    // Notice Board和Quest Archive的list里每行都有个NPC小头像
    // 两个adapter都在重复这段lookup, 所以放这里统一用
    fun getSubjectIconForList(subject: Subject): Int {
        if (subject == Subject.MATH) {
            return R.drawable.ic_npc_tanuki_normal
        } else if (subject == Subject.ENGLISH) {
            return R.drawable.ic_npc_isabelle_happy
        } else {
            // GK
            return R.drawable.ic_npc_owl_angry
        }
    }

    // 每个subject的小chip pill不同颜色 (MATH / ENGLISH / GK)
    fun getSubjectChipBg(subject: Subject): Int {
        if (subject == Subject.MATH) {
            return R.drawable.shape_subject_chip_math
        } else if (subject == Subject.ENGLISH) {
            return R.drawable.shape_subject_chip_english
        } else {
            return R.drawable.shape_subject_chip_gk
        }
    }

    // Study screen的toolbar颜色: Math红, English蓝, GK紫
    fun getSubjectToolbarColor(subject: Subject): Int {
        if (subject == Subject.MATH) {
            return R.color.chip_red
        } else if (subject == Subject.ENGLISH) {
            return R.color.chip_blue
        } else {
            return R.color.chip_purple
        }
    }

    // NPC全身像有3种表情, 3个subject = 9张图
    //   normal = 正常/idle
    //   sweat  = 答错时
    //   win    = 答对时
    // TopFragment用这些常量和helper来选图
    const val NPC_STATE_NORMAL = 0
    const val NPC_STATE_SWEAT = 1
    const val NPC_STATE_WIN = 2

    fun getNpcFullbody(subject: Subject, state: Int): Int {
        // 先判断subject, 再判断表情
        if (subject == Subject.MATH) {
            if (state == NPC_STATE_SWEAT) {
                return R.drawable.ic_npc_tanuki_sweat_fullbody
            } else if (state == NPC_STATE_WIN) {
                return R.drawable.ic_npc_tanuki_win_fullbody
            } else {
                return R.drawable.ic_npc_tanuki_normal_fullbody
            }
        } else if (subject == Subject.ENGLISH) {
            if (state == NPC_STATE_SWEAT) {
                return R.drawable.ic_npc_squirrel_sweat_fullbody
            } else if (state == NPC_STATE_WIN) {
                return R.drawable.ic_npc_squirrel_win_fullbody
            } else {
                return R.drawable.ic_npc_squirrel_normal_fullbody
            }
        } else {
            // GK = owl
            if (state == NPC_STATE_SWEAT) {
                return R.drawable.ic_npc_owl_sweat_fullbody
            } else if (state == NPC_STATE_WIN) {
                return R.drawable.ic_npc_owl_win_fullbody
            } else {
                return R.drawable.ic_npc_owl_normal_fullbody
            }
        }
    }

    // shrinkCorkIfFits - 如果RecyclerView内容不多, 就把cork board缩小
    // 避免屏幕上大片空的cork板子不好看
    // 先measure RecyclerView的高度, 如果比cork board小就改WRAP_CONTENT
    fun shrinkCorkIfFits(recyclerView: RecyclerView, corkBoard: FrameLayout) {
        // measure: 宽度固定, 高度不限
        val wSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        recyclerView.measure(wSpec, hSpec)
        val contentH = recyclerView.measuredHeight

        if (contentH > 0 && contentH < corkBoard.height) {
            // 内容够小, shrink cork board
            val p = corkBoard.layoutParams as LinearLayout.LayoutParams
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT
            p.weight = 0f
            corkBoard.layoutParams = p
            recyclerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            // 内容不需要scroll了
            recyclerView.isNestedScrollingEnabled = false
        }
    }
}
