package com.example.testsvg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.utils.SVGBase;

public class TestView extends View {
    private SVG svg;
    private PictureDrawable pictureDrawable;
    public TestView(Context context) {
        super(context);
        init();
    }

    public TestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TestView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init(){
        setLayerType(LAYER_TYPE_HARDWARE,null);
    }
    Picture picture;
    public void setSVG(SVG svg){
        this.svg = svg;
//        setLayerType(LAYER_TYPE_SOFTWARE,null);

         picture = svg.renderToPicture();
        pictureDrawable = new PictureDrawable(picture);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long time = System.nanoTime();
//        svg.renderToCanvas(canvas);
//        picture.draw(canvas);
        pictureDrawable.setBounds(getWidth()/2,0,getWidth(),getHeight());
        pictureDrawable.draw(canvas);
        Log.d("vvvzzt", "onDraw: "+(System.nanoTime() - time)/1000);
    }
}
