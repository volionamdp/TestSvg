package com.volio.pdfediter.pdf

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import com.artifex.mupdfdemo.MuPDFCore
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlin.random.Random

class Page(
    val context: Context,
    val position: Int,
    val core: MuPDFCore,
    val viewWidth: Int,
    val viewHeight: Int,
    val updateView: () -> Unit
) {
    // kích thước page hiện tại
    private var rectDraw = RectF()

    // kich thước sau khi scale ban đầu
    private var rectDefaultView = RectF()
    private var recBitmap = Rect()
    private var recBitmapScale = Rect()

    private var rectBitmapScaleDefault = RectF()
    private var rectBitmapScaleDraw = RectF()

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintDrawing = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentMatrix: Matrix = Matrix()
    private var invertMatrix: Matrix = Matrix()
    private var path = Path()
    private var job: Job? = null
    private var jobLoadHighQuality:Job? = null
    private var dataBitmap: Bitmap? = null
    private var dataBitmapScale: Bitmap? = null
    private var scaleLoadHighQuality: Float = 1.5f

    init {
        paint.color = Color.BLUE
        paintDrawing.color = Color.RED
        paintDrawing.style = Paint.Style.STROKE
        paintDrawing.strokeWidth = 5f
        paint.color = Color.rgb(range.nextInt(), range.nextInt(), range.nextInt())

    }

    fun getPageOriginSize(): RectF {
        val pointF = core.getPageSize(position)
        return RectF(0f, 0f, pointF.x, pointF.y)
    }

    fun setDefaultRect(rectF: RectF) {
        this.rectDefaultView.set(rectF)
        this.rectDraw.set(rectF)
    }

    fun getScaleY(): Float {
        return rectDraw.height() / rectDefaultView.height()
    }

    fun getScaleX(): Float {
        return rectDraw.width() / rectDefaultView.width()
    }

    fun draw(canvas: Canvas) {
        if ((rectDraw.top > 0 && rectDraw.top < viewHeight) || (rectDraw.bottom > 0 && rectDraw.bottom < viewHeight) || (rectDraw.top < 0 && rectDraw.bottom > viewHeight)) {
            canvas.drawRect(rectDraw, paint)
            dataBitmap?.let {
                canvas.drawBitmap(it, recBitmap, rectDraw, null)
            }
            dataBitmapScale?.let {
                if (scaleLoadHighQuality < rectDraw.width() / rectDefaultView.width()) {
                    canvas.drawBitmap(it, recBitmapScale, rectBitmapScaleDraw, null)
                }
            }
            canvas.save()
            canvas.setMatrix(currentMatrix)
            canvas.drawPath(path, paintDrawing)
            canvas.restore()
        }
    }

    fun getRectDraw(): RectF {
        return rectDraw
    }


    fun updateMatrix(matrix: Matrix) {
        matrix.mapRect(rectDraw, rectDefaultView)
        Log.d("zzz", "updateMatrix: $rectBitmapScaleDraw")
        matrix.mapRect(rectBitmapScaleDraw, rectBitmapScaleDefault)
        Log.d("zzz", "updateMatrix: $rectBitmapScaleDraw")

        matrix.invert(invertMatrix)
        currentMatrix.set(matrix)

    }

    private var dstPointMap: FloatArray = floatArrayOf(0f, 0f)
    private var srcPointMap: FloatArray = floatArrayOf(0f, 0f)
    private fun mapPoint(event: MotionEvent, matrix: Matrix, onMap: (x: Float, y: Float) -> Unit) {
        srcPointMap[0] = event.x
        srcPointMap[1] = event.y
        matrix.mapPoints(dstPointMap, srcPointMap)
        onMap(dstPointMap[0], dstPointMap[1])
    }

    fun moveTouch(event: MotionEvent) {
        mapPoint(event, invertMatrix) { x, y ->
            path.lineTo(x, y)
        }
    }


    fun downTouch(event: MotionEvent) {
        mapPoint(event, invertMatrix) { x, y ->
            path.moveTo(x, y)
        }
    }

    fun touchEvent(event: MotionEvent) {
//        when (event.action.and(MotionEvent.ACTION_MASK)) {
//            MotionEvent.ACTION_DOWN -> {
//            }
//            MotionEvent.ACTION_UP -> {
//                loadImageScale()
//            }
 //       }
    }

    fun loadImage() {
        if (job == null && dataBitmap == null) {
            job = CoroutineScope(Dispatchers.Default).launch {
//                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.anh2)
                val bitmap = Bitmap.createBitmap(
                    rectDefaultView.width().toInt(),
                    rectDefaultView.height().toInt(),
                    Bitmap.Config.ARGB_8888
                )
                core.drawPage(
                    bitmap,
                    position,
                    bitmap.width,
                    bitmap.height,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height
                )
                recBitmap.set(0, 0, bitmap.width, bitmap.height)
                dataBitmap = bitmap
                updateView()

                Log.d("zzvvv22", "loadImage: ok  $position")

            }
        }
    }

    fun loadImageScale() {
        Log.d("zzvvv330", "loadImage: $position")

        val scale = rectDraw.width() / rectDefaultView.width()
        if (scale > scaleLoadHighQuality) {
            Log.d("zzvvv33", "loadImage: $position")
            if ((rectDraw.left < 0 && rectDraw.right < 0) || (rectDraw.left > viewWidth && rectDraw.right > viewWidth)
                || (rectDraw.top < 0 && rectDraw.bottom < 0) || (rectDraw.top > viewHeight && rectDraw.bottom > viewHeight)
            ) {
                return
            }
            var left = 0f
            var right = viewWidth.toFloat()
            var top = 0f
            var bottom = viewHeight.toFloat()
            if (rectDraw.left > 0) {
                left = rectDraw.left
            }
            if (rectDraw.top > 0) {
                top = rectDraw.top
            }
            if (rectDraw.right < viewWidth) {
                right = rectDraw.right
            }
            if (rectDraw.bottom < viewHeight) {
                bottom = rectDraw.bottom
            }

            val templateRectF = RectF(left, top, right, bottom)

            Log.d("zzvv", "loadImageScale: ${rectBitmapScaleDefault.toShortString()}")
            jobLoadHighQuality?.cancel()
            jobLoadHighQuality = CoroutineScope(Dispatchers.Default).launch {
//                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.anh2)
                val bitmap = Bitmap.createBitmap(
                    templateRectF.width().toInt(),
                    templateRectF.height().toInt(),
                    Bitmap.Config.ARGB_8888
                )
                core.drawPage(
                    bitmap,
                    position,
                    (rectDraw.width()).roundToInt(),
                    (rectDraw.height()).roundToInt(),
                    (templateRectF.left - rectDraw.left).roundToInt(),
                    ((templateRectF.top - rectDraw.top).toInt()),
                    bitmap.width,
                    bitmap.height
                )
                invertMatrix.mapRect(rectBitmapScaleDefault, templateRectF)
                recBitmapScale.set(0, 0, bitmap.width, bitmap.height)
                dataBitmapScale = bitmap
                rectBitmapScaleDraw.set(templateRectF)
                updateView()
                Log.d("zzvvv33", "loadImage: ok  $recBitmapScale")

            }
        }

    }

    fun cancelLoadImage() {
        Log.d("zzvvv22", "cancel: $position")
        dataBitmap?.recycle()
        dataBitmap = null
        job?.cancel()
        job = null
    }
    fun cancelLoadHighQualityImage() {
        Log.d("zzvvv22", "cancel: $position")
        dataBitmapScale?.recycle()
        dataBitmapScale = null
        jobLoadHighQuality?.cancel()
        jobLoadHighQuality = null
    }

    fun release() {
        job?.cancel()
        job = null

        dataBitmap?.recycle()
        dataBitmap = null
    }

    companion object {
        val range = Random(254)
    }
}