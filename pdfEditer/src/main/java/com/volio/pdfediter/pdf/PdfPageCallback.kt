package com.volio.pdfediter.pdf

interface PdfPageCallback {
    fun updateView()
    fun changePage(position:Int)
    //max value = 1
    fun scroll(percent:Float)
}