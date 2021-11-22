package com.example.testsvg.pdf

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class PdfView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var viewMode: PdfViewMode = PdfViewMode.TYPE_HORIZONTAL_PAGE_BY_PAGE
    var pdfBaseMode:PdfBaseMode? = null
    init {
        setPdfMode(viewMode)
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        pdfBaseMode?.initData(width,height)
    }
    fun setPdfMode(pdfViewMode: PdfViewMode){
        viewMode = pdfViewMode
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
    }
    private fun createPdfVerticalContinuousMode(){
        pdfBaseMode = PdfVerticalContinuousMode(context){
            postInvalidate()
        }.apply {
            if (width > 0 && height > 0){
                initData(width,height)
            }
        }
    }
    private fun createPdfVerticalPageByPageMode(){
        pdfBaseMode = PdfVerticalPageByPageMode(context){
            postInvalidate()
        }.apply {
            if (width > 0 && height > 0){
                initData(width,height)
            }
        }
    }

    private fun createPdfHorizontalContinuousMode(){
        pdfBaseMode = PdfHorizontalContinuousMode(context){
            postInvalidate()
        }.apply {
            if (width > 0 && height > 0){
                initData(width,height)
            }
        }
    }
    private fun createPdfHorizontalPageByPageMode(){
        pdfBaseMode = PdfHorizontalPageByPageMode(context){
            postInvalidate()
        }.apply {
            if (width > 0 && height > 0){
                initData(width,height)
            }
        }
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

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            pdfBaseMode?.draw(canvas)
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        pdfBaseMode?.let {
            if (event != null) return it.touch(event)
        }
        return true
    }


    companion object {
        val TYPE_DEFAULT = 0
        val TYPE_MOVE = 1
        val TYPE_SCALE = 2
        val TYPE_DRAWING = 3


    }
}