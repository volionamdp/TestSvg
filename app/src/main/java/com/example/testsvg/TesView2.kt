package com.example.testsvg

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TesView2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var listDraw:MutableList<Page> = mutableListOf()
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        init()
    }
    private fun init(){
        listDraw.clear()
        val space = 20
        val height = width*2f
        for (i in 0..10){
            val page = Page()
            val top = i * (height+space)
            page.setRect(RectF(0f,top,width.toFloat(),top+height))
            listDraw.add(page)
        }
    }
    override fun onDraw(canvas: Canvas?) {
        for (page in listDraw){
            if (canvas != null) {
                page.draw(canvas)
            }
        }
    }
}