package com.volio.pdfediter.pdf

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.StructuredText
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class PageTextSelectCtr {
    private var textBlock: MutableList<StructuredText.TextBlock> = mutableListOf()
    private var rectSelect: RectF = RectF()
    private var rectSelectBackup: RectF = RectF()

    private var rectTextBoxPoint1: RectF = RectF()
    private var rectTextBoxPoint2: RectF = RectF()
    private var point1 = PointF()
    private var point2 = PointF()

    var indexP1 = 0
    var indexP2 = 0
    fun changePointSelect(px0: Float, py0: Float, px1: Float, py1: Float): Boolean {
        rectSelectBackup.set(rectSelect)
        var textBlockP1: StructuredText.TextBlock? = null
        var textBlockP2: StructuredText.TextBlock? = null
        var distanceP1: Float = Float.MAX_VALUE
        var distanceP2: Float = Float.MAX_VALUE
        for ((index, item) in textBlock.withIndex()) {
            val d1 = item.bbox.getDistancePoint(px0, py0)
            val d2 = item.bbox.getDistancePoint(px1, py1)
            Log.d("zzvvv", "changePointSelect: ${d1.toInt()}   ${d2.toInt()}")
            if (d1 < distanceP1) {
                textBlockP1 = item
                distanceP1 = d1
                indexP1 = index
            }
            if (d2 < distanceP2) {
                textBlockP2 = item
                distanceP2 = d2
                indexP2 = index
            }
        }
        if (textBlockP1 != null && textBlockP2 != null) {
            when {
                indexP2 == indexP1 -> {
                    rectTextBoxPoint1.setTextBox(textBlockP1)
                    rectTextBoxPoint2.setTextBox(textBlockP2)
                    rectSelect.left = px0
                    rectSelect.top = py0
                    rectSelect.right = px1
                    rectSelect.bottom = py1
                    if (px0 < rectTextBoxPoint1.left) rectSelect.left = rectTextBoxPoint1.left
                    if (px0 > rectTextBoxPoint1.right) rectSelect.left = rectTextBoxPoint1.right
                    if (py0 < rectTextBoxPoint1.top) rectSelect.top = rectTextBoxPoint1.top
                    if (py0 > rectTextBoxPoint1.bottom) rectSelect.top = rectTextBoxPoint1.bottom

                    if (px1 < rectTextBoxPoint1.left) rectSelect.right = rectTextBoxPoint1.left
                    if (px1 > rectTextBoxPoint1.right) rectSelect.right = rectTextBoxPoint1.right
                    if (py1 < rectTextBoxPoint1.top) rectSelect.bottom = rectTextBoxPoint1.top
                    if (py1 > rectTextBoxPoint1.bottom) rectSelect.bottom = rectTextBoxPoint1.bottom
                    var line1: StructuredText.TextLine? = null
                    var line2: StructuredText.TextLine? = null
                    var spaceLine1: Float = Float.MAX_VALUE
                    var spaceLine2: Float = Float.MAX_VALUE
                    for (item in textBlockP1.lines) {
                        val space1 = abs(rectSelect.top - item.bbox.centerY())
                        val space2 = abs(rectSelect.bottom - item.bbox.centerY())
                        if (space1 < spaceLine1) {
                            spaceLine1 = space1
                            line1 = item
                        }
                        if (space2 < spaceLine2) {
                            spaceLine2 = space2
                            line2 = item
                        }
                    }
                    if (line1 != null && line2 != null) {
                        val left = rectSelect.left
                        val top = rectSelect.top
                        val right = rectSelect.right
                        val bottom = rectSelect.bottom
                        if (line1 == line2) {
                            if (left > right) {
                                rectSelect.set(right, bottom, left, top)
                                swapIndex()
                            }
                        } else {
                            if (line1.bbox.centerY() > line2.bbox.centerY()) {
                                rectSelect.set(right, bottom, left, top)
                                swapIndex()
                            }
                        }
                    }
                }
                indexP1 < indexP2 -> {
                    rectTextBoxPoint1.setTextBox(textBlockP1)
                    rectTextBoxPoint2.setTextBox(textBlockP2)
                    standardizeRect(px0, py0, true)
                    standardizeRect(px1, py1, false)
                }
                else -> {
                    rectTextBoxPoint1.setTextBox(textBlockP2)
                    rectTextBoxPoint2.setTextBox(textBlockP1)
                    standardizeRect(px0, py0, false)
                    standardizeRect(px1, py1, true)
                    swapIndex()
                }
            }
        }
        return true
    }

    private fun swapIndex() {
        val temp = indexP1
        indexP1 = indexP2
        indexP2 = temp
    }

    fun checkPointText(x: Float, y: Float): Boolean {
        for (item in textBlock) {
            if (item.bbox.contains(x, y)) return true
        }
        return false
    }


    private fun standardizeRect(x: Float, y: Float, isPoint1: Boolean) {
        if (isPoint1) {
            rectSelect.left = x
            rectSelect.top = y
            if (rectSelect.left > rectTextBoxPoint1.right) rectSelect.left =
                rectTextBoxPoint1.right
            if (rectSelect.left < rectTextBoxPoint1.left) rectSelect.left =
                rectTextBoxPoint1.left
            if (rectSelect.top > rectTextBoxPoint1.bottom) rectSelect.top =
                rectTextBoxPoint1.bottom
            if (rectSelect.top < rectTextBoxPoint1.top) rectSelect.top =
                rectTextBoxPoint1.top
        } else {
            rectSelect.right = x
            rectSelect.bottom = y
            if (rectSelect.right > rectTextBoxPoint2.right) rectSelect.right =
                rectTextBoxPoint2.right
            if (rectSelect.right < rectTextBoxPoint2.left) rectSelect.right =
                rectTextBoxPoint2.left
            if (rectSelect.bottom > rectTextBoxPoint2.bottom) rectSelect.bottom =
                rectTextBoxPoint2.bottom
            if (rectSelect.bottom < rectTextBoxPoint2.top) rectSelect.bottom =
                rectTextBoxPoint2.top
        }
    }

    private fun Rect.getDistancePoint(x: Float, y: Float): Float {
        var distance = Float.MAX_VALUE
        if (x > x0 && x < x1) {
            distance = if (y > y0 && y < y1) {
                0f
            } else {
                min(distance, min(abs(y - y1), abs(y0 - y)))
            }
        } else if (x < x0) {
            distance = if (y > y0 && y < y1) {
                x0 - x
            } else {
                min(distance, abs(min(hypot(x0 - x, y - y1), hypot(x0 - x, y0 - y))))
            }
        } else {
            distance = if (y > y0 && y < y1) {
                x - x1
            } else {
                min(distance, abs(min(hypot(x - x1, y - y1), hypot(x - x1, y0 - y))))
            }
        }
        return distance
    }

    private fun RectF.setTextBox(textBlock: StructuredText.TextBlock) {
        textBlock.bbox.let { box ->
            set(box.x0, box.y0, box.x1, box.y1)
        }
    }

    fun getListRectSelect(): List<RectF> {
        val listRect: MutableList<RectF> = mutableListOf()
        var positionP1 = 0
        var positionP2 = 0
        val textBlock1 = textBlock[indexP1].apply {
            positionP1 = getLine(this.lines, rectSelect.top)
            val line = lines[positionP1]
            rectSelect.top = line.bbox.centerY()
        }
        val textBlock2 = textBlock[indexP2].apply {
            positionP2 = getLine(this.lines, rectSelect.bottom)
            val line = lines[positionP2]
            rectSelect.bottom = line.bbox.centerY()
        }
        if (indexP1 == indexP2) {
            if (positionP1 == positionP2) {
                textBlock1.lines[positionP1].apply {
                    listRect.add(
                        getRectP1P2(
                            rectSelect.left,
                            rectSelect.top,
                            rectSelect.right,
                            rectSelect.bottom
                        )
                    )
                }
            } else {
                textBlock1.lines[positionP1].apply {
                    listRect.add(getRectP1(rectSelect.left, rectSelect.top))
                }
                for (index in positionP1 + 1 until positionP2) {
                    listRect.add(textBlock1.lines[index].bbox.toRectF())
                }
                textBlock2.lines[positionP2].apply {
                    listRect.add(getRectP2(rectSelect.right, rectSelect.bottom))
                }
            }
        } else {
            textBlock1.lines[positionP1].apply {
                listRect.add(getRectP1(rectSelect.left, rectSelect.top))
            }
            for (index in positionP1 + 1 until textBlock1.lines.size) {
                listRect.add(textBlock1.lines[index].bbox.toRectF())
            }
            for (index in indexP1 + 1 until indexP2) {
                val lines = textBlock[index].lines
                for (item in lines) {
                    listRect.add(item.bbox.toRectF())
                }
            }
            for (index in 0 until positionP2) {
                listRect.add(textBlock2.lines[index].bbox.toRectF())
            }
            textBlock2.lines[positionP2].apply {
                listRect.add(getRectP2(rectSelect.right, rectSelect.bottom))
            }
        }
        return listRect
    }

    private fun StructuredText.TextLine.getRectP1P2(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float
    ): RectF {
        val rectF: RectF = RectF(-1f, y0, -1f, y0)
        for (char in chars) {
            val rect = char.quad.toRect()
            if ((rect.x0 >= x0 || rect.contains(
                    x0,
                    rect.centerY()
                )) && (rect.x1 <= x1 || rect.contains(x1, rect.centerY()))
            ) {
                if (rectF.left == -1f) {
                    rectF.left = rect.x0
                    rectF.right = rect.x1
                }
                rectF.insert(rect)
            }
        }

        rectSelect.left = rectF.left
        rectSelect.right = rectF.right

        point1.set(rectF.left, rectF.centerY())
        point2.set(rectF.right, rectF.centerY())

        return rectF
    }

    private fun StructuredText.TextLine.getRectP1(x0: Float, y0: Float): RectF {
        val rectF: RectF = RectF(-1f, y0, -1f, y0)
        for (char in chars) {
            val rect = char.quad.toRect()
            if (rect.x0 >= x0 || rect.contains(x0, rect.centerY())) {
                if (rectF.left == -1f) {
                    rectF.left = rect.x0
                    rectF.right = rect.x1
                }
                rectF.insert(rect)
            }
        }
        rectSelect.left = rectF.left

        point1.set(rectF.left, rectF.centerY())

        return rectF
    }

    private fun StructuredText.TextLine.getRectP2(x1: Float, y1: Float): RectF {
        val rectF: RectF = RectF(-1f, y1, -1f, y1)
        for (char in chars) {
            val rect = char.quad.toRect()
            if (rect.x1 <= x1 || rect.contains(x1, rect.centerY())) {
                if (rectF.left == -1f) {
                    rectF.left = rect.x0
                    rectF.right = rect.x1
                    rectF.top = rect.y0
                    rectF.bottom = rect.y0
                }

                rectF.insert(rect)

            }
        }
        rectSelect.right = rectF.right
        point2.set(rectF.right, rectF.centerY())
//        if (rectF.right == -1f && chars.isNotEmpty()) point2.x = chars[0].quad.toRect().x0
        return rectF
    }

    private fun getLine(lines: Array<StructuredText.TextLine>, pointY: Float): Int {
        var line: StructuredText.TextLine? = null
        var spaceLine: Float = Float.MAX_VALUE
        var position: Int = 0
        for ((index, item) in lines.withIndex()) {
            val space = abs(pointY - item.bbox.centerY())
            if (space < spaceLine) {
                spaceLine = space
                line = item
                position = index
            }
        }
        return position
    }

    private fun RectF.insert(rect: Rect) {
        if (left > rect.x0) left = rect.x0
        if (right < rect.x1) right = rect.x1
        if (top > rect.y0) top = rect.y0
        if (bottom < rect.y1) bottom = rect.y1
    }

    fun getRectSelect() = rectSelect
    fun getPoint2() = point2
    fun getPoint1() = point1
    fun setListTextBlock(list: MutableList<StructuredText.TextBlock>) {
        textBlock.clear()
        textBlock.addAll(list)
    }

}