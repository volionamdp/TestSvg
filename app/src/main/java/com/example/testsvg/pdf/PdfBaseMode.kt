package com.example.testsvg.pdf

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent

abstract class PdfBaseMode (val context: Context,val updateView:()->Unit){
    var pageLoad:Int = 2
    abstract fun initData(viewWidth:Int,viewHeight:Int)
    abstract fun draw(canvas: Canvas)
    abstract fun touch(event: MotionEvent):Boolean
    abstract fun changePage(position:Int)
    abstract fun release()
}