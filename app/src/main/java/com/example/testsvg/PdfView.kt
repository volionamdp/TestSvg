package com.example.testsvg

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

class PdfView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private var listDraw: MutableList<Page> = mutableListOf()
    private var pdfMatrix: Matrix = Matrix()
    private val space = 20
    private val spaceHorizontal = 20f
    private val pointScaleCenter = PointF(0f, 0f)
    private val minScale = 1f
    private val maxScale = 3f

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        init()
    }

    private fun init() {
        listDraw.clear()
        val height = width * 1.5f
        for (i in 0..3) {
            val page = Page(i)
            val top = i * (height + space) + space
            page.setRect(
                RectF(
                    spaceHorizontal,
                    top,
                    width.toFloat() - spaceHorizontal,
                    top + height
                )
            )
            listDraw.add(page)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        for (page in listDraw) {
            if (canvas != null) {
                page.draw(canvas)
            }
        }
    }

    private var lastY = 0f
    private var lastX = 0f
    private var typeTouch:Int = TYPE_MOVE
    private var lastScale = 1f
    private var currentPage:Page? = null
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action?.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.y
                lastX = event.x
                if (typeTouch == TYPE_DRAWING) {
                    findDownPage(event)
                    currentPage?.downTouch(event)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN->{
                Log.d("zvv", "onTouchEvent: ACTION_POINTER_DOWN")
                calculateMidPoint(event,pointScaleCenter)
                lastScale = calculateDistance(event)
                typeTouch = TYPE_SCALE
            }
            MotionEvent.ACTION_MOVE -> {
                if (typeTouch == TYPE_MOVE) {
                    pdfMatrix.postTranslate(event.x - lastX, event.y - lastY)
                    lastY = event.y
                    lastX = event.x
                }
                if (event.pointerCount == 2 && typeTouch == TYPE_SCALE){
                    val scale = calculateDistance(event)
                    pdfMatrix.postScale(scale/lastScale,scale/lastScale,pointScaleCenter.x,pointScaleCenter.y)
                    lastScale = scale
                    Log.d("zvv", "onTouchEvent: ")
                }
                if (typeTouch == TYPE_DRAWING){
                    currentPage?.moveTouch(event)
                }
                updatePage()
                standardizePage()
                invalidate()
            }

            MotionEvent.ACTION_UP->{
                Log.d("zzvv", "onTouchEvent: ${event.eventTime - event.downTime}")
                if (event.eventTime - event.downTime < 250){
                    if (typeTouch != TYPE_MOVE) typeTouch = TYPE_MOVE
                    else if (typeTouch == TYPE_MOVE) typeTouch = TYPE_DRAWING
                }
                Log.d("zzvv", "onTouchEvent: ${typeTouch}")

            }
        }
        return true
    }
    private fun findDownPage(motionEvent: MotionEvent){
        for (page in listDraw){
            if (page.getRectDraw().contains(motionEvent.x,motionEvent.y)){
                currentPage = page
            }
        }
    }
    private fun calculateDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }
    private fun calculateMidPoint(event: MotionEvent, point: PointF) {
        point.x = (event.getX(0) + event.getX(1)) / 2
        point.y = (event.getY(0) + event.getY(1)) / 2
    }

    private fun updateMatrix() {
        for (page in listDraw) {
            page.updateMatrix(pdfMatrix)
        }
    }

    private fun updatePage() {
        updateMatrix()
        standardizePage()
    }

    private fun standardizePage() {
        if (listDraw.size > 0) {
            val firsPage = listDraw[0]
            val lastPage = listDraw[listDraw.size - 1]
            val rectFirs = firsPage.getRectDraw()
            val rectLast = lastPage.getRectDraw()
            standardizePageScale(rectFirs)
            standardizePageScrollX(rectFirs, rectLast)
            standardizePageScrollY(rectFirs, rectLast)
        }

    }

    private fun standardizePageScrollY(rectFirs: RectF, rectLast: RectF) {
        if (rectFirs.top > space) {
            pdfMatrix.postTranslate(0f, space - rectFirs.top)
            updateMatrix()
        } else if (rectLast.bottom < height - space) {
            val tY = height - space - rectLast.bottom
            if (rectFirs.top + tY < space) {
                pdfMatrix.postTranslate(0f, tY)
            } else {
                pdfMatrix.postTranslate(0f, space - rectFirs.top)
            }
            updateMatrix()

        }
    }

    private fun standardizePageScrollX(rectFirs: RectF, rectLast: RectF) {
        if (rectFirs.left > spaceHorizontal) {
            pdfMatrix.postTranslate( spaceHorizontal - rectFirs.left,0f)
            updateMatrix()
        } else if (rectLast.right < width - spaceHorizontal) {
            val tX = width - spaceHorizontal - rectLast.right
            if (rectFirs.left + tX < spaceHorizontal) {
                pdfMatrix.postTranslate( tX,0f)
            } else {
                pdfMatrix.postTranslate( spaceHorizontal - rectFirs.left,0f)
            }
            updateMatrix()

        }
    }

    private fun standardizePageScale(rectFirs: RectF) {
        val viewWidth = width - spaceHorizontal * 2
        val currentScale = rectFirs.width() / viewWidth
        if (currentScale < minScale) {
            pdfMatrix.postScale(
                minScale / currentScale,
                minScale / currentScale,
                pointScaleCenter.x,
                pointScaleCenter.y
            )
            updateMatrix()
        }
        if (currentScale > maxScale){
            pdfMatrix.postScale(
                maxScale / currentScale,
                maxScale / currentScale,
                pointScaleCenter.x,
                pointScaleCenter.y
            )
            updateMatrix()
        }
    }

    companion object{
        val TYPE_DEFAULT = 0
        val TYPE_MOVE = 1
        val TYPE_SCALE = 2
        val TYPE_DRAWING = 3
    }
}