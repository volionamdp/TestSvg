package com.example.testsvg

import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.artifex.mupdfdemo.MuPDFCore
import com.caverock.androidsvg.SVG
import com.example.testsvg.border.BolderImage
import com.volio.pdfediter.pdf.PdfView
import com.volio.pdfediter.pdf.PdfViewMode
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val svg = SVG.getFromAsset(assets, "h1.svg")
//        val imageView: ImageView = findViewById(R.id.test1)
//        val svg = SVG.getFromAsset(assets,"collage3_01.svg")
//        val bitmap2 = Bitmap.createBitmap(500,500,Bitmap.Config.ARGB_8888)
//        Log.d("zzz", "onCreate: ")
//        val canvas = Canvas(bitmap2)
//
//        canvas.scale(500/svg.documentWidth,500/svg.documentHeight)
//        svg.renderToCanvas(canvas)
//        imageView.setImageBitmap(bitmap2)
//        val image2: TestView = findViewById(R.id.test2)
//        image2.setSVG(svg)
//        findViewById<ConstraintLayout>(R.id.test3).apply {
//            val borderImage = findViewById<ImageView>(R.id.imgBorder)
//            val content = findViewById<View>(R.id.test1)
//            viewTreeObserver.addOnGlobalLayoutListener {
//                val border = BolderImage()
//                border.loadDataAsset(assets,"border/goc2.svg","border/canh2.svg")
//                val background = BitmapFactory.decodeResource(resources,R.drawable.background)
//                val borderData = border.getBorder(width = width,height = height ,color = Color.BLUE)
////                val borderData = border.getBorder(width = width,height = height )
//
//                borderImage.setImageBitmap(borderData.bitmap)
//                val padding = borderData.padding.toInt()
//                val layoutParams = content.layoutParams as ConstraintLayout.LayoutParams
//                layoutParams.setMargins(padding,padding,padding,padding)
//            }
//        }
        val pdf = findViewById<PdfView>(R.id.test2).apply {
            val core = MuPDFCore(context,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path+File.separator+"test.pdf")
            setPdfCore(core)
        }
        findViewById<View>(R.id.btnClick).setOnClickListener {
            if (pdf.getCurrentMode() == PdfViewMode.TYPE_HORIZONTAL_PAGE_BY_PAGE){
                pdf.setPdfMode(PdfViewMode.TYPE_HORIZONTAL_CONTINUOUS)
            }else{
                pdf.setPdfMode(PdfViewMode.TYPE_HORIZONTAL_PAGE_BY_PAGE)
            }
        }




    }
    private fun getBitmap(svg: SVG):Bitmap{
        val bitmap = Bitmap.createBitmap(500,500,Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        canvas.scale(500/svg.documentWidth,500/svg.documentHeight)
        svg.renderToCanvas(canvas)
        return bitmap
    }



    private fun testSvg(){
        val svg = SVG.getFromInputStream(FileInputStream(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath()+ File.separator+"h1.svg"))
        val imageView: ImageView = findViewById(R.id.test1)
        if (svg.documentWidth != -1f) {
            val newBM: Bitmap = Bitmap.createBitmap(
                1080,
                1080,
                Bitmap.Config.ARGB_8888
            )
            val width = svg.documentWidth
            val height = svg.documentHeight
            val bmcanvas = Canvas(newBM)
            bmcanvas.scale(1080/width,1080/height)
            // Clear background to white

            // Render our document onto our canvas
            svg.renderToCanvas(bmcanvas)
            svg.renderToCanvas(bmcanvas,true,3f,Color.BLUE)
            imageView.setImageBitmap(newBM)
//            val file = File("file:///android_asset/h1.svg")
//            Log.d("zzzzvv", "onCreate: ${file.length()}")
        }
    }
}