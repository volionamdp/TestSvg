package com.volio.pdfediter.pdf

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import com.artifex.mupdfdemo.MuPDFCore
import kotlin.math.sqrt
import kotlin.random.Random

class PdfVerticalPageByPageMode(context: Context, callback: PdfPageCallback) :
    PdfBaseMode(context = context, callback = callback) {
    private var lastY = 0f
    private var lastX = 0f
    private var lastScale = 1f
    private val spaceVertical = 20f
    private val spaceHorizontal = 20f

    private val pointScaleCenter = PointF(0f, 0f)
    private var width: Int = 1
    private var height: Int = 1
    private var anim: ValueAnimator? = null
    private var beforePagePosition: Int = 0
    val TAG = "zvvv"

    override fun initData(muPDFCore: MuPDFCore, viewWidth: Int, viewHeight: Int) {
        core = muPDFCore
        width = viewWidth
        height = viewHeight
        listDraw.clear()
        var totalHeight = 0f
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
            val top = totalHeight + (height - pageHeight) / 2f
            val left = (width - pageWidth) / 2f

            totalHeight += height

            val page = Page(context, index, muPDFCore, viewWidth, viewHeight) {
                callback.updateView()
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
        }
    }

    val random = Random(1)


    override fun draw(canvas: Canvas) {
        for (page in listDraw) {
            page.draw(canvas)
        }
    }

    override fun changePage(position: Int) {
        if (position != currentPagePosition) {
            currentPagePosition = position
            animUp()
        }
    }

    override fun setCurrentPage(position: Int, isAnim: Boolean) {

    }

    override fun release() {
        for (page in listDraw) page.release()
    }

    override fun updateScroll() {
        if (listDraw.size > 0) {
            val firstPage = listDraw[0]
            val lastPage = listDraw[listDraw.size - 1]
            var space = lastPage.getRectDraw().top - firstPage.getRectDraw().top
            if (space == 0f) space = 1f
            var scroll = (spaceVertical - firstPage.getRectDraw().top) / space
            if (scroll < 0) scroll = 0f
            if (scroll > 1) scroll = 1f
            callback.scroll(scroll)
        }
    }

    override fun fling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) {
        if (getPageCount() > 0) {
            val currentRect = listDraw[currentPagePosition].getRectDraw()
            if (currentRect.height() < height) {
                if (velocityY > 7000) changePage(currentPagePosition - 1)

                if (velocityY < -7000) changePage(currentPagePosition + 1)

                if (currentPagePosition == getPageCount()) {
                    currentPagePosition = getPageCount() - 1
                }
                if (currentPagePosition < 0) currentPagePosition = 0
            }
        }
    }

    override fun touch(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.y
                lastX = event.x
//                if (typeTouch == PdfView.TYPE_DRAWING) {
//                    findDownPage(event)
//                    currentPage?.downTouch(event)
//                }
                downTouchEvent(event)

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
//                standardizePage()
                callback.updateView()
            }

            MotionEvent.ACTION_UP -> {
                Log.d("zzvv", "onTouchEvent: ${event.eventTime - event.downTime}")
                if (event.eventTime - event.downTime < 250) {
                    if (typeTouch != PdfView.TYPE_MOVE) typeTouch = PdfView.TYPE_MOVE
//                    else if (typeTouch == PdfView.TYPE_MOVE) typeTouch = PdfView.TYPE_DRAWING
                }
                Log.d("zzvv", "onTouchEvent: ${typeTouch}")
//                currentPagePosition++
//                if (currentPagePosition == getPageCount()) currentPagePosition = 0
                animUp()

            }
        }
        return true
    }

    private fun animUp() {
        if (currentPagePosition != beforePagePosition) {
            animPageByPage()
        } else {
            val currentRect = listDraw[currentPagePosition].getRectDraw()
            if (currentRect.height() > height) {
                if (currentRect.top > spaceVertical) {
                    if (currentPagePosition > 0) {
                        changePage(currentPagePosition - 1)
                        animPageByPage()
                    } else {
                        animTopBottom(true)
                    }
                }
                if (currentRect.bottom < height - spaceVertical) {
                    if (currentPagePosition < getPageCount() - 1) {
                        changePage(currentPagePosition + 1)
                        animPageByPage()
                    } else {
                        animTopBottom(false)
                    }
                }
            } else {
                if (currentRect.bottom < height / 2 && currentPagePosition < getPageCount() - 1) {
                    changePage(currentPagePosition + 1)
                }
                if (currentRect.top > height / 2 && currentPagePosition > 0) {
                    changePage(currentPagePosition - 1)
                }
                animPageByPage()
            }
        }
        beforePagePosition = currentPagePosition
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
        updateScroll()
    }

    override fun updatePage() {
        updateMatrix()
        standardizePage()
    }

    override fun cancelAnim() {

    }

    override fun endAnimZoom() {

    }

    private fun standardizePage() {
        if (listDraw.size > 0) {
            val firsPage = listDraw[0]
            val lastPage = listDraw[listDraw.size - 1]
            val rectFirs = firsPage.getRectDraw()
            val rectLast = lastPage.getRectDraw()
            standardizePageScale(rectFirs)
            standardizePageScrollX(
                listDraw[currentPagePosition].getRectDraw(),
                listDraw[currentPagePosition].getRectDraw()
            )
//            standardizePageScrollY(rectFirs, rectLast)
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

    private fun animPageByPage() {
        anim?.cancel()

        val currentRect = listDraw[currentPagePosition].getRectDraw()
        val tY = height / 2 - currentRect.centerY()

        anim = ValueAnimator.ofFloat(tY)
        anim?.duration = 300
        val saveMatrix = Matrix(pdfMatrix)
        anim?.addUpdateListener {
            pdfMatrix.set(saveMatrix)
            pdfMatrix.postTranslate(0f, it.animatedValue as Float)
            updateMatrix()
            callback.updateView()
        }
        anim?.start()
    }

    private fun animTopBottom(isTop: Boolean) {
        anim?.cancel()

        val currentRect = listDraw[currentPagePosition].getRectDraw()
        var tY = spaceVertical - currentRect.top
        if (!isTop) {
            tY = height - spaceVertical - currentRect.bottom
        }
        anim = ValueAnimator.ofFloat(tY)
        anim?.duration = 300
        val saveMatrix = Matrix(pdfMatrix)
        anim?.addUpdateListener {
            pdfMatrix.set(saveMatrix)
            pdfMatrix.postTranslate(0f, it.animatedValue as Float)
            updateMatrix()
            callback.updateView()
        }
        anim?.start()
    }
}