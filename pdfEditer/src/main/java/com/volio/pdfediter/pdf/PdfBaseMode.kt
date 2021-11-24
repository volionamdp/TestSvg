package com.volio.pdfediter.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import com.artifex.mupdfdemo.MuPDFCore

abstract class PdfBaseMode (val context: Context,val updateView:()->Unit){
    var pageLoad:Int = 2
    protected var core:MuPDFCore? = null
    protected var currentPagePosition: Int = 0
    fun getPageCount():Int{
        Log.d("zzzvv", "getPageCount: ${core == null} ${core?.countPages()}")

        core?.let {
            return it.countPages()
        }
        return  0
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
    fun getCurrentPage() = currentPagePosition
    abstract fun setCurrentPage(position: Int,isAnim:Boolean)
    abstract fun initData(muPDFCore: MuPDFCore,viewWidth:Int,viewHeight:Int)
    abstract fun draw(canvas: Canvas)
    abstract fun touch(event: MotionEvent):Boolean
    protected abstract fun changePage(position:Int)
    abstract fun release()
}