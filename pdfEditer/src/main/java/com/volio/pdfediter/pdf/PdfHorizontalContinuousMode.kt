package com.volio.pdfediter.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import com.artifex.mupdfdemo.MuPDFCore
import kotlin.math.sqrt
import kotlin.random.Random


class PdfHorizontalContinuousMode(context: Context, updateView: () -> Unit) :
    PdfBaseMode(context = context, updateView = updateView) {
    private var lastY = 0f
    private var lastX = 0f
    private var typeTouch: Int = PdfView.TYPE_MOVE
    private var lastScale = 1f
    private var currentPage: Page? = null
    private var listDraw: MutableList<Page> = mutableListOf()
    private var pdfMatrix: Matrix = Matrix()
    private val spaceVertical = 20f
    private val spaceHorizontal = 20f

    private val pointScaleCenter = PointF(0f, 0f)
    private val minScale = 1f
    private val maxScale = 3f
    private var width: Int = 1
    private var height: Int = 1
    private var positionPageMaxHeight: Int = 0
    private var flingAnimation: FlingAnimation? = null
    private val gestureDetector: GestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (getPageCount() > 0 && e2 != null) {
                    for (item in listDraw) item.cancelLoadHighQualityImage()
                    val saveMatrix = Matrix(pdfMatrix)
                    flingAnimation?.cancel()
                    flingAnimation = FlingAnimation(FloatValueHolder())
                    flingAnimation?.setStartVelocity(velocityX)
                    flingAnimation?.addUpdateListener { _, value, _ ->
                        pdfMatrix.set(saveMatrix)
                        pdfMatrix.postTranslate(value, 0f)
                        updateMatrix()
                        standardizePage()
                        updateView()
                    }
                    flingAnimation?.addEndListener { animation, canceled, value, velocity ->
                        for (item in listDraw) item.loadImageScale()
                    }
                    flingAnimation?.start()
                }else{
                    for (item in listDraw) item.loadImageScale()
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })

    override fun initData(muPDFCore: MuPDFCore, viewWidth: Int, viewHeight: Int) {
        core = muPDFCore
        width = viewWidth
        height = viewHeight
        listDraw.clear()
        var totalWidth = 0f
        for (index in 0 until getPageCount()) {
            val originRect = getPageSize(index)
            val ratioScreen = (width - 2 * spaceHorizontal) / (height - spaceVertical * 2)
            val ratioPage = originRect.width() / originRect.height()

            var pageWidth = width - 2f * spaceHorizontal
            var pageHeight = pageWidth * originRect.height() / originRect.width()
            if (ratioPage < ratioScreen) {
                pageHeight = height - spaceVertical * 2
                pageWidth = pageHeight * ratioPage
            }
            val top = (height - pageHeight) / 2f
            val left = totalWidth + spaceHorizontal

            Log.d("vvzz3", "initData: $pageHeight  ${height - spaceVertical * 2}")

            totalWidth += spaceHorizontal + pageWidth

            val page = Page(context, index, muPDFCore, viewWidth, viewHeight) {
                updateView()
            }
            page.setDefaultRect(
                RectF(
                    left,
                    top,
                    left + pageWidth,
                    top + pageHeight
                )
            )
            listDraw.add(page)
            if (positionPageMaxHeight < listDraw.size) {
                listDraw[positionPageMaxHeight].let {
                    if (pageHeight > it.getRectDraw().height()) {
                        positionPageMaxHeight = index
                    }
                }
            }
        }
        //updateCurrentPage(0)
    }

    val random = Random(2)


    override fun draw(canvas: Canvas) {
        for (page in listDraw) {
            page.draw(canvas)
        }
    }

    override fun changePage(position: Int) {
        if (position != currentPagePosition) {
            currentPagePosition = position
        }
    }

    override fun release() {
        currentPagePosition = 0
        for (page in listDraw) page.release()
    }

    override fun setCurrentPage(position: Int, isAnim: Boolean) {
        if (position != currentPagePosition) {
            currentPagePosition = position
            pageToPageNoAnim()
            updateCurrentPage(position)
        }
    }

    override fun touch(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                cancelAnim()
                lastY = event.y
                lastX = event.x
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                calculateMidPoint(event, pointScaleCenter)
                lastScale = calculateDistance(event)
                typeTouch = PdfView.TYPE_SCALE
            }
            MotionEvent.ACTION_MOVE -> {
                if (typeTouch == PdfView.TYPE_MOVE) {
                    pdfMatrix.postTranslate(event.x - lastX, event.y - lastY)
                    lastY = event.y
                    lastX = event.x
                }
                if (event.pointerCount == 2 && typeTouch == PdfView.TYPE_SCALE) {
                    val scale = calculateDistance(event)
                    pdfMatrix.postScale(
                        scale / lastScale,
                        scale / lastScale,
                        pointScaleCenter.x,
                        pointScaleCenter.y
                    )
                    lastScale = scale
                }
                updatePage()
                updateView()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                Log.d("zzze3", "touch: ${event.pointerCount}")
            }

            MotionEvent.ACTION_UP -> {
                if (typeTouch != PdfView.TYPE_MOVE) typeTouch = PdfView.TYPE_MOVE
                Log.d("zzvv123", "onTouchEvent: ${typeTouch} ${listDraw.size}")
//                for (item in listDraw) {
//                    Log.d("zzvvv33", "onTouchEvent: ${typeTouch}")
//
//                    item.loadImageScale()
//                }

            }
        }
        return true
    }

    private fun cancelAnim() {
        flingAnimation?.cancel()
    }

    private fun findDownPage(motionEvent: MotionEvent) {
        for (page in listDraw) {
            if (page.getRectDraw().contains(motionEvent.x, motionEvent.y)) {
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

    private fun updateCurrentPage(position: Int) {
        Log.d("zzzz", "updateCurrentPage1: $position")
        for (page in listDraw) {
            if (page.position > position - pageLoad && page.position < position + pageLoad) {
                page.loadImage()
            } else {
                page.cancelLoadImage()
            }
        }

    }

    private fun updateCurrentPageContinuous(position: Int) {
        if (position != currentPagePosition) {
            updateCurrentPage(position)
        }
    }

    private fun updateMatrix() {
        for (page in listDraw) {
            page.updateMatrix(pdfMatrix)
            if (page.getRectDraw().contains(width / 2f, height / 2f)) {
                updateCurrentPageContinuous(page.position)
            }
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
            listDraw[positionPageMaxHeight].let {
                standardizePageScale(it)
                standardizePageScrollY(rectFirs)

            }
            standardizePageScrollX(rectFirs, rectLast)
        }

    }

    private fun standardizePageScrollY(rectF: RectF) {
        if (rectF.height() > height - spaceVertical) {
            if (rectF.top > spaceVertical) {
                pdfMatrix.postTranslate(0f, spaceVertical - rectF.top)
            }
            if (rectF.bottom < height - spaceVertical) {
                pdfMatrix.postTranslate(0f, height - spaceVertical - rectF.bottom)
            }
            Log.d("zzzz", "standardizePageScrollY: ")
        } else {
            pdfMatrix.postTranslate(0f, height / 2f - rectF.centerY())
            Log.d("zzzz2", "standardizePageScrollY: ${rectF.centerY()}")

        }
        updateMatrix()
    }

    private fun standardizePageScrollX(rectFirs: RectF, rectLast: RectF) {

        if (rectFirs.left > spaceHorizontal) {
            pdfMatrix.postTranslate(spaceHorizontal - rectFirs.left, 0f)
            updateMatrix()
        } else if (rectLast.right < width - spaceHorizontal) {
            val tX = width - spaceHorizontal - rectLast.right
            if (rectFirs.left + tX < spaceHorizontal) {
                pdfMatrix.postTranslate(tX, 0f)
            } else {
                pdfMatrix.postTranslate(spaceHorizontal - rectFirs.left, 0f)
            }
            updateMatrix()

        }

    }

    private fun standardizePageScale(page: Page) {
        val currentScale = page.getScaleY()
        if (currentScale < minScale) {
            pdfMatrix.postScale(
                minScale / currentScale,
                minScale / currentScale,
                pointScaleCenter.x,
                pointScaleCenter.y
            )
            updateMatrix()
        }
        if (currentScale > maxScale) {
            pdfMatrix.postScale(
                maxScale / currentScale,
                maxScale / currentScale,
                pointScaleCenter.x,
                pointScaleCenter.y
            )
            updateMatrix()
        }
    }

    private fun pageToPageNoAnim(){
        val currentRect = listDraw[currentPagePosition].getRectDraw()
        val tX = width / 2 - currentRect.centerX()
        pdfMatrix.postTranslate(tX, 0f)
        updateMatrix()
        updateView()
    }
}