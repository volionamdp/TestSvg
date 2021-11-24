package com.example.testsvg.border

import android.content.res.AssetManager
import android.graphics.*
import com.caverock.androidsvg.SVG
import kotlin.math.abs
import kotlin.math.min

class BolderImage {
    private var cornerSvg: SVG? = null
    private var edgeSvg: SVG? = null
    private var maxEdgeHeight: Float = 50f
    private var minEdgeHeight: Float = 20f
    private val matrix = Matrix()

    fun loadDataAsset(assetManager: AssetManager, corner: String, edge: String) {
        cornerSvg = SVG.getFromAsset(assetManager, corner)
        edgeSvg = SVG.getFromAsset(assetManager, edge)
    }

    fun getBorder(width: Int, height: Int,color:Int? = null,background:Bitmap? = null):BorderData {
        minEdgeHeight = min(width, height) /60f
        maxEdgeHeight = min(width,height)/30f
        val edgeData = getBorderHeight(width.toFloat(), height.toFloat())
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val corner = cornerSvg
        val edge = edgeSvg
        if (corner != null && edge != null) {
            drawAllCorner(canvas,width,height, corner, edgeData)
            drawAllEdge(canvas,width,height, edge, edgeData)
            color?.let {
                drawBackgroundColor(canvas,width,height,it)
            }
            background?.let {
                drawBackground(canvas,width,height,it)
            }
        }
        return BorderData(bitmap,edgeData.paddingContent)

    }

    private fun drawBackgroundColor(canvas: Canvas, width: Int, height: Int, color: Int) {
        canvas.save()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        paint.color = color
        canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas,width: Int, height: Int,background: Bitmap) {
        canvas.save()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ratioBackground = background.width.toFloat()/background.height
        val ratioView = width.toFloat()/height
        if (ratioView > ratioBackground){
            val scale = width.toFloat()/background.width
            matrix.setTranslate((width-background.width)/2f,(height-background.height)/2f)
            matrix.postScale(scale,scale,width/2f,height/2f)
        }else{
            val scale = height.toFloat()/background.height
            matrix.setTranslate((width-background.width)/2f,(height-background.height)/2f)
            matrix.postScale(scale,scale,width/2f,height/2f)
        }
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(background,matrix,paint)
        canvas.restore()
    }

    private fun drawAllEdge(canvas: Canvas,width: Int,height: Int,edge: SVG,edgeData:EdgeData){
        val verticalCount = ((width - 2 * edgeData.cornerWidth) / edgeData.edgeWidth).toInt()
        val verticalEdgeWidth = (width - 2 * edgeData.cornerWidth) / verticalCount
        val horizontalCount = ((height - 2 * edgeData.cornerWidth) / edgeData.edgeWidth).toInt()
        val horizontalEdgeWidth = (height - 2 * edgeData.cornerWidth) / horizontalCount
        val edgePicture = edge.renderToPicture()

        val scaleVerticalX = (verticalEdgeWidth+0.01f)/edgePicture.width
        val scaleVerticalY = edgeData.edgeHeight/edgePicture.height

        val scaleHorizontalX = (horizontalEdgeWidth+0.01f)/edgePicture.width
        val scaleHorizontalY = edgeData.edgeHeight/edgePicture.height

        for (index in 0 until verticalCount){
            drawPicture(canvas,edgePicture,edgeData.cornerWidth+(index+0.5f)*verticalEdgeWidth,edgeData.edgeHeight/2f,0f,scaleVerticalX,scaleVerticalY)
            drawPicture(canvas,edgePicture,edgeData.cornerWidth+(index+0.5f)*verticalEdgeWidth,height-edgeData.edgeHeight/2f,180f,scaleVerticalX,scaleVerticalY)
        }

        for (index in 0 until horizontalCount){
            drawPicture(canvas,edgePicture,edgeData.edgeHeight/2f,edgeData.cornerWidth+(index+0.5f)*horizontalEdgeWidth,-90f,scaleHorizontalX,scaleHorizontalY)
            drawPicture(canvas,edgePicture,width-edgeData.edgeHeight/2f,edgeData.cornerWidth+(index+0.5f)*horizontalEdgeWidth,90f,scaleHorizontalX,scaleHorizontalY)
        }

    }
    private fun drawAllCorner(canvas: Canvas,width: Int,height: Int,corner: SVG,edgeData:EdgeData){
        val cornerPicture = corner.renderToPicture()
        val cornerScale = edgeData.cornerWidth / cornerPicture.width
        drawPicture(
            canvas,
            cornerPicture,
            edgeData.cornerWidth / 2f,
            edgeData.cornerWidth / 2f,
            0f,
            cornerScale,
            cornerScale
        )
        drawPicture(
            canvas,
            cornerPicture,
            width - edgeData.cornerWidth / 2f,
            edgeData.cornerWidth / 2f,
            90f,
            cornerScale,
            cornerScale
        )
        drawPicture(
            canvas,
            cornerPicture,
            width - edgeData.cornerWidth / 2f,
            height - edgeData.cornerWidth / 2f,
            180f,
            cornerScale,
            cornerScale
        )
        drawPicture(
            canvas,
            cornerPicture,
            edgeData.cornerWidth / 2f,
            height-edgeData.cornerWidth / 2f,
            270f,
            cornerScale,
            cornerScale
        )
    }

