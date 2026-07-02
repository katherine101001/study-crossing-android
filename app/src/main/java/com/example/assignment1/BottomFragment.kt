package com.example.assignment1

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.assignment1.data.model.GameData
import com.example.assignment1.data.model.Subject
import com.example.assignment1.databinding.FragmentBottomBinding

// BottomFragment - StudyActivity的下半部分
// 四种mini game都在这里:
//   1. Math  -> slot machine (转盘密码锁)
//   2. English -> conveyor belt (传送带拖单词)
//   3. GK    -> 化石挖掘
//   4. 通用   -> multiple choice (选择题)
// 答对了就跳RewardActivity, 错了就扣apple
class BottomFragment : Fragment() {

    // ViewBinding, fragment里面可以用
    private var _binding: FragmentBottomBinding? = null
    private val binding get() = _binding!!

    // 从StudyActivity传过来的参数
    private var questionId = 0
    private var subject = Subject.MATH

    private var gameSolved = false

    // 每个game自己的状态变量
    // GK化石相关
    private var fossilSolved = false
    private var correctTagAnswer = ""
    private var dialogueRevertGen = 0

    // English传送带相关
    @Volatile private var trackRunning = false   // conveyor belt是否在滚动
    private val scrollSpeed = 2f                 // 每次移动的像素
    private var trackGeneration = 0              // 用来cancel旧的scroll thread
    private var correctWord = ""
    private val wordViews = ArrayList<TextView>()
    private val wordOrigins = HashMap<View, Float>()

    // Math slot machine
    private var mathTarget = ""
    private var digit1 = 0
    private var digit2 = 0
    private var digit3 = 0

    // Multiple choice
    private var mcCorrectIndex = -1
    private var mcSelectedIndex = -1
    private var mcTiles = ArrayList<View>()

    // 用来cancel auto-revert timer的计数器
    private var revertGeneration = 0

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 读取StudyActivity传过来的参数
        val subjectStr = arguments?.getString("subject") ?: "Math"
        subject = Subject.fromString(subjectStr)
        questionId = arguments?.getInt("questionId", 0) ?: 0

        // 找不到quest就显示个提示然后return
        val quest = IslandDataStore.getQuestion(questionId)
        if (quest == null) {
            binding.tvGameTitle.text = "Quest not found"
            return
        }

        // 根据GameData的subclass来决定显示哪个mini game
        // 先把所有section都隐藏，然后只开对应的那个
        val game = quest.game

        if (game is GameData.MultipleChoiceData) {
            // MC占屏大，不需要game title
            // 点tile直接auto advance, Done button也不要
            binding.tvGameTitle.visibility = View.GONE
            binding.llMathSection.visibility = View.GONE
            binding.llEnglishSection.visibility = View.GONE
            binding.llGkPlaceholder.visibility = View.GONE
            binding.llMcSection.visibility = View.VISIBLE
            binding.llBtnDone.visibility = View.GONE

            // MC section是English和GK共用的, English科目把ribbon换成蓝色, 其它保持原样
            if (subject == Subject.ENGLISH) {
                binding.flMcRibbon.setBackgroundResource(R.drawable.shape_english_ribbon)
            }

            setupMultipleChoice(game)

        } else if (game is GameData.SlotMachineData) {
            // math game title拿掉, 让vault下面slot machine有更多space
            binding.tvGameTitle.visibility = View.GONE
            binding.llMathSection.visibility = View.VISIBLE
            binding.llEnglishSection.visibility = View.GONE
            binding.llGkPlaceholder.visibility = View.GONE
            binding.llMcSection.visibility = View.GONE

            setupSlotMachine(game)

        } else if (game is GameData.WordTrackData) {
            // conveyor belt不用Done按钮，drag到drop zone就自动submit
            binding.tvGameTitle.visibility = View.GONE
            binding.llMathSection.visibility = View.GONE
            binding.llEnglishSection.visibility = View.VISIBLE
            binding.llGkPlaceholder.visibility = View.GONE
            binding.llMcSection.visibility = View.GONE
            binding.llBtnDone.visibility = View.GONE

            setupWordTrack(game)

        } else if (game is GameData.FossilExcavationData) {
            // GK也不用Done按钮
            binding.tvGameTitle.visibility = View.GONE
            binding.llMathSection.visibility = View.GONE
            binding.llEnglishSection.visibility = View.GONE
            binding.llGkPlaceholder.visibility = View.VISIBLE
            binding.llMcSection.visibility = View.GONE
            binding.llBtnDone.visibility = View.GONE

            setupFossilExcavation(game)
        }

