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
import androidx.core.animation.doOnEnd
import com.artifex.mupdfdemo.MuPDFCore
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

abstract class PdfBaseMode(val context: Context, val callback: PdfPageCallback) {
    var pageLoad: Int = 2
    protected var core: MuPDFCore? = null
    protected var currentPagePosition: Int = 0
    protected var typeTouch: Int = PdfView.TYPE_MOVE
    protected var listDraw: MutableList<Page> = mutableListOf()
    protected var currentPage: Page? = null
    protected var pdfMatrix: Matrix = Matrix()
    protected var fixedPointSelect = PointF()
    protected val minScale = 1f
    protected val maxScale = 3f
    private var spaceTouchSelect: Float = 200f
    protected var animZoom: ValueAnimator? = null

    private var spaceDownX = 0f
    private var spaceDownY = 0f

    protected val gestureDetector: GestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                fling(e1, e2, velocityX, velocityY)
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                if (currentPage?.isSelect() != true) {
                    e?.let {
                        Log.d("zzzvv", "onLongPress: ")
                        for (item in listDraw) {
                            if (item.getRectDraw().contains(e.x, e.y) && item.checkPointText(
                                    e.x,
                                    e.y
                                )
                            ) {
                                typeTouch = PdfView.TYPE_SELECT
                                currentPage = item
                                fixedPointSelect.set(e.x, e.y)
                                currentPage?.setSelect(true)
                                Log.d("zzzvv", "onLongPress123: ")

                                break
                            }
                        }
                    }
                }
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                Log.d("zzvvee3", "onDoubleTap: ${e?.x}")
                e?.let {
                    animZoom(e.x, e.y)
                }
                return super.onDoubleTap(e)
            }

        })

    fun getCurrentPage() = currentPagePosition
    abstract fun setCurrentPage(position: Int, isAnim: Boolean)
    abstract fun initData(muPDFCore: MuPDFCore, viewWidth: Int, viewHeight: Int)
    abstract fun draw(canvas: Canvas)
    abstract fun touch(event: MotionEvent): Boolean
    protected abstract fun fling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    )

    protected abstract fun updateScroll()
    protected abstract fun changePage(position: Int)
    protected abstract fun updatePage()
    protected abstract fun cancelAnim()
    protected abstract fun endAnimZoom()
    abstract fun release()
    fun getPageCount(): Int {
        Log.d("zzzvv", "getPageCount: ${core == null} ${core?.countPages()}")

        core?.let {
            return it.countPages()
        }
        return 0
    }

    fun getPageSize(index: Int): RectF {
        var width = 1f
        var height = 1f
        core?.let {
            val pointF = it.getPageSize(index)
            width = pointF.x
            height = pointF.y
        }
        return RectF(0f, 0f, width, height)
    }

    protected fun loadHighQualityPage() {
        for (item in listDraw) item.loadImageScale()
    }

    protected fun moveTouchEvent(event: MotionEvent, onMove: () -> Unit, onScale: () -> Unit) {
        when {
            typeTouch == PdfView.TYPE_MOVE -> {
                onMove()
            }
            event.pointerCount == 2 && typeTouch == PdfView.TYPE_SCALE -> {
                onScale()
            }
            typeTouch == PdfView.TYPE_SELECT -> {
                Log.d("zzvvzze", "downTouchEvent:33355----------")

                currentPage?.changePointSelect(
                    fixedPointSelect.x,
                    fixedPointSelect.y,
                    event.x - spaceDownX,
                    event.y - spaceDownY
                )
                currentPage?.let {

                }
            }
        }
    }

    protected fun downTouchEvent(event: MotionEvent) {
//        when{
//            typeTouch == PdfView.TYPE_SELECT || typeTouch == PdfView.TYPE_SELECT_NON_MOVE->{
        currentPage?.let {
            val rectSelect = it.getRectSelect()
            val spaceP1 = abs(hypot(event.x - rectSelect.left, event.y - rectSelect.top))
            val spaceP2 = abs(hypot(event.x - rectSelect.right, event.y - rectSelect.bottom))
            Log.d("zzvvvv", "downTouchEvent: $spaceP1 $spaceP2")
            if (min(spaceP1, spaceP2) > spaceTouchSelect) {
                typeTouch = PdfView.TYPE_MOVE
            } else {
                if (spaceP1 < spaceP2) {
                    fixedPointSelect.set(rectSelect.right, rectSelect.bottom)
                    spaceDownX = event.x - rectSelect.left
                    spaceDownY = event.y - rectSelect.top
                } else {
                    fixedPointSelect.set(rectSelect.left, rectSelect.top)
                    spaceDownX = event.x - rectSelect.right
                    spaceDownY = event.y - rectSelect.bottom
                }
                typeTouch = PdfView.TYPE_SELECT
            }
        }
//            }
//
//        }
    }

    protected fun animZoom(x: Float, y: Float) {
        if (listDraw.size > 0) {
            val page = listDraw.first()
            val currentScale = page.getScaleX()
            var end = maxScale
            if (currentScale == maxScale) {
                end = minScale
            }
            cancelAnim()
            animZoom?.cancel()
            animZoom = ValueAnimator.ofFloat(currentScale, end)
            val saveMatrix = Matrix(pdfMatrix)

            Log.d("zzvvv", "animZoom: ${currentScale} $end --------------")
            animZoom?.addUpdateListener {
                val scale = (it.animatedValue as Float) / currentScale
                Log.d("zzvvv", "animZoom: ${currentScale} ${it.animatedValue}")
                pdfMatrix.set(saveMatrix)
                pdfMatrix.postScale(scale, scale, x, y)
                updatePage()
                callback.updateView()
            }
            animZoom?.doOnEnd {
                endAnimZoom()
            }
            animZoom?.start()
        }
    }

}