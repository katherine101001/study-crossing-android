package com.example.assignment1

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// 自定义View: 在化石图片上覆盖一层dirt, 手指scratch掉dirt露出下面的图
// 用的是完整方块tile, 不裁边。tile size会自动调整保证整数个tile填满
class ScratchView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var dirtBitmap: Bitmap? = null
    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null
    private var brushRadius = 0f
    private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dirtyMask = false
    private var onRevealListener: ((Float) -> Unit)? = null

    // 预渲染的dirt surface (tile或单图填充)
    private var tileSurface: Bitmap? = null
    private var tileCols = 1
    private var tileRows = 1
    private var tileSize = 0f
    private var tileOffsetX = 0f
    private var tileOffsetY = 0f
    private var singleImageMode = false
    private var cornerRadiusPx = 0f
    private val clipPath = Path()
    private val soilColor = Color.rgb(0x5D, 0x40, 0x37)  // 泥土色, 和dig site frame匹配

    fun setBrushSizeDp(dp: Float) {
        brushRadius = (dp * resources.displayMetrics.density) / 2f
        erasePaint.strokeWidth = brushRadius * 2f
    }

    // 圆角clip, 让dirt overlay和themed frame的内圆角对齐
    fun setCornerRadiusDp(dp: Float) {
        cornerRadiusPx = dp * resources.displayMetrics.density
        rebuildClipPath()
        invalidate()
    }

    private fun rebuildClipPath() {
        clipPath.reset()
        if (cornerRadiusPx > 0f && width > 0 && height > 0) {
            clipPath.addRoundRect(
                0f, 0f, width.toFloat(), height.toFloat(),
                cornerRadiusPx, cornerRadiusPx, Path.Direction.CW
            )
        }
    }

    fun setDirtTexture(resId: Int) {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        dirtBitmap = BitmapFactory.decodeResource(resources, resId, opts)
        singleImageMode = false
        post { rebuildMask() }
    }

    // 单图模式: 一张dirt图拉伸到填满整个view (不做tile)
    // 用在dirt素材是一整张真实土壤图的时候
    fun setDirtImageSingle(resId: Int) {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        dirtBitmap = BitmapFactory.decodeResource(resources, resId, opts)
        singleImageMode = true
        post { rebuildMask() }
    }

    fun setOnRevealListener(listener: (Float) -> Unit) {
        onRevealListener = listener
    }

    fun getRevealedPercent(): Float {
        val mask = maskBitmap ?: return 0f
        val w = mask.width
        val h = mask.height
        if (w == 0 || h == 0) return 0f

        // 每4个像素采样一次, 省性能
        val pixels = IntArray((w * h) / 4)
        var idx = 0
        for (y in 0 until h step 2) {
            for (x in 0 until w step 2) {
                if (idx < pixels.size) {
                    pixels[idx++] = mask.getPixel(x, y)
                }
            }
        }

        var cleared = 0
        for (p in pixels) {
            if (Color.alpha(p) == 0) cleared++
        }
        return cleared.toFloat() / pixels.size
    }

    fun reset() {
        maskBitmap?.let {
            it.eraseColor(Color.BLACK)
            maskCanvas?.drawColor(Color.BLACK)
            invalidate()
        }
    }

    private fun rebuildMask() {
        if (width <= 0 || height <= 0) return
        val tile = dirtBitmap ?: return

        maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap!!)
        maskCanvas!!.drawColor(Color.BLACK)

        erasePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        erasePaint.style = Paint.Style.FILL
        erasePaint.isAntiAlias = true

        rebuildTileSurface(tile)
        invalidate()
    }

    private fun rebuildTileSurface(tile: Bitmap) {
        tileSurface = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val surfCanvas = Canvas(tileSurface!!)
        surfCanvas.drawColor(soilColor)

        if (singleImageMode) {
            // centerCrop缩放: 填满整个view, 长边会溢出
            val srcW = tile.width.toFloat()
            val srcH = tile.height.toFloat()
            if (srcW <= 0f || srcH <= 0f) return
            val scale = max(width / srcW, height / srcH)
            val outW = (srcW * scale).toInt().coerceAtLeast(1)
            val outH = (srcH * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(tile, outW, outH, true)
            val left = (width - outW) / 2f
            val top = (height - outH) / 2f
            surfCanvas.drawBitmap(scaled, left, top, null)
            return
        }

        val density = resources.displayMetrics.density
        val targetDp = 48f
        val targetPx = targetDp * density

        tileCols = max(1, (width / targetPx).roundToInt())
        tileRows = max(1, (height / targetPx).roundToInt())
        tileSize = min(width.toFloat() / tileCols, height.toFloat() / tileRows)
        tileOffsetX = (width - tileSize * tileCols) / 2f
        tileOffsetY = (height - tileSize * tileRows) / 2f

        val ts = tileSize.toInt()
        if (ts <= 0) return
        val scaledTile = Bitmap.createScaledBitmap(tile, ts, ts, true)

        for (row in 0 until tileRows) {
            for (col in 0 until tileCols) {
                val x = tileOffsetX + col * tileSize
                val y = tileOffsetY + row * tileSize
                surfCanvas.drawBitmap(scaledTile, x, y, null)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            rebuildMask()
            rebuildClipPath()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mask = maskBitmap ?: return
        val surface = tileSurface ?: return

        val rounded = cornerRadiusPx > 0f
        if (rounded) {
            canvas.save()
            canvas.clipPath(clipPath)
        }

        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 先画dirt surface
        canvas.drawBitmap(surface, 0f, 0f, null)

        // mask: 不透明的地方保留dirt, 透明的地方(scratch过的)露出下面
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, maskPaint)

        canvas.restoreToCount(layerId)

        if (rounded) canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mc = maskCanvas ?: return true
        if (dirtBitmap == null) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mc.drawCircle(event.x, event.y, brushRadius, erasePaint)
                dirtyMask = true
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    mc.drawCircle(
                        event.getHistoricalX(i),
                        event.getHistoricalY(i),
                        brushRadius,
                        erasePaint
                    )
                }
                mc.drawCircle(event.x, event.y, brushRadius, erasePaint)
                dirtyMask = true
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (dirtyMask && onRevealListener != null) {
                    onRevealListener?.invoke(getRevealedPercent())
                }
                dirtyMask = false
            }
        }
        return true
    }
}
