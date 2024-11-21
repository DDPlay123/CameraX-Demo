package mai.project.cameraxapp.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.FloatRange
import kotlin.math.abs

/**
 * 繪製一層矩形的 View
 *
 * - 可以控制大小 及 位移
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        /**
         * 矩形邊框的寬度
         */
        private const val BORDER_WIDTH = 5f

        /**
         * 矩形邊框的圓角半徑，即觸控點
         */
        private const val CORNER_RADIUS = 20f

        /**
         * 控制點觸碰區的大小
         */
        private const val TOUCH_SLOP = 50f

        /**
         * 邊框顏色
         */
        private const val BORDER_COLOR = Color.RED

        /**
         * 控制點顏色
         */
        private const val CORNER_COLOR = Color.BLUE

        /**
         * 最小寬度，避免矩形太小
         */
        private const val MIN_WIDTH = 100
    }

    /**
     * 四個控制點的 Enum
     */
    private enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    // 矩形初始位置
    private var left: Float = 100f
    private var top: Float = 100f
    private var right: Float = 600f
    private var bottom: Float = 600f

    // 是否要固定矩形大小 且 不可控制
    private var isFixed = false

    // 邊框繪筆
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = BORDER_COLOR
        strokeWidth = BORDER_WIDTH
    }

    // 控制點繪筆
    private val cornerPaint = Paint().apply {
        style = Paint.Style.FILL
        color = CORNER_COLOR
    }

    // 當前操作的控制點
    private var activeCorner: Corner? = null

    // 是否正在拖曳矩形
    private var dragging = false

    // 完成移動後，紀錄 X 值
    private var lastTouchX = 0f

    // 完成移動後，紀錄 Y 值
    private var lastTouchY = 0f

    /**
     * 設定矩形初始區域
     *
     * - 傳遞相對比例，例如 0.25 代表 25%
     */
    fun setInitialRegion(
        @FloatRange(from = 0.0, to = 1.0)
        leftRatio: Float,
        @FloatRange(from = 0.0, to = 1.0)
        topRatio: Float,
        @FloatRange(from = 0.0, to = 1.0)
        rightRatio: Float,
        @FloatRange(from = 0.0, to = 1.0)
        bottomRatio: Float
    ) {
        // 將相對比例轉換為實際位置
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            left = (width * leftRatio).coerceIn(0f, width.toFloat())
            top = (height * topRatio).coerceIn(0f, height.toFloat())
            right = (width * rightRatio).coerceIn(0f, width.toFloat())
            bottom = (height * bottomRatio).coerceIn(0f, height.toFloat())
            invalidate()
        }
    }

    /**
     * 設定是否固定矩形大小 且 不可控制
     */
    fun setIsFixed(isFixed: Boolean) {
        this.isFixed = isFixed
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 繪製矩形
        canvas.drawRect(left, top, right, bottom, borderPaint)

        if (!isFixed) {
            // 繪製四個圓形控制點
            canvas.drawCircle(left, top, CORNER_RADIUS, cornerPaint)
            canvas.drawCircle(right, top, CORNER_RADIUS, cornerPaint)
            canvas.drawCircle(left, bottom, CORNER_RADIUS, cornerPaint)
            canvas.drawCircle(right, bottom, CORNER_RADIUS, cornerPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 觸碰的點
        val x = event.x
        val y = event.y
        // 如果固定矩形，則不執行手勢操作
        if (isFixed) return super.onTouchEvent(event)
        // 處理觸碰事件
        when (event.action) {
            MotionEvent.ACTION_DOWN -> if (handleActionDown(x, y)) return true

            MotionEvent.ACTION_MOVE -> if (handleActionMove(x, y)) return true

            MotionEvent.ACTION_UP -> handleActionUp()
        }
        return super.onTouchEvent(event)
    }

    /**
     * 處理 手指按下元件後的事件
     */
    private fun handleActionDown(x: Float, y: Float): Boolean {
        // 檢查是否按到控制點
        activeCorner = getTouchedCorner(x, y)
        if (activeCorner != null) {
            return true // 開始調整大小
        }
        // 檢查是否按在矩形內部
        if (x > left && x < right && y > top && y < bottom) {
            dragging = true
            lastTouchX = x
            lastTouchY = y
            return true // 開始拖曳
        }
        // 不處理其他情況
        return false
    }

    /**
     * 處理 手指移開元件後的事件
     */
    private fun handleActionUp() {
        // 重置狀態
        activeCorner = null
        dragging = false
    }

    /**
     * 處理 手指移動元件後的事件
     */
    private fun handleActionMove(x: Float, y: Float): Boolean {
        if (activeCorner != null) {
            // 根據控制點調整矩形大小
            when (activeCorner) {
                Corner.TOP_LEFT -> {
                    left = x.coerceAtMost(right - MIN_WIDTH)
                    top = y.coerceAtMost(bottom - MIN_WIDTH)
                }

                Corner.TOP_RIGHT -> {
                    right = x.coerceAtLeast(left + MIN_WIDTH)
                    top = y.coerceAtMost(bottom - MIN_WIDTH)
                }

                Corner.BOTTOM_LEFT -> {
                    left = x.coerceAtMost(right - MIN_WIDTH)
                    bottom = y.coerceAtLeast(top + MIN_WIDTH)
                }

                Corner.BOTTOM_RIGHT -> {
                    right = x.coerceAtLeast(left + MIN_WIDTH)
                    bottom = y.coerceAtLeast(top + MIN_WIDTH)
                }

                else -> Unit
            }
            invalidate()
            return true
        }

        if (dragging) {
            // 計算位移值
            val dx = x - lastTouchX
            val dy = y - lastTouchY

            // 添加位移值到矩形位置
            left += dx
            right += dx
            top += dy
            bottom += dy

            // 確保矩形不會移出視圖的範圍
            left = if (right < width.toFloat()) left.coerceAtLeast(0f) else left - dx
            top = if (bottom < height.toFloat()) top.coerceAtLeast(0f) else top - dy
            right = if (left > 0) right.coerceAtMost(width.toFloat()) else right - dx
            bottom = if (top > 0) bottom.coerceAtMost(height.toFloat()) else bottom - dy

            // 紀錄最後一次的觸碰點
            lastTouchX = x
            lastTouchY = y
            invalidate()
            return true
        }
        // 不處理其他情況
        return false
    }

    /**
     * 判斷觸碰的位置是哪一個控制點
     */
    private fun getTouchedCorner(x: Float, y: Float): Corner? {
        return when {
            isInCornerRange(x, y, left, top) -> Corner.TOP_LEFT
            isInCornerRange(x, y, right, top) -> Corner.TOP_RIGHT
            isInCornerRange(x, y, left, bottom) -> Corner.BOTTOM_LEFT
            isInCornerRange(x, y, right, bottom) -> Corner.BOTTOM_RIGHT
            else -> null
        }
    }

    /**
     * 判斷觸碰的位置是否在控制點的範圍內
     */
    private fun isInCornerRange(
        x: Float, y: Float,
        cornerX: Float, cornerY: Float
    ): Boolean {
        return (abs(x - cornerX) <= TOUCH_SLOP) && (abs(y - cornerY) <= TOUCH_SLOP)
    }
}
