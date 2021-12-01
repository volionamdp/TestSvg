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
import androidx.core.animation.addListener
import androidx.dynamicanimation.animation.FlingAnimation
import com.artifex.mupdfdemo.MuPDFCore
import kotlin.math.sqrt


class PdfHorizontalPageByPageMode(context: Context, callback: PdfPageCallback) :
    PdfBaseMode(context = context, callback = callback) {
    private var lastY = 0f
    private var lastX = 0f
    private var lastScale = 1f

    private val spaceVertical = 20f
    private val spaceHorizontal = 20f

    private val pointScaleCenter = PointF(0f, 0f)
    private var width: Int = 1
    private var height: Int = 1
    private var positionPageMaxHeight: Int = 0
    private var flingAnimation: FlingAnimation? = null
    private var anim: ValueAnimator? = null
    private var beforePagePosition: Int = 0


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

            //tinh chieu rong chieu cao page
            var pageWidth = width - 2f * spaceHorizontal
            var pageHeight = pageWidth * originRect.height() / originRect.width()
            if (ratioPage < ratioScreen) {
                pageHeight = height - spaceVertical * 2
                pageWidth = pageHeight * ratioPage
            }
            val top = (height - pageHeight) / 2f
            val left = totalWidth + (width - pageWidth) / 2

            totalWidth += width

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
            if (positionPageMaxHeight < listDraw.size) {
                listDraw[positionPageMaxHeight].let {
                    if (pageHeight > it.getRectDraw().height()) {
                        positionPageMaxHeight = index
                    }
                }
            }
        }
        updateCurrentPage(currentPagePosition)
    }


    override fun draw(canvas: Canvas) {
        for (page in listDraw) {
            page.draw(canvas)
        }
    }

    override fun changePage(position: Int) {
        if (position != currentPagePosition && position < getPageCount() && position >= 0) {
            currentPagePosition = position
            callback.changePage(position)
        }
    }

    override fun setCurrentPage(position: Int, isAnim: Boolean) {
        if (position != currentPagePosition) {
            currentPagePosition = position
            if (isAnim) animPageByPage()
            else pageToPageNoAnim()
        }
    }

    override fun release() {
        currentPagePosition = 0
        for (page in listDraw) page.release()
    }

    override fun updateScroll() {
        if (listDraw.size > 0) {
            val firstPage = listDraw[0]
            val lastPage = listDraw[listDraw.size - 1]
            var space = lastPage.getRectDraw().left - firstPage.getRectDraw().left
            if (space == 0f) space = 1f
            var scroll = (spaceHorizontal - firstPage.getRectDraw().left) / space
            if (scroll < 0) scroll = 0f
            if (scroll > 1) scroll = 1f
            callback.scroll(scroll)
        }
    }

    override fun fling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) {
        if (getPageCount() > 0) {
            val currentRect = listDraw[currentPagePosition].getRectDraw()
            if (currentRect.width() < width) {
                if (velocityX > 7000) changePage(currentPagePosition - 1)

                if (velocityX < -7000) changePage(currentPagePosition + 1)

                if (currentPagePosition == getPageCount()) currentPagePosition =
                    getPageCount() - 1
                if (currentPagePosition < 0) currentPagePosition = 0
            }
        }
    }

    override fun touch(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                cancelAnim()
                lastY = event.y
                lastX = event.x
                downTouchEvent(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                calculateMidPoint(event, pointScaleCenter)
                lastScale = calculateDistance(event)
                typeTouch = PdfView.TYPE_SCALE
            }
            MotionEvent.ACTION_MOVE -> {
                moveTouchEvent(event,onMove = {
                    pdfMatrix.postTranslate(event.x - lastX, event.y - lastY)
                    lastY = event.y
                    lastX = event.x
                },onScale = {
                    val scale = calculateDistance(event)
                    pdfMatrix.postScale(
                        scale / lastScale,
                        scale / lastScale,
                        pointScaleCenter.x,
                        pointScaleCenter.y
                    )
                    lastScale = scale
                })
                updatePage()
                callback.updateView()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                Log.d("zvveer", "touch: " + event.pointerCount)
            }

            MotionEvent.ACTION_UP -> {
                Log.d("zzvvee3", "onTouchEventUp------: ${event.eventTime - event.downTime}")
                if (typeTouch == PdfView.TYPE_SCALE) typeTouch = PdfView.TYPE_MOVE

                Log.d("zzvv", "onTouchEvent: ${typeTouch}")
                animUp()

            }
        }
        return true
    }

    override fun cancelAnim() {
        flingAnimation?.cancel()
        anim?.cancel()
    }

    override fun endAnimZoom() {
//        animUp()
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
//        if (position != currentPagePosition){
        Log.d("zzzz", "updateCurrentPage: $position")
        for (page in listDraw) {
            if (page.position > position - pageLoad && page.position < position + pageLoad) {
                page.loadImage()
            } else {
                page.cancelLoadImage()
            }
        }
//        }
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

    // chuẩn hóa trục y khi với chiều cao của page (tính với scale)
    private fun standardizePageScrollY(rectF: RectF) {
        // chiều cao page > chiều cao view
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

    // chuẩn hóa trục X khi với chiều cao của page (tính với scale)
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

    // anim di chuyển
    private fun animUp() {
        if (listDraw.size == 0||animZoom?.isRunning==true) return
        if (currentPagePosition != beforePagePosition) {
            animPageByPage()
        } else {
            val currentRect = listDraw[currentPagePosition].getRectDraw()
            if (currentRect.width() > width) {
                var checkAnim = true
                if (currentRect.left > spaceHorizontal) {
                    if (currentPagePosition > 0) {
                        changePage(currentPagePosition - 1)
                        animPageByPage()
                    } else {
                        animLeftRight(true)
                    }
                } else {
                    checkAnim = false
                }
                if (currentRect.right < width - spaceHorizontal) {
                    if (currentPagePosition < getPageCount() - 1) {
                        changePage(currentPagePosition + 1)
                        animPageByPage()
                    } else {
                        animLeftRight(false)
                    }
                } else {
                    checkAnim = false
                }
                if (!checkAnim) loadHighQualityPage()
            } else {
                if (currentRect.right < width / 2 && currentPagePosition < getPageCount() - 1) {
                    changePage(currentPagePosition + 1)
                }
                if (currentRect.left > width / 2 && currentPagePosition > 0) {
                    changePage(currentPagePosition - 1)
                }
                animPageByPage()
            }
        }
        updateCurrentPage(currentPagePosition)
        beforePagePosition = currentPagePosition
    }

    //  anim chuyển page
    private fun animPageByPage() {
        anim?.cancel()

        val currentRect = listDraw[currentPagePosition].getRectDraw()
        val tX = width / 2 - currentRect.centerX()

        anim = ValueAnimator.ofFloat(tX)
        anim?.duration = 300
        val saveMatrix = Matrix(pdfMatrix)
        anim?.addUpdateListener {
            pdfMatrix.set(saveMatrix)
            pdfMatrix.postTranslate(it.animatedValue as Float, 0f)
            updateMatrix()
            callback.updateView()
        }
        anim?.addListener(onEnd = {
            loadHighQualityPage()
        })
        anim?.start()
    }

    private fun pageToPageNoAnim() {
        val currentRect = listDraw[currentPagePosition].getRectDraw()
        val tX = width / 2 - currentRect.centerX()
        pdfMatrix.postTranslate(tX, 0f)
        updateMatrix()
        callback.updateView()
    }

    private fun animLeftRight(isTop: Boolean) {
        anim?.cancel()

        val currentRect = listDraw[currentPagePosition].getRectDraw()
        var tX = spaceHorizontal - currentRect.left
        if (!isTop) {
            tX = width - spaceHorizontal - currentRect.right
        }
        anim = ValueAnimator.ofFloat(tX)
        anim?.duration = 300
        val saveMatrix = Matrix(pdfMatrix)
        anim?.addUpdateListener {
            pdfMatrix.set(saveMatrix)
            pdfMatrix.postTranslate(it.animatedValue as Float, 0f)
            updateMatrix()
            callback.updateView()
        }
        anim?.addListener(onEnd = {
            loadHighQualityPage()
        })
        anim?.start()
    }


}