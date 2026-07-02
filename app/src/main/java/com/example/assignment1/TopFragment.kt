package com.example.assignment1

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.example.assignment1.data.model.MediaItem
import com.example.assignment1.data.model.MediaType
import com.example.assignment1.data.model.Subject
import com.example.assignment1.databinding.FragmentTopBinding

// TopFragment - StudyActivity的上半部分
// Phase 1 (看lesson): 显示NPC全身像 + 对话气泡 + lesson面板
// Phase 2 (mini game): StudyActivity隐藏lesson面板, 只留NPC + 气泡
// 另外提供public方法让外部切换NPC表情 (normal/sweat/win)
class TopFragment : Fragment() {

    // Fragment用ViewBinding (project rules允许)
    private var _binding: FragmentTopBinding? = null
    private val binding get() = _binding!!

    // 从arguments里缓存的参数
    private var questionId = 0
    private var subject = Subject.MATH

    // 标准fragment lifecycle: inflate binding然后return root
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopBinding.inflate(inflater, container, false)
        return binding.root
    }

    // view创建好了, 开始填充内容
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 读取StudyActivity传的参数
        val subjectStr = arguments?.getString("subject") ?: "Math"
        subject = Subject.fromString(subjectStr)
        questionId = arguments?.getInt("questionId", 0) ?: 0

        // 从data store查quest数据
        val quest = IslandDataStore.getQuestion(questionId)
        val learning = quest?.learning

        // 设置NPC全身像, 不同subject不同NPC
        val npcRes = Utils.getNpcFullbody(subject, Utils.NPC_STATE_NORMAL)
        binding.ivNpcFullbody.setImageResource(npcRes)
        binding.ivNpcFullbody.visibility = View.VISIBLE

        // 这个quest有没有media (图片/video)? 没的话image frame整个藏起来
        // lesson text就拿到更多space, 不会显示默认那张blueprint/handbook/fossil图
        val media = learning?.mediaItems?.firstOrNull()
        val hasMedia = media != null && (media.resourceId != 0 || media.url.isNotEmpty())

        // lesson面板每个subject不一样, 显示对应的, 隐藏另外两个
        // 用if/else chain不用when, 好读
        if (subject == Subject.MATH) {
            // Math -> Tom Nook老师 + blueprint面板
            binding.tvNpcName.text = learning?.npcName ?: "Tom Nook"
            binding.tvDialogue.text = learning?.npcDialogue ?: "Read the lesson, then crack the vault!"
            binding.tvMathLesson.text = learning?.lessonText ?: "Solve the math problem."

            // 没media就藏掉blueprint image area, 留给text
            if (hasMedia) {
                binding.flMathImageArea.visibility = View.VISIBLE
                applyMedia(binding.ivMathDisplay, binding.vvMathDisplay, media)
            } else {
                binding.flMathImageArea.visibility = View.GONE
            }

            binding.llMathBlueprint.visibility = View.VISIBLE
            binding.llEnglishManual.visibility = View.GONE
            binding.flGkProjector.visibility = View.GONE

        } else if (subject == Subject.ENGLISH) {
            // English -> Isabelle老师 + handbook面板
            binding.tvNpcName.text = learning?.npcName ?: "Isabelle"
            binding.tvDialogue.text = learning?.npcDialogue ?: "Read the manual carefully before sorting!"
            binding.tvEnglishLesson.text = learning?.lessonText ?: "Complete the sentence."

            if (hasMedia) {
                binding.flEnglishImageArea.visibility = View.VISIBLE
                applyMedia(binding.ivHandbook, binding.vvHandbook, media)
            } else {
                binding.flEnglishImageArea.visibility = View.GONE
            }

            binding.llMathBlueprint.visibility = View.GONE
            binding.llEnglishManual.visibility = View.VISIBLE
            binding.flGkProjector.visibility = View.GONE

        } else {
            // GK -> Blathers猫头鹰, field notes面板
            binding.tvNpcName.text = learning?.npcName ?: "Blathers"
            binding.tvDialogue.text = learning?.npcDialogue ?: "Read the field notes, then go dig!"

            if (hasMedia) {
                binding.flGkImageArea.visibility = View.VISIBLE
                applyMedia(binding.ivGkDisplay, binding.vvGkDisplay, media)
            } else {
                binding.flGkImageArea.visibility = View.GONE
            }

            binding.tvGkLesson.text = learning?.lessonText ?: "Study the field notes."

            binding.llMathBlueprint.visibility = View.GONE
            binding.llEnglishManual.visibility = View.GONE
            binding.flGkProjector.visibility = View.VISIBLE
        }
    }

    // lesson可以放image或者video
    // item==null就不动, 保留xml里的默认illustration
    // VIDEO: 隐藏ImageView, VideoView播raw resource, loop+静音
    // IMAGE: 用ImageView, VideoView藏起来
    private fun applyMedia(imageView: ImageView, videoView: VideoView, item: MediaItem?) {
        if (item == null || (item.resourceId == 0 && item.url.isEmpty())) {
            videoView.visibility = View.GONE
            return
        }

        if (item.type == MediaType.VIDEO) {
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE

            // 用android.resource:// scheme读raw里的video
            val pkg = requireContext().packageName
            val uri = Uri.parse("android.resource://$pkg/${item.resourceId}")
            videoView.setVideoURI(uri)

            // prepared之后才start, loop+静音, 然后scale让video crop-fill不留黑边
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f)

                // VideoView默认fit-center会留黑边, 算个scale让短边fill parent
                // 长边自动overflow, parent的clipToOutline会裁掉
                val parentView = videoView.parent as? View
                if (parentView != null && parentView.width > 0 && parentView.height > 0
                    && mp.videoWidth > 0 && mp.videoHeight > 0) {
                    val viewAspect = parentView.width.toFloat() / parentView.height.toFloat()
                    val videoAspect = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                    val scale: Float
                    if (videoAspect > viewAspect) {
                        // video比container宽, scale up让高度fill, 左右溢出被crop
                        scale = videoAspect / viewAspect
                    } else {
                        // video比container窄, scale up让宽度fill, 上下溢出被crop
                        scale = viewAspect / videoAspect
                    }
                    videoView.scaleX = scale
                    videoView.scaleY = scale
                }

                videoView.start()
            }
        } else {
            // image: VideoView藏起来. 内置图走resId, admin上载的图走uri
            imageView.visibility = View.VISIBLE
            videoView.visibility = View.GONE
            if (item.resourceId != 0) {
                imageView.setImageResource(item.resourceId)
            } else if (item.url.isNotEmpty()) {
                imageView.setImageURI(Uri.parse(item.url))
            }
            if (item.fillFrame) {
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }
    }

    // ---------- 以下是StudyActivity/BottomFragment调用的public API ----------

    // 点Start Challenge后隐藏lesson面板, 只留NPC+气泡
    fun hideLessonPanel() {
        val b = _binding
        if (b == null) {
            return
        }
        b.llMathBlueprint.visibility = View.GONE
        b.llEnglishManual.visibility = View.GONE
        b.flGkProjector.visibility = View.GONE
    }

    // 切到worried/sweat表情 (答错时)
    fun showWrongFace() {
        val b = _binding
        if (b == null) {
            return
        }
        val wrongRes = Utils.getNpcFullbody(subject, Utils.NPC_STATE_SWEAT)
        b.ivNpcFullbody.setImageResource(wrongRes)
    }

    // 切回normal表情
    fun showNormalFace() {
        val b = _binding
        if (b == null) {
            return
        }
        val normalRes = Utils.getNpcFullbody(subject, Utils.NPC_STATE_NORMAL)
        b.ivNpcFullbody.setImageResource(normalRes)
    }

    // 切到win表情 (答对时)
    fun showWinFace() {
        val b = _binding
        if (b == null) {
            return
        }
        val winRes = Utils.getNpcFullbody(subject, Utils.NPC_STATE_WIN)
        b.ivNpcFullbody.setImageResource(winRes)
    }

    // 改NPC对话气泡的文字
    fun setDialogue(text: String) {
        val b = _binding
        if (b == null) {
            return
        }
        b.tvDialogue.text = text
    }

    // 清理binding, 防止leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
