package mai.project.cameraxapp.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * 繪製一層矩型的 View
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var left: Float = 100f
    private var top: Float = 100f
    private var right: Float = 600f
    private var bottom: Float = 600f

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 5f
    }

    private val cornerPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }

    private val cornerRadius = 20f
    private val touchSlop = 50f // 增加觸控區的大小，以便更容易點擊控制點
    private var activeCorner: Corner? = null
    private var dragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    fun setInitialRegion(leftRatio: Float, topRatio: Float, rightRatio: Float, bottomRatio: Float) {
        // 將相對比例轉換為實際位置
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            left = (width * leftRatio).coerceIn(0f, width.toFloat())
            top = (height * topRatio).coerceIn(0f, height.toFloat())
            right = (width * rightRatio).coerceIn(0f, width.toFloat())
            bottom = (height * bottomRatio).coerceIn(0f, height.toFloat())
            invalidate() // 重繪視圖
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 繪製矩形
        canvas.drawRect(left, top, right, bottom, paint)

        // 繪製四個圓形控制點
        canvas.drawCircle(left, top, cornerRadius, cornerPaint)
        canvas.drawCircle(right, top, cornerRadius, cornerPaint)
        canvas.drawCircle(left, bottom, cornerRadius, cornerPaint)
        canvas.drawCircle(right, bottom, cornerRadius, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
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
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeCorner != null) {
                    // 根據控制點調整矩形大小
                    when (activeCorner) {
                        Corner.TOP_LEFT -> {
                            left = x.coerceAtMost(right - 100)
                            top = y.coerceAtMost(bottom - 100)
                        }

                        Corner.TOP_RIGHT -> {
                            right = x.coerceAtLeast(left + 100)
                            top = y.coerceAtMost(bottom - 100)
                        }

                        Corner.BOTTOM_LEFT -> {
                            left = x.coerceAtMost(right - 100)
                            bottom = y.coerceAtLeast(top + 100)
                        }

                        Corner.BOTTOM_RIGHT -> {
                            right = x.coerceAtLeast(left + 100)
                            bottom = y.coerceAtLeast(top + 100)
                        }

                        else -> Unit
                    }
                    invalidate()
                    return true
                }

                if (dragging) {
                    // 拖曳整個矩形
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    left += dx
                    right += dx
                    top += dy
                    bottom += dy

                    // 確保矩形不會移出視圖的範圍
                    left = left.coerceAtLeast(0f)
                    top = top.coerceAtLeast(0f)
                    right = right.coerceAtMost(width.toFloat())
                    bottom = bottom.coerceAtMost(height.toFloat())

                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                // 重置狀態
                activeCorner = null
                dragging = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTouchedCorner(x: Float, y: Float): Corner? {
        return when {
            isInCornerRange(x, y, left, top) -> Corner.TOP_LEFT
            isInCornerRange(x, y, right, top) -> Corner.TOP_RIGHT
            isInCornerRange(x, y, left, bottom) -> Corner.BOTTOM_LEFT
            isInCornerRange(x, y, right, bottom) -> Corner.BOTTOM_RIGHT
            else -> null
        }
    }

    private fun isInCornerRange(x: Float, y: Float, cornerX: Float, cornerY: Float): Boolean {
        return (abs(x - cornerX) <= touchSlop) && (abs(y - cornerY) <= touchSlop)
    }
}