    private fun drawPicture(
        canvas: Canvas,
        picture: Picture,
        centX: Float,
        centY: Float,
        rotate: Float,
        scaleX: Float,
        scaleY:Float
    ) {
        matrix.setTranslate(centX - picture.width/2f, centY - picture.height/2f)
        matrix.postScale(scaleX,scaleY,centX,centY)
        matrix.postRotate(rotate,centX,centY)
        canvas.save()
        canvas.setMatrix(matrix)
        picture.draw(canvas)
        canvas.restore()
    }

    private fun getRealEdgeHeightMaxMin(edge: SVG): Pair<Int,Int> {
        val bitmap: Bitmap = Bitmap.createBitmap(
            edge.documentWidth.toInt(),
            edge.documentHeight.toInt(),
            Bitmap.Config.ALPHA_8
        )
        val canvas = Canvas(bitmap)
        edge.renderToCanvas(canvas)
        var min = bitmap.height
        var max = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val alpha = Color.alpha(bitmap.getPixel(x, y))
                if (alpha > 10) {
                    if (y > max) max = y
                    if (y < min) min = y
                }
            }
        }
        return Pair(max,min)
    }

    private fun getBorderHeight(width: Float, height: Float): EdgeData {
        val corner = cornerSvg
        val edge = edgeSvg
        val listEdgeData: MutableList<EdgeData> = mutableListOf()
        val listEdgeBackup: MutableList<EdgeData> = mutableListOf()
        if (corner != null && edge != null) {
            val maxMin = getRealEdgeHeightMaxMin(edge)
            val realEdgeHeight = maxMin.first - maxMin.second
            val scale = edge.documentHeight / realEdgeHeight
            val scalePaddingContent = maxMin.first.toFloat()/realEdgeHeight
            for (value in minEdgeHeight.toInt()..maxEdgeHeight.toInt()) {
                val edgeHeight = (scale * value)
                val edgeWidth = edge.documentWidth * edgeHeight / edge.documentHeight
                val cornerWidth = edgeWidth * corner.documentWidth / edge.documentWidth
                val paddingContent = scalePaddingContent*value
                val remainderW = (width - 2 * cornerWidth) % (edgeWidth)
                val remainderH = (height - 2 * cornerWidth) % (edgeWidth)
                if ((width - 2 * cornerWidth) / (edgeWidth) >= 1 && (height - 2 * cornerWidth) / (edgeWidth)>=1) {
                    listEdgeData.add(
                        EdgeData(
                            edgeWidth,
                            edgeHeight,
                            cornerWidth,
                            abs(remainderW - remainderH),
                            paddingContent
                        )
                    )
                }else{
                    listEdgeBackup.add(
                        EdgeData(
                            edgeWidth,
                            edgeHeight,
                            cornerWidth,
                            abs(remainderW - remainderH),
                            paddingContent
                        )
                    )
                }
            }
            if (listEdgeData.size == 0) listEdgeData.addAll(listEdgeBackup)
            listEdgeData.sortBy { it.edgeSpace }
            return listEdgeData.first()
        }
        return EdgeData(1f, 1f, 1f, 1f,0f)
    }

    private data class EdgeData(
        val edgeWidth: Float,
        val edgeHeight: Float,
        val cornerWidth: Float,
        val edgeSpace: Float,
        val paddingContent:Float
    )
    data class BorderData(val bitmap: Bitmap,val padding:Float)

}