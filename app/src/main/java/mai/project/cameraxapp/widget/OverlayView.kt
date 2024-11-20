package mai.project.cameraxapp.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var left: Float = 0f
        private set

    var top: Float = 0f
        private set

    var right: Float = 0f
        private set

    var bottom: Float = 0f
        private set

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 計算中心方形的大小和位置
        left = (width * 0.25).toFloat()
        top = (height * 0.25).toFloat()
        right = (width * 0.75).toFloat()
        bottom = (height * 0.75).toFloat()

        // 繪製中心的方形區域
        canvas.drawRect(left, top, right, bottom, paint)
    }
}