        // Done按钮 - math用它来check vault, 其他game type直接submit
        // wrapper和里面的button都绑同一个listener, 因为两个都能点到
        val doneClick = View.OnClickListener {
            if (quest.game is GameData.SlotMachineData && !gameSolved) {
                // math vault有自己的check逻辑
                checkVaultAnswer()
            } else {
                submitAndGoToReward()
            }
        }
        binding.llBtnDone.setOnClickListener(doneClick)
        binding.btnDone.setOnClickListener(doneClick)
    }

    // 提交答案, 算分数, 跳RewardActivity
    // 对了=95分, 错了/没做=55分, Reward那边再转成letter grade
    private fun submitAndGoToReward() {
        val quest = IslandDataStore.getQuestion(questionId)

        // 检查这个game type是否真的solved了
        var solved = false
        if (quest != null) {
            val game = quest.game
            if (game is GameData.MultipleChoiceData) {
                solved = gameSolved
            } else if (game is GameData.SlotMachineData) {
                solved = gameSolved
            } else if (game is GameData.WordTrackData) {
                solved = gameSolved
            } else {
                // fossil game用的是fossilSolved, 不是gameSolved
                solved = fossilSolved
            }
        }

        var score = 55
        if (solved) {
            score = 95
        }

        val intent = Intent(requireContext(), RewardActivity::class.java)
        intent.putExtra("score", score)
        intent.putExtra("subject", subject.displayName)
        // 把questionId也传过去, Reward那边答对了好markDone
        intent.putExtra("questionId", questionId)
        requireActivity().startActivity(intent)
        requireActivity().finish()
    }

    // ==================== MATH SLOT MACHINE ====================

    // 初始化3个digit reel, 随机起始数字, 绑drag事件
    private fun setupSlotMachine(data: GameData.SlotMachineData) {
        mathTarget = data.targetNumber

        // 显示题目
        if (data.questionText.isNotEmpty()) {
            binding.tvMathQuestion.text = data.questionText
        } else {
            binding.tvMathQuestion.text = "Enter the answer!"
        }

        // 更新NPC对话
        val topFrag = getTopFragment()
        if (topFrag != null) {
            topFrag.setDialogue("Solve it in your head, then dial the reels!")
        }

        // 随机初始数字 0-9
        digit1 = (0..9).random()
        digit2 = (0..9).random()
        digit3 = (0..9).random()
        updateReelDisplay(1)
        updateReelDisplay(2)
        updateReelDisplay(3)

        styleSlotDigits()

        // 18dp = 手指需要拖动的垂直距离才跳一个数字, 越小越灵敏
        val thresholdPx = Utils.dpToPx(requireContext(), 18)
        setupReelDrag(binding.slotFrame1, 1, thresholdPx)
        setupReelDrag(binding.slotFrame2, 2, thresholdPx)
        setupReelDrag(binding.slotFrame3, 3, thresholdPx)
    }

    // 设置digit的文字样式: 主数字白字加阴影, ghost数字小一点半透明
    private fun styleSlotDigits() {
        val density = resources.displayMetrics.density

        // 3个主数字
        val mainDigits = arrayOf(
            binding.slotDigit1,
            binding.slotDigit2,
            binding.slotDigit3
        )
        for (i in mainDigits.indices) {
            val tv = mainDigits[i]
            tv.setTextColor(Color.WHITE)
            tv.textSize = 32f
            tv.setShadowLayer(4f * density, 0f, 2.5f * density, 0xCC000000.toInt())
            tv.setBackgroundResource(0)
            tv.setPadding(0, 0, 0, 0)
        }

        // 6个ghost数字 (每个reel的prev和next)
        val ghostDigits = arrayOf(
            binding.slotPrev1, binding.slotNext1,
            binding.slotPrev2, binding.slotNext2,
            binding.slotPrev3, binding.slotNext3
        )
        for (i in ghostDigits.indices) {
            val tv = ghostDigits[i]
            tv.setTextColor(0x88FFFFFF.toInt())
            tv.textSize = 18f
            tv.setShadowLayer(0f, 0f, 0f, 0)
            tv.setBackgroundResource(0)
            tv.setPadding(0, 0, 0, 0)
        }
    }

    // 给reel frame绑touch listener, 上下拖改变数字
    // 往上拖 -> digit+1, 往下拖 -> digit-1
    private fun setupReelDrag(frame: FrameLayout, reelIndex: Int, thresholdPx: Int) {
        // 用size=1的array来存状态, 因为lambda里面要修改
        val lastY = floatArrayOf(0f)
        val accumulatedDy = floatArrayOf(0f)

        frame.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                lastY[0] = event.y
                accumulatedDy[0] = 0f
                return@setOnTouchListener true

            } else if (event.action == MotionEvent.ACTION_MOVE) {
                val dy = event.y - lastY[0]
                accumulatedDy[0] = accumulatedDy[0] + dy
                lastY[0] = event.y

                // 累积到threshold就跳一个数字
                while (accumulatedDy[0] >= thresholdPx) {
                    accumulatedDy[0] = accumulatedDy[0] - thresholdPx
                    changeDigit(reelIndex, -1)
                }
                while (accumulatedDy[0] <= -thresholdPx) {
                    accumulatedDy[0] = accumulatedDy[0] + thresholdPx
                    changeDigit(reelIndex, 1)
                }
                return@setOnTouchListener true

            } else if (event.action == MotionEvent.ACTION_UP ||
                       event.action == MotionEvent.ACTION_CANCEL) {
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
    }

    // 改一个reel的数字, delta是+1或-1, 带wrap-around (0-9循环)
    private fun changeDigit(index: Int, delta: Int) {
        if (index == 1) {
            digit1 = (digit1 + delta + 10) % 10
        } else if (index == 2) {
            digit2 = (digit2 + delta + 10) % 10
        } else if (index == 3) {
            digit3 = (digit3 + delta + 10) % 10
        }
        updateReelDisplay(index)

        // 如果玩家之前答对了, 又在转reel, 就隐藏绿色的correct提示
        val b = _binding
        if (b != null) {
            b.tvMathResult.visibility = View.GONE
        }
    }

    // 刷新一个reel的三个TextView (prev, current, next)
    private fun updateReelDisplay(index: Int) {
        val b = _binding ?: return

        // 找出当前reel的digit
        var digit = digit3
        if (index == 1) {
            digit = digit1
        } else if (index == 2) {
            digit = digit2
        }

        // 计算上下数字 (0-9 wrap)
        val prev = (digit + 9) % 10
        val next = (digit + 1) % 10

        if (index == 1) {
            b.slotPrev1.text = prev.toString()
            b.slotDigit1.text = digit.toString()
            b.slotNext1.text = next.toString()
        } else if (index == 2) {
            b.slotPrev2.text = prev.toString()
            b.slotDigit2.text = digit.toString()
            b.slotNext2.text = next.toString()
        } else if (index == 3) {
            b.slotPrev3.text = prev.toString()
            b.slotDigit3.text = digit.toString()
            b.slotNext3.text = next.toString()
        }
    }

    // 玩家按Done时检查vault密码对不对
    private fun checkVaultAnswer() {
        val b = _binding ?: return
        val combo = "$digit1$digit2$digit3"

        if (combo == mathTarget) {
            // 答对了!
            gameSolved = true

            val topFrag = getTopFragment()
            if (topFrag != null) {
                topFrag.showWinFace()
                topFrag.setDialogue("You cracked the vault! Well done!")
            }

            b.tvMathResult.text = "CORRECT! The vault opens!"
            b.tvMathResult.visibility = View.VISIBLE
            b.tvDragHint.text = "Well done!"
            Toast.makeText(requireContext(), "The vault opens! You cracked it!", Toast.LENGTH_LONG).show()

            // 等1.5秒让玩家看到结果, 然后跳reward
            Utils.runAfterDelay(activity, 1500) {
                if (_binding != null) {
                    submitAndGoToReward()
                }
            }
        } else {
            // 错了, 扣一个apple
            val act = activity
            if (act is StudyActivity) {
                act.deductApple()
            }

            val topFrag = getTopFragment()
            if (topFrag != null) {
                topFrag.setDialogue("Wrong: $combo — try again!")
            }

            // 2.5秒后恢复NPC对话, 除非又错了(用generation counter取消)
            val myGen = revertGeneration + 1
            revertGeneration = myGen
            Utils.runAfterDelay(activity, 2500) {
                if (_binding != null && revertGeneration == myGen) {
                    val tf = getTopFragment()
                    if (tf != null) {
                        tf.setDialogue("Solve it in your head, then dial the reels!")
                    }
                }
            }
        }
    }

    // ==================== ENGLISH WORD TRACK ====================

    // 在conveyor belt上创建word chips, 开始滚动
    private fun setupWordTrack(data: GameData.WordTrackData) {
        correctWord = data.correctWord

        // conveyor belt不用Done按钮
        binding.llBtnDone.visibility = View.GONE

        val topFrag = getTopFragment()
        if (topFrag != null) {
            topFrag.setDialogue("Now fill in the blank! Drag the correct word into the box above.")
        }

        // 显示填空句子
        binding.tvQuestionSentence.text = data.sentence

        // 重置drop zone
        binding.tvDropZone.text = "Drop word here"
        binding.tvDropZone.setBackgroundResource(R.drawable.shape_letter_dropzone)
        binding.tvDropZone.setTextColor(resources.getColor(R.color.deep_wood, null))

        // 清空belt上的旧chip
        val track = binding.wordTrack
        track.removeAllViews()
        wordViews.clear()

        // 随机打乱word顺序
        val shuffled = data.options.shuffled()

        // 创建chip, 所有chip共用一个touch listener
        val touchListener = WordTouchListener()
        for (i in shuffled.indices) {
            val word = shuffled[i]
            val tv = TextView(requireContext())
            tv.text = word
            tv.textSize = 17f
            tv.setTextColor(resources.getColor(R.color.ink_brown, null))
            tv.typeface = resources.getFont(R.font.nunito_bold)
            tv.gravity = Gravity.CENTER

            // word stamp样式: 奶油色纸+绿色边框
            tv.setBackgroundResource(R.drawable.shape_word_chip)
            // bottom padding稍微大一点, 防止drawable的bezel裁到文字
            val padH = Utils.dpToPx(requireContext(), 26)
            val padTop = Utils.dpToPx(requireContext(), 12)
            val padBot = Utils.dpToPx(requireContext(), 15)
            tv.setPadding(padH, padTop, padH, padBot)
            tv.minWidth = Utils.dpToPx(requireContext(), 94)
            tv.elevation = 10f
            tv.setOnTouchListener(touchListener)

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER_VERTICAL
            tv.layoutParams = params

            track.addView(tv)
            wordViews.add(tv)
        }

        // 等belt measure完之后再放置chips并开始scroll
        track.post {
            if (_binding == null) {
                return@post
            }
            val trackWidth = track.width.toFloat()
            if (trackWidth <= 0f || wordViews.isEmpty()) {
                return@post
            }
            val spacing = computeChipSpacing(trackWidth)
            for (i in wordViews.indices) {
                wordViews[i].translationX = spacing * i
                wordViews[i].translationY = 0f
            }
            startScrollThread()
        }
    }

    // 算chip之间的间距: 用最宽的chip + 36dp, 保证不重叠
    private fun computeChipSpacing(trackWidth: Float): Float {
        val gapPx = Utils.dpToPxF(requireContext(), 36f)

        // 找最宽的chip, 用plain loop不用maxOfOrNull
        var widest = 0
        for (i in wordViews.indices) {
            if (wordViews[i].width > widest) {
                widest = wordViews[i].width
            }
        }
        val byContent = widest.toFloat() + gapPx

        // 如果chip还没measure好, 用track宽度做fallback
        val byTrack = trackWidth / 2.0f
        val byTrackScaled = byTrack * 0.55f

        if (byContent > byTrackScaled) {
            return byContent
        } else {
            return byTrackScaled
        }
    }

    // 后台thread: 每40ms把所有chip往左移scrollSpeed像素
    // 移出屏幕左边的chip放到最右边循环
    // 用generation counter来安全地停止旧的thread
    private fun startScrollThread() {
        trackRunning = true
        val myGen = trackGeneration + 1
        trackGeneration = myGen

        Thread {
            while (trackRunning && trackGeneration == myGen) {
                Thread.sleep(40)

                val b = _binding
                if (b == null) {
                    break
                }
                if (!trackRunning || trackGeneration != myGen || activity == null) {
                    break
                }
                activity?.runOnUiThread {
                    if (_binding == null || trackGeneration != myGen) {
                        return@runOnUiThread
                    }
                    val track = b.wordTrack
                    val trackWidth = track.width.toFloat()
                    if (trackWidth <= 0f || wordViews.isEmpty()) {
                        return@runOnUiThread
                    }
                    val spacing = computeChipSpacing(trackWidth)

                    // 每个chip左移, 正在被drag的跳过
                    for (i in wordViews.indices) {
                        val w = wordViews[i]
                        if (w.tag == "dragging") {
                            continue
                        }
                        w.translationX = w.translationX - scrollSpeed

                        // 半个chip出了左边就放到最右边
                        if (w.translationX + (w.width / 2f) < 0f) {
                            var rightmost = w.translationX
                            for (j in wordViews.indices) {
                                if (wordViews[j].translationX > rightmost) {
                                    rightmost = wordViews[j].translationX
                                }
                            }
                            w.translationX = rightmost + spacing
                        }
                    }
                }
            }
        }.start()
    }

    // 获取activity的content view (FrameLayout), 用来当drag overlay
    // 把chip加到这里面才能浮在所有东西上面
    private fun dragOverlay(): ViewGroup? {
        return activity?.findViewById<ViewGroup>(android.R.id.content)
    }

    // WordTouchListener - 处理chip的拖拽
    //   DOWN: 把chip从belt提到overlay上
    //   MOVE: 手指移动chip
    //   UP:   检查是否在drop zone上
    private inner class WordTouchListener : View.OnTouchListener {
        private var touchOffsetX = 0f
        private var touchOffsetY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val wordView = v as TextView
            val b = _binding ?: return false

            if (event.action == MotionEvent.ACTION_DOWN) {
                // 记录手指在chip内部的位置, 防止chip跳位
                touchOffsetX = event.x
                touchOffsetY = event.y
                wordView.tag = "dragging"
                wordView.elevation = 32f
                wordOrigins[wordView] = wordView.translationX

                // 把chip从belt移到overlay, 这样它才能浮在所有view上面
                val track = b.wordTrack
                val overlay = dragOverlay() ?: return false

                val screenLoc = IntArray(2)
                track.getLocationOnScreen(screenLoc)
                val screenX = screenLoc[0] + wordView.left + wordView.translationX
                val screenY = screenLoc[1] + wordView.top + wordView.translationY

                val parent = wordView.parent as? ViewGroup
                if (parent != null) {
                    parent.removeView(wordView)
                }
                wordView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                overlay.addView(wordView)

                // screen坐标转成overlay坐标
                val overlayLoc = IntArray(2)
                overlay.getLocationOnScreen(overlayLoc)
                wordView.translationX = screenX - overlayLoc[0] - wordView.left
                wordView.translationY = screenY - overlayLoc[1] - wordView.top
                return true

            } else if (event.action == MotionEvent.ACTION_MOVE) {
                val overlay = dragOverlay() ?: return false
                val overlayLoc = IntArray(2)
                overlay.getLocationOnScreen(overlayLoc)
                wordView.translationX = event.rawX - touchOffsetX - overlayLoc[0] - wordView.left
                wordView.translationY = event.rawY - touchOffsetY - overlayLoc[1] - wordView.top
                return true

            } else if (event.action == MotionEvent.ACTION_UP) {
                wordView.elevation = 0f
                checkDrop(wordView, event.rawX, event.rawY)
                return true
            }
            return false
        }
    }

    // 检查拖拽释放时chip是否在drop zone上面
    // 如果在: 判断对错; 如果不在: 放回belt
    private fun checkDrop(wordView: TextView, rawX: Float, rawY: Float) {
        val b = _binding ?: return
        val dropZone = b.tvDropZone

        var landed = false

        if (dropZone.visibility == View.VISIBLE) {
            // 获取drop zone的屏幕坐标
            val loc = IntArray(2)
            dropZone.getLocationOnScreen(loc)

            val inZoneX = rawX >= loc[0] && rawX <= loc[0] + dropZone.width
            val inZoneY = rawY >= loc[1] && rawY <= loc[1] + dropZone.height
            val inZone = inZoneX && inZoneY

            if (inZone) {
                landed = true
                val word = wordView.text.toString()

                if (word == correctWord) {
                    // 答对了!
                    gameSolved = true
                    trackRunning = false      // 停下conveyor belt
                    wordView.visibility = View.INVISIBLE

                    dropZone.text = word
                    dropZone.setBackgroundResource(R.drawable.shape_rounded_green)
                    dropZone.setTextColor(Color.WHITE)

                    binding.tvAnswerFeedback.text = "Correct! Well done!"
                    binding.tvAnswerFeedback.visibility = View.VISIBLE

                    val topFrag = getTopFragment()
                    if (topFrag != null) {
                        topFrag.showWinFace()
                        topFrag.setDialogue("Perfect! That's the right word!")
                    }

                    autoAdvanceToReward()
                    return

                } else {
                    // 错了
                    val act = activity
                    if (act is StudyActivity) {
                        act.deductApple()
                    }
                    returnToTrack(wordView)

                    val topFrag = getTopFragment()
                    if (topFrag != null) {
                        topFrag.showWrongFace()
                        topFrag.setDialogue("Hmm, that doesn't seem right... try again!")
                    }
                    scheduleNpcRevert()
                }
            }
        }

        // 没放在drop zone上, 放回belt
        if (!landed) {
            returnToTrack(wordView)
        }
    }

    // 等1.8秒让玩家看到"Correct!"然后自动跳reward
    private fun autoAdvanceToReward() {
        Utils.runAfterDelay(activity, 1800) {
            if (_binding != null) {
                submitAndGoToReward()
            }
        }
    }

    // 把chip放回conveyor belt
    private fun returnToTrack(wordView: TextView) {
        val b = _binding ?: return
        wordView.tag = null

        // 从overlay中移除
        val parent = wordView.parent as? ViewGroup
        if (parent != null) {
            parent.removeView(wordView)
        }

        // 放回belt, 用原来的layout params
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER_VERTICAL
        wordView.layoutParams = lp
        b.wordTrack.addView(wordView)

        // 恢复到之前的位置, 找不到就放最右边
        val savedX = wordOrigins.remove(wordView)
        if (savedX != null) {
            wordView.translationX = savedX
        } else {
            val trackWidth = b.wordTrack.width.toFloat()
            wordView.translationX = trackWidth - wordView.width
        }
        wordView.translationY = 0f
    }

    // 2.5秒后恢复NPC正常表情, generation counter防冲突
    private fun scheduleNpcRevert() {
        val myGen = revertGeneration + 1
        revertGeneration = myGen
        Utils.runAfterDelay(activity, 2500) {
            if (_binding != null && revertGeneration == myGen) {
                val tf = getTopFragment()
                if (tf != null) {
                    tf.showNormalFace()
                    tf.setDialogue("Now fill in the blank! Drag the correct word into the box above.")
                }
            }
        }
    }

    // ==================== MULTIPLE CHOICE ====================

    // 填题目+tile (4-6个选项都support), 绑click事件
    private fun setupMultipleChoice(data: GameData.MultipleChoiceData) {
        mcCorrectIndex = data.correctIndex
        mcSelectedIndex = -1

        binding.tvMcPrompt.text = data.prompt

        // 6个固定tile slot (A-F), 有几个option就填几个, 多余的GONE
        // 前2排A/B/C/D总是visible, 第3排E/F只在5-6选项时才出现
        val tileText = arrayOf(
            binding.tvMcAText,
            binding.tvMcBText,
            binding.tvMcCText,
            binding.tvMcDText,
            binding.tvMcEText,
            binding.tvMcFText
        )
        val tileWrap = arrayOf(
            binding.tileMcA,
            binding.tileMcB,
            binding.tileMcC,
            binding.tileMcD,
            binding.tileMcE,
            binding.tileMcF
        )

        mcTiles.clear()

        for (i in tileText.indices) {
            if (i < data.options.size) {
                tileText[i].text = data.options[i].text
                tileWrap[i].visibility = View.VISIBLE
                mcTiles.add(tileWrap[i])
            } else {
                tileWrap[i].visibility = View.GONE
            }
        }

        // 第3排只在>4个option时才显示
        if (data.options.size > 4) {
            binding.mcRow3.visibility = View.VISIBLE
            binding.mcRow3Spacer.visibility = View.VISIBLE
        } else {
            binding.mcRow3.visibility = View.GONE
            binding.mcRow3Spacer.visibility = View.GONE
        }

        // 绑click, 用local variable idx确保lambda capture正确的值
        for (i in mcTiles.indices) {
            val idx = i
            mcTiles[i].setOnClickListener {
                selectMcTile(idx)
            }
        }
    }

    // 玩家选了一个option, 高亮它然后判断对错
    private fun selectMcTile(index: Int) {
        // 已经solved了就不再响应click
        if (gameSolved) {
            return
        }

        mcSelectedIndex = index

        // 只高亮选中的tile
        for (i in mcTiles.indices) {
            if (i == index) {
                mcTiles[i].isSelected = true
            } else {
                mcTiles[i].isSelected = false
            }
        }

        if (index == mcCorrectIndex) {
            gameSolved = true
            binding.tvMcFeedback.text = "Correct! Well done!"
            binding.tvMcFeedback.visibility = View.VISIBLE

            // 没有Done按钮了, 答对了直接跳reward (跟english/gk一样)
            val topFrag = getTopFragment()
            if (topFrag != null) {
                topFrag.showWinFace()
                topFrag.setDialogue("Nice pick! That's the right one!")
            }
            autoAdvanceToReward()

        } else {
            val act = activity
            if (act is StudyActivity) {
                act.deductApple()
            }
            // 错误选项短暂高亮400ms然后取消, 让玩家重试
            Utils.runAfterDelay(activity, 400) {
                if (_binding != null) {
                    for (i in mcTiles.indices) {
                        mcTiles[i].isSelected = false
                    }
                    mcSelectedIndex = -1
                }
            }
        }
    }

    // ==================== GK FOSSIL EXCAVATION ====================

    // 填4个label tag, 设置dirt overlay, 绑tag的drag事件
    private fun setupFossilExcavation(data: GameData.FossilExcavationData) {
        correctTagAnswer = data.correctTag

        // 4个tag slot, 和MC tiles一样的做法
        val tagViews = arrayOf(
            binding.tag1,
            binding.tag2,
            binding.tag3,
            binding.tag4
        )
        for (i in tagViews.indices) {
            if (i < data.options.size) {
                tagViews[i].text = data.options[i]
                tagViews[i].visibility = View.VISIBLE
            } else {
                tagViews[i].visibility = View.GONE
            }
        }

        // 有hint text就用
        if (data.hintText.isNotEmpty()) {
            binding.tvFossilHint.text = data.hintText
        }

        val topFrag = getTopFragment()
        if (topFrag != null) {
            topFrag.setDialogue("Scratch the dirt, then drag the right label!")
        }

        // 显示化石图片
        binding.ivFossilImage.setImageResource(data.fossilImageRes)

        // 设置scratch overlay: 单张大图, 40dp笔刷, 12dp圆角
        val scratch = binding.scratchView
        scratch.setDirtImageSingle(R.drawable.soil)
        scratch.setBrushSizeDp(40f)
        scratch.setCornerRadiusDp(12f)
        scratch.visibility = View.VISIBLE
        binding.ivFossilImage.visibility = View.VISIBLE

        // 给4个tag绑drag listener
        val tagListener = GkTagTouchListener()
        for (i in tagViews.indices) {
            tagViews[i].setOnTouchListener(tagListener)
        }
    }

    // 拖tag时记下它原本在answer row里的位置, 弹回去时按这个index塞回
    private val tagOriginIndex = HashMap<View, Int>()

    // GK fossil标签的拖拽处理
    // 之前tag在answer row里面拖, fossil image有elevation会把tag盖住
    // 现在跟english word chip一样, 拖的时候挪到activity overlay上, 浮在所有view上面
    private inner class GkTagTouchListener : View.OnTouchListener {
        private var touchOffsetX = 0f
        private var touchOffsetY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val tag = v as TextView
            val b = _binding ?: return false

            if (event.action == MotionEvent.ACTION_DOWN) {
                touchOffsetX = event.x
                touchOffsetY = event.y
                tag.elevation = 32f

                // 把tag从answer row搬到overlay, 这样drag时不会被fossil image盖住
                val overlay = dragOverlay() ?: return false
                val row = b.llAnswerTags
                tagOriginIndex[tag] = row.indexOfChild(tag)

                // 记一下tag在screen上的位置, 搬过去后保持原位不跳
                val screenLoc = IntArray(2)
                tag.getLocationOnScreen(screenLoc)

                row.removeView(tag)
                val lp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                tag.layoutParams = lp
                overlay.addView(tag)

                // screen坐标转成overlay坐标
                val overlayLoc = IntArray(2)
                overlay.getLocationOnScreen(overlayLoc)
                tag.translationX = (screenLoc[0] - overlayLoc[0] - tag.left).toFloat()
                tag.translationY = (screenLoc[1] - overlayLoc[1] - tag.top).toFloat()
                return true

            } else if (event.action == MotionEvent.ACTION_MOVE) {
                // 在overlay上跟手指走
                val overlay = dragOverlay() ?: return false
                val overlayLoc = IntArray(2)
                overlay.getLocationOnScreen(overlayLoc)
                tag.translationX = event.rawX - touchOffsetX - overlayLoc[0] - tag.left
                tag.translationY = event.rawY - touchOffsetY - overlayLoc[1] - tag.top
                return true

            } else if (event.action == MotionEvent.ACTION_UP) {
                tag.elevation = 0f
                checkGkTagDrop(tag, event.rawX, event.rawY)
                return true
            }
            return false
        }
    }

    // tag放错 / 没放到fossil上时, 把它放回answer row原本的位置
    private fun returnTagToRow(tag: TextView) {
        val b = _binding ?: return

        val parent = tag.parent as? ViewGroup
        if (parent != null) {
            parent.removeView(tag)
        }

        // 还原原本的LinearLayout layout params (跟xml里一样, weight=1, 4dp margin)
        val marginPx = Utils.dpToPx(requireContext(), 4)
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(marginPx, marginPx, marginPx, marginPx)
        tag.layoutParams = lp
        tag.translationX = 0f
        tag.translationY = 0f

        val idx = tagOriginIndex[tag] ?: b.llAnswerTags.childCount
        b.llAnswerTags.addView(tag, idx)
        tagOriginIndex.remove(tag)
    }

    // 检查GK tag是否拖到了fossil区域
    private fun checkGkTagDrop(tag: TextView, rawX: Float, rawY: Float) {
        val b = _binding ?: return

        // 检查是否在fossil display frame范围内
        val fossilLoc = IntArray(2)
        b.flFossilArea.getLocationOnScreen(fossilLoc)

        val inX = rawX >= fossilLoc[0] && rawX <= fossilLoc[0] + b.flFossilArea.width
        val inY = rawY >= fossilLoc[1] && rawY <= fossilLoc[1] + b.flFossilArea.height
        val inFossil = inX && inY

        if (inFossil) {
            val answer = tag.text.toString()
            if (answer == correctTagAnswer) {
                fossilSolved = true
                b.scratchView.visibility = View.GONE

                val topFrag = getTopFragment()
                if (topFrag != null) {
                    topFrag.showWinFace()
                    topFrag.setDialogue("A fine discovery! It's a $correctTagAnswer!")
                }

                // 短暂显示win face后跳reward
                Utils.runAfterDelay(activity, 1600) {
                    if (_binding != null) {
                        submitAndGoToReward()
                    }
                }
            } else {
                val act = activity
                if (act is StudyActivity) {
                    act.deductApple()
                }

                val topFrag = getTopFragment()
                if (topFrag != null) {
                    topFrag.showWrongFace()
                    topFrag.setDialogue("Hmm, that doesn't look right. Keep digging!")
                }

                // schedule NPC表情恢复
                val myGen = dialogueRevertGen + 1
                dialogueRevertGen = myGen
                Utils.runAfterDelay(activity, 2500) {
                    if (_binding != null && dialogueRevertGen == myGen) {
                        val tf = getTopFragment()
                        if (tf != null) {
                            tf.showNormalFace()
                            tf.setDialogue("Scratch the dirt, then drag the right label!")
                        }
                    }
                }
                // 错的tag从overlay放回answer row
                returnTagToRow(tag)
            }
        } else {
            // 没放在fossil区域, 一样放回去
            returnTagToRow(tag)
        }
    }

    // 给外部(GK check)用的
    fun isGkSolved(): Boolean {
        return fossilSolved
    }

    // ---------- helpers ----------

    // 找到TopFragment, 用来call它的public方法(改NPC表情/对话)
    private fun getTopFragment(): TopFragment? {
        val act = activity ?: return null
        val frag = act.supportFragmentManager.findFragmentById(R.id.container_top)
        if (frag is TopFragment) {
            return frag
        }
        return null
    }

    // 清理: 停止conveyor belt thread, 释放binding
    override fun onDestroyView() {
        super.onDestroyView()
        trackRunning = false
        trackGeneration = trackGeneration + 1
        _binding = null
    }
}
