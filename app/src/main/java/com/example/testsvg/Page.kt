package com.example.testsvg

import android.graphics.*
import android.view.MotionEvent

class Page (val position:Int){
    private var rectDraw = RectF()
    private var rectOrigin = RectF()
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintDrawing = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentMatrix:Matrix = Matrix()
    private var invertMatrix:Matrix = Matrix()
    private var path = Path()
    init {
        paint.color = Color.BLUE
        paintDrawing.color = Color.RED
        paintDrawing.style = Paint.Style.STROKE
        paintDrawing.strokeWidth = 5f
    }
    fun setRect(rectF: RectF){
        this.rectOrigin.set(rectF)
        this.rectDraw.set(rectF)
    }
    fun draw(canvas: Canvas){
        canvas.drawRect(rectDraw,paint)
        canvas.save()
        canvas.setMatrix(currentMatrix)
        canvas.drawPath(path,paintDrawing)
        canvas.restore()
    }
    fun getRectDraw():RectF{
        return rectDraw
    }


    fun updateMatrix(matrix: Matrix) {
        matrix.mapRect(rectDraw,rectOrigin)
        matrix.invert(invertMatrix)
        currentMatrix.set(matrix)

    }

    private var dstPointMap:FloatArray = floatArrayOf(0f,0f)
    private var srcPointMap:FloatArray = floatArrayOf(0f,0f)
    private fun mapPoint(event: MotionEvent,matrix: Matrix,onMap:(x:Float,y:Float)->Unit){
        srcPointMap[0] = event.x
        srcPointMap[1] = event.y
        matrix.mapPoints(dstPointMap,srcPointMap)
        onMap(dstPointMap[0],dstPointMap[1])
    }
    fun moveTouch(event: MotionEvent) {
        mapPoint(event,invertMatrix){x,y->
            path.lineTo(x,y)
        }
    }


    fun downTouch(event: MotionEvent) {
        mapPoint(event,invertMatrix){x,y->
            path.moveTo(x,y)
        }
//        path.moveTo(event.x,event.y)
    }
}