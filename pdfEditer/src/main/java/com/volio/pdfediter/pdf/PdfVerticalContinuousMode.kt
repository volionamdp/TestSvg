package com.volio.pdfediter.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import com.artifex.mupdfdemo.MuPDFCore
import kotlin.math.sqrt
import kotlin.random.Random

class PdfVerticalContinuousMode(context: Context, updateView: () -> Unit) :
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
    override fun initData(muPDFCore: MuPDFCore, viewWidth: Int, viewHeight: Int) {
        core = muPDFCore
        width = viewWidth
        height = viewHeight
        listDraw.clear()
        var totalHeight = 0f
        for (index in 0 until getPageCount()) {
            val originRect = getPageSize(index)
            val pageWidth = width - 2f * spaceHorizontal
            val pageHeight = pageWidth * originRect.height() / originRect.width()
            val top = totalHeight + spaceVertical
            totalHeight = top + pageHeight
            val page = Page(context,index,muPDFCore,viewWidth,viewHeight){
                updateView()
            }
            Log.d("zzz", "initData: $pageHeight")
            page.setDefaultRect(
                RectF(
                    spaceHorizontal,
                    top,
                    width.toFloat() - spaceHorizontal,
                    top + pageHeight
                )
            )
            listDraw.add(page)
        }
    }

    val random = Random(1)



    override fun draw(canvas: Canvas) {
        for (page in listDraw) {
            page.draw(canvas)
        }
    }

    override fun changePage(position: Int) {
        if (position != currentPagePosition){
            currentPagePosition = position
        }
    }
    override fun setCurrentPage(position: Int, isAnim: Boolean) {

    }
    override fun release() {
        for (page in listDraw) page.release()
    }
    override fun touch(event: MotionEvent): Boolean {
        when (event.action.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.y
                lastX = event.x
                if (typeTouch == PdfView.TYPE_DRAWING) {
                    findDownPage(event)
                    currentPage?.downTouch(event)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                Log.d("zvv", "onTouchEvent: ACTION_POINTER_DOWN")
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
                    Log.d("zvv", "onTouchEvent: ")
                }
                if (typeTouch == PdfView.TYPE_DRAWING) {
                    currentPage?.moveTouch(event)
                }
                updatePage()
                standardizePage()
                updateView()
            }

            MotionEvent.ACTION_UP -> {
                Log.d("zzvv", "onTouchEvent: ${event.eventTime - event.downTime}")
                if (event.eventTime - event.downTime < 250) {
                    if (typeTouch != PdfView.TYPE_MOVE) typeTouch = PdfView.TYPE_MOVE
                    else if (typeTouch == PdfView.TYPE_MOVE) typeTouch = PdfView.TYPE_DRAWING
                }
                Log.d("zzvv", "onTouchEvent: ${typeTouch}")

            }
        }
        return true
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
        if (rectFirs.top > spaceVertical) {
            pdfMatrix.postTranslate(0f, spaceVertical - rectFirs.top)
            updateMatrix()
        } else if (rectLast.bottom < height - spaceVertical) {
            val tY = height - spaceVertical - rectLast.bottom
            if (rectFirs.top + tY < spaceVertical) {
                pdfMatrix.postTranslate(0f, tY)
            } else {
                pdfMatrix.postTranslate(0f, spaceVertical - rectFirs.top)
            }
            updateMatrix()

        }
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
}