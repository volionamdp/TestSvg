package com.volio.pdfediter.pdf

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdfdemo.MuPDFCore
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class Page(
    val context: Context,
    val position: Int,
    private val core: MuPDFCore,
    private val viewWidth: Int,
    private val viewHeight: Int,
    private val updateView: () -> Unit
) {
    // kích thước page hiện tại
    private var rectDraw = RectF()

    // kich thước sau khi scale ban đầu
    private var rectDefaultView = RectF()

    private var rectOriginPage = RectF()

    private var recBitmap = Rect()
    private var recBitmapScale = Rect()

    private var rectBitmapScaleDefault = RectF()
    private var rectBitmapScaleDraw = RectF()

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintDrawing = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentMatrix: Matrix = Matrix()
    private var invertMatrix: Matrix = Matrix()
    private var pageMatrix: Matrix = Matrix()
    private var invertPageMatrix: Matrix = Matrix()

    private var path = Path()
    private var job: Job? = null
    private var jobLoadHighQuality: Job? = null
    private var dataBitmap: Bitmap? = null
    private var dataBitmapScale: Bitmap? = null
    private var scaleLoadHighQuality: Float = 1.5f

    private var paintBitmapTest = Paint()
    private var paintTextTest = Paint()

    private var textSelectCtr = PageTextSelectCtr()
    private var textBlock: MutableList<StructuredText.TextBlock> = mutableListOf()

    private var rectSelectView: RectF = RectF()

    private var listRectSelect: MutableList<RectF> = mutableListOf()

    private var isSelect: Boolean = false

    private var pointSelectHeight = 60f
    private var pointSelectRadius = 20f

    private var pointSelectRect1 = RectF()
    private var pointSelectRect2 = RectF()

    private var paintPointSelect = Paint(Paint.ANTI_ALIAS_FLAG)


    init {
        paint.color = Color.BLUE
        paintDrawing.color = Color.RED
        paintDrawing.style = Paint.Style.STROKE
        paintDrawing.strokeWidth = 5f
        paint.color = Color.WHITE
        rectOriginPage.set(getPageOriginSize())
        paintTextTest.color = Color.BLACK
        paintTextTest.textSize = 20f
        paintTextTest.textAlign = Paint.Align.CENTER

        paintPointSelect.strokeWidth = 8f
        paintPointSelect.color = Color.RED
    }

    fun getPageOriginSize(): RectF {
        val pointF = core.getPageSize(position)
        return RectF(0f, 0f, pointF.x, pointF.y)
    }

    fun setDefaultRect(rectF: RectF) {
        this.rectDefaultView.set(rectF)
        this.rectDraw.set(rectF)
        pageMatrix.setRectToRect(rectOriginPage, rectDraw, Matrix.ScaleToFit.CENTER)
        pageMatrix.invert(invertPageMatrix)
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
//                    canvas.drawRect(rectBitmapScaleDraw,paintBitmapTest)
                }
            }
            canvas.save()
            canvas.setMatrix(currentMatrix)
            canvas.drawPath(path, paintDrawing)
            canvas.restore()

            canvas.save()
            canvas.setMatrix(pageMatrix)
//            for (i in textBlock.indices) {
//                val item = textBlock[i]
//                if (item.color == 0) item.color =
//                    Color.rgb(range.nextInt(), range.nextInt(), range.nextInt())
//                paintBitmapTest.color = item.color
//                canvas.drawRect(item.bbox.toRectF(), paintBitmapTest)
//                paintBitmapTest.textSize = 20f
////                canvas.drawText("$i", item.bbox.centerX(), item.bbox.centerY(), paintTextTest)
//            }

//            canvas.drawText(
//                "point1",
//                textSelectCtr.getRectSelect().left,
//                textSelectCtr.getRectSelect().top,
//                paintTextTest
//            )
//            canvas.drawText(
//                "point2",
//                textSelectCtr.getRectSelect().right,
//                textSelectCtr.getRectSelect().bottom,
//                paintTextTest
//            )

            paintTextTest.setColor(Color.BLUE)
//            canvas.drawCircle(
//                textSelectCtr.getPoint1().x,
//                textSelectCtr.getPoint1().y,
//                2f,
//                paintTextTest
//            )
//            canvas.drawCircle(
//                textSelectCtr.getPoint2().x,
//                textSelectCtr.getPoint2().y,
//                2f,
//                paintTextTest
//            )
            if (isSelect) {
                for (rect in listRectSelect) {
                    paintBitmapTest.color = Color.argb(50, 255, 0, 0)
                    canvas.drawRect(rect, paintBitmapTest)
                }
            }

            canvas.restore()
//            paintTextTest.textSize = 120f
//            canvas.drawRect(
//                rectSelectView.left,
//                rectSelectView.top,
//                rectSelectView.left + 500,
//                rectSelectView.top + 500,
//                paintTextTest
//            )
//            canvas.drawRect(
//                rectSelectView.right,
//                rectSelectView.bottom,
//                rectSelectView.right + 500,
//                rectSelectView.bottom + 500,
//                paintTextTest
//            )
            if (isSelect) {
                drawPointSelect(canvas, pointSelectRect1)
                drawPointSelect(canvas, pointSelectRect2)
            }

        }

    }

    private fun drawPointSelect(canvas: Canvas,rectF: RectF) {
        val stopLine = rectF.bottom + pointSelectHeight
        canvas.drawLine(
            rectF.centerX(),
            rectF.top,
            rectF.centerX(),
            stopLine,
            paintPointSelect
        )
        canvas.drawCircle(rectF.centerX(),stopLine,pointSelectRadius,paintPointSelect)
    }


    fun getRectDraw(): RectF {
        return rectDraw
    }


    fun updateMatrix(matrix: Matrix) {
        matrix.mapRect(rectDraw, rectDefaultView)
        matrix.mapRect(rectBitmapScaleDraw, rectBitmapScaleDefault)
        matrix.invert(invertMatrix)
        currentMatrix.set(matrix)
        pageMatrix.setRectToRect(rectOriginPage, rectDraw, Matrix.ScaleToFit.CENTER)
        pageMatrix.invert(invertPageMatrix)
        if (isSelect) {
            updatePointSelect()
        }
    }

    private var dstPointMap: FloatArray = floatArrayOf(0f, 0f)
    private var srcPointMap: FloatArray = floatArrayOf(0f, 0f)
    private fun mapPoint(event: MotionEvent, matrix: Matrix, onMap: (x: Float, y: Float) -> Unit) {
        srcPointMap[0] = event.x
        srcPointMap[1] = event.y
        matrix.mapPoints(dstPointMap, srcPointMap)
        onMap(dstPointMap[0], dstPointMap[1])
    }

    fun mapRange(range: Float): Float {
        srcPointMap[0] = range
        srcPointMap[1] = range
        currentMatrix.mapPoints(dstPointMap, srcPointMap)
        return max(dstPointMap[0], dstPointMap[1])
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

    private var downX = 0f
    private var downY = 0f
    fun touchEvent(event: MotionEvent) {
        Log.d("zvvee", "touchEvent: ")

//        when (event.action.and(MotionEvent.ACTION_MASK)) {
//            MotionEvent.ACTION_DOWN -> {
//                downX = event.x
//                downY = event.y
//            }
//            MotionEvent.ACTION_MOVE -> {
//                Log.d("zvvee", "touchEvent: move")
//                val pointArray = floatArrayOf(downX, downY, event.x, event.y)
//                val pointMap = FloatArray(4)
//                invertPageMatrix.mapPoints(pointMap, pointArray)
//                textSelectCtr.changePointSelect(pointMap[0], pointMap[1], pointMap[2], pointMap[3])
//                updateView()
//            }
//        }
    }

    fun loadImage() {
        if (job == null && dataBitmap == null) {
            job = CoroutineScope(Dispatchers.Default).launch {
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
                loadText()

            }
        }
    }

    fun loadImageScale() {
        val scale = rectDraw.width() / rectDefaultView.width()
        if (scale > scaleLoadHighQuality) {
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
            val templateRealRect = RectF()
            invertMatrix.mapRect(templateRealRect, templateRectF)
            jobLoadHighQuality?.cancel()
            jobLoadHighQuality = CoroutineScope(Dispatchers.Default).launch {
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
                rectBitmapScaleDefault.set(templateRealRect)
                currentMatrix.mapRect(rectBitmapScaleDraw, rectBitmapScaleDefault)
                recBitmapScale.set(0, 0, bitmap.width, bitmap.height)
                dataBitmapScale = bitmap
                rectBitmapScaleDraw.set(templateRectF)
                updateView()
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


    fun loadText() {
        val list = core.getTextBlock(position).toMutableList()
        textBlock.clear()
        textBlock.addAll(list)
        textSelectCtr.setListTextBlock(list)
        updateView()

    }

    fun checkPointText(x: Float, y: Float): Boolean {
        val pointArray = floatArrayOf(x, y)
        val pointMap = FloatArray(2)
        invertPageMatrix.mapPoints(pointMap, pointArray)
        return textSelectCtr.checkPointText(pointMap[0], pointMap[1])
    }

    fun changePointSelect(x0: Float, y0: Float, x1: Float, y1: Float) {
        val pointArray = floatArrayOf(x0, y0, x1, y1)
        val pointMap = FloatArray(4)
        invertPageMatrix.mapPoints(pointMap, pointArray)
        textSelectCtr.changePointSelect(pointMap[0], pointMap[1], pointMap[2], pointMap[3])
        val listRectF = textSelectCtr.getListRectSelect()
        listRectSelect.clear()
        listRectSelect.addAll(listRectF)
        updateView()
    }

    private fun updatePointSelect() {
        pageMatrix.mapRect(pointSelectRect1, textSelectCtr.getPoint1())
        pageMatrix.mapRect(pointSelectRect2, textSelectCtr.getPoint2())

    }

    private fun updateRectSelectView() {
        val rectF = textSelectCtr.getRectSelect()
        val pointArray = floatArrayOf(rectF.left, rectF.top, rectF.right, rectF.bottom)
        val pointMap = FloatArray(4)
        pageMatrix.mapPoints(pointMap, pointArray)
        rectSelectView.set(pointMap[0], pointMap[1], pointMap[2], pointMap[3])
    }

    fun getRectSelect(): RectF {
        updateRectSelectView()
        return rectSelectView
    }

    fun clearSelect(){
        isSelect = false
        listRectSelect.clear()
        updateView()
    }

    fun isSelect() = isSelect

    fun setSelect(isSelect:Boolean){
        this.isSelect = isSelect
        updateView()
    }


    companion object {
        val range = Random(254)
    }
}