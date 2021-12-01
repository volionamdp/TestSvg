package com.volio.pdfediter.pdf

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.artifex.mupdfdemo.MuPDFCore

class PdfView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var viewMode: PdfViewMode = PdfViewMode.TYPE_HORIZONTAL_PAGE_BY_PAGE
    var pdfBaseMode:PdfBaseMode? = null
    var muPDFCore:MuPDFCore? = null
    var beforePage:Int = 0
    private val callback = object :PdfPageCallback{
        override fun updateView() {
            postInvalidate()
        }

        override fun changePage(position: Int) {
            Log.d("zveet2", "changePage: $position")

        }

        override fun scroll(percent: Float) {
            Log.d("zveet", "scroll: $percent")
        }

    }
    init {
        setPdfMode(viewMode)
    }
    fun setPdfCore(pdfCore: MuPDFCore){
        muPDFCore = pdfCore
        initData()
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        initData()
    }
    private fun initData(){
        muPDFCore?.let {
            if (width > 0 && height > 0) {
                pdfBaseMode?.release()
                pdfBaseMode?.initData(it, width, height)
                pdfBaseMode?.setCurrentPage(beforePage, false)
            }
        }
    }
    fun setPdfMode(pdfViewMode: PdfViewMode){
        viewMode = pdfViewMode
        pdfBaseMode?.let {
            beforePage = it.getCurrentPage()
        }
        when(viewMode){
            PdfViewMode.TYPE_VERTICAL_CONTINUOUS->{
                createPdfVerticalContinuousMode()
            }
            PdfViewMode.TYPE_VERTICAL_PAGE_BY_PAGE->{
                createPdfVerticalPageByPageMode()
            }
            PdfViewMode.TYPE_HORIZONTAL_CONTINUOUS->{
                createPdfHorizontalContinuousMode()
            }
            PdfViewMode.TYPE_HORIZONTAL_PAGE_BY_PAGE->{
                createPdfHorizontalPageByPageMode()
            }
            else->{

            }
        }
        initData()
    }
    private fun createPdfVerticalContinuousMode(){
        pdfBaseMode = PdfVerticalContinuousMode(context,callback)
    }
    private fun createPdfVerticalPageByPageMode(){
        pdfBaseMode = PdfVerticalPageByPageMode(context,callback)

    }

    private fun createPdfHorizontalContinuousMode(){
        pdfBaseMode = PdfHorizontalContinuousMode(context,callback)

    }
    private fun createPdfHorizontalPageByPageMode(){
        pdfBaseMode = PdfHorizontalPageByPageMode(context,callback)
    }


//    private fun initHorizontalPage(){
//        listDraw.clear()
//        var height = width * 1.5f
//        var totalHeight = 0f
//        val random = Random(1)
//        for (i in 0..6) {
//            height = (random.nextFloat()+1)*width
//            val page = Page(i)
//            val top = totalHeight+spaceTopBot
//            totalHeight = top + height
//            page.setRect(
//                RectF(
//                    spaceHorizontal,
//                    top,
//                    width.toFloat() - spaceHorizontal,
//                    top + height
//                )
//            )
//            listDraw.add(page)
//        }
//    }

    var time = System.currentTimeMillis()

    override fun onDraw(canvas: Canvas?) {
        time = System.currentTimeMillis()
        canvas?.let {
            pdfBaseMode?.draw(canvas)
        }
        Log.d("timeDraw", "onDraw: ${System.currentTimeMillis() - time}")
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        pdfBaseMode?.let {
            if (event != null) return it.touch(event)
        }
        return true
    }

    fun getCurrentMode() = viewMode

    companion object {
        val TYPE_DEFAULT = 0
        val TYPE_MOVE = 1
        val TYPE_SCALE = 2
        val TYPE_DRAWING = 3
        val TYPE_SELECT = 4
        val TYPE_SELECT_NON_MOVE = 5

    }
}