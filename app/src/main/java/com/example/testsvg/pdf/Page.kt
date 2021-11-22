package com.example.testsvg.pdf

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import com.example.testsvg.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.SoftReference
import kotlin.random.Random

class Page (val context:Context,val position:Int,val viewWidth:Int,val viewHeight:Int,val updateView:()->Unit){
    private var rectDraw = RectF()
    private var rectOrigin = RectF()
    private var recBitmap = Rect()
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintDrawing = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentMatrix:Matrix = Matrix()
    private var invertMatrix:Matrix = Matrix()
    private var path = Path()
    private var job: Job? = null
    private var dataBitmap:Bitmap? = null

    init {
        paint.color = Color.BLUE
        paintDrawing.color = Color.RED
        paintDrawing.style = Paint.Style.STROKE
        paintDrawing.strokeWidth = 5f
        paint.color = Color.rgb(range.nextInt(), range.nextInt(), range.nextInt())

    }
    fun setRect(rectF: RectF){
        this.rectOrigin.set(rectF)
        this.rectDraw.set(rectF)
    }
    fun getScaleY():Float{
        return rectDraw.height()/rectOrigin.height()
    }
    fun getScaleX():Float{
        return rectDraw.width()/rectOrigin.width()
    }
    fun draw(canvas: Canvas){
        if ((rectDraw.top > 0 && rectDraw.top<viewHeight) || (rectDraw.bottom > 0 && rectDraw.bottom<viewHeight) || (rectDraw.top < 0 && rectDraw.bottom>viewHeight)) {
            canvas.drawRect(rectDraw, paint)
            dataBitmap?.let {
                canvas.drawBitmap(it,recBitmap,rectDraw,null)
            }
            canvas.save()
            canvas.setMatrix(currentMatrix)
            canvas.drawPath(path, paintDrawing)
            canvas.restore()
        }
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

    fun loadImage() {
        Log.d("zzvvv22", "loadImage: $position")
        if (job == null && dataBitmap == null){
            job = CoroutineScope(Dispatchers.Default).launch {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.anh2)
                recBitmap.set(0,0,bitmap.width,bitmap.height)
                dataBitmap = bitmap
                updateView()
                Log.d("zzvvv22", "loadImage: ok  $position")

            }
        }
    }

    fun cancelLoadImage() {
        Log.d("zzvvv22", "cancel: $position")
        dataBitmap?.recycle()
        dataBitmap = null
        job?.cancel()
        job = null
    }

    fun release(){
        job?.cancel()
        job = null

        dataBitmap?.recycle()
        dataBitmap = null
    }

    companion object{
        val range = Random(254)
    }
}