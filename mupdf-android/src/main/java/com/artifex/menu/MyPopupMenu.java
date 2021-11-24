package com.artifex.menu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import com.artifex.mupdfdemo.Annotation;
import com.artifex.mupdfdemo.R;
import com.artifex.mupdfdemo.ReaderView;

import java.util.ArrayList;
import java.util.List;

public class MyPopupMenu {
    private ArrayList<DefaultEditMenu> listMenu = new ArrayList<>();
    protected boolean isDrawMenu = false;
    private TextPaint textPaintMenu = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Paint backgroundPaintMenu = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Context context;
    private float width = 0;
    private float height = 0;
    private float itemMenuWidth;
    private float itemMenuHeight;
    private float shadowWidth = 0;
    private Bitmap bitmap;
    private float drawX=0,drawY = 0;
    private float marginTopBottom = 0;
    private float radius = 0;
    private static MyPopupMenu myPopupMenu = null;
    private ClickListener clickListener = null;

    public MyPopupMenu(Context context) {
        setDefaultValue();
        this.context = context;
        textPaintMenu.setTextSize(convertDpToPixel(12));
        backgroundPaintMenu.setColor(Color.WHITE);


    }
    public void destroyMyPopupMenu(){
        myPopupMenu = null;
    }
    private void setDefaultValue(){
        itemMenuWidth = convertDpToPixel(130);
        itemMenuHeight = convertDpToPixel(50);
        shadowWidth = convertDpToPixel(4);
        marginTopBottom = convertDpToPixel(10);
        radius = convertDpToPixel(4);
    }

    public Context getContext() {
        return context;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static MyPopupMenu getInstance(Context context){
        if(myPopupMenu == null){
            myPopupMenu = new MyPopupMenu(context);
            myPopupMenu.setItemMenuEditDefault();
        }else {
            if(myPopupMenu.context == null && context != null){
                myPopupMenu.context = context;
                myPopupMenu.setDefaultValue();
                myPopupMenu.createBitmap();
            }
        }
        return myPopupMenu;
    }

    public float convertDpToPixel(float dp) {
        if(context == null) {
            Log.d(TAG, "convertDpToPixel: null");
            return dp*2.5f;
        }
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public void addItem(DefaultEditMenu defaultEditMenu) {
        if (defaultEditMenu != null) {
            listMenu.add(defaultEditMenu);
        }
        createBitmap();
    }
    public void setWidthHeight(int width,int height){
        this.width = width;
        this.height = height;
    }
    public void addListItem(List<DefaultEditMenu> list) {
        if (list != null && list.size() > 0) {
            listMenu.addAll(list);
        }
        createBitmap();

    }
    private void createBitmap(){
        if(listMenu.size() == 1) marginTopBottom = convertDpToPixel(3);
        else  marginTopBottom = convertDpToPixel(10);
//        if(bitmap == null || (bitmap.getWidth() != (itemMenuWidth + shadowWidth * 2) && bitmap.getHeight() != (itemMenuHeight*listMenu.size() + shadowWidth * 2+marginTopBottom*2))) {
            long time = System.currentTimeMillis();
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setShadowLayer(shadowWidth, 0, 0, Color.argb(80, 0, 0, 0));
            bitmap = Bitmap.createBitmap((int) (itemMenuWidth + shadowWidth * 2), (int) (itemMenuHeight*listMenu.size() + shadowWidth * 2+marginTopBottom*2), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(shadowWidth, shadowWidth, shadowWidth + itemMenuWidth, shadowWidth + itemMenuHeight * listMenu.size()+marginTopBottom*2,radius,radius, paint);
            }
            Log.d(TAG, "createBitmap: "+(System.currentTimeMillis() - time)+"  -  "+listMenu.size());
//        }
    }

    public void setListItem(List<DefaultEditMenu> list) {
        listMenu.clear();
        if (list != null && list.size() > 0) {
            listMenu.addAll(list);
        }
        createBitmap();
    }

    public void setItemMenuEditDefault() {
        listMenu.clear();
        listMenu.add(new DefaultEditMenu(context.getString(R.string.copy), "menu_copy", Annotation.Type.DEFAULT));
        listMenu.add(new DefaultEditMenu(context.getString(R.string.highlight), "menu_hl", Annotation.Type.HIGHLIGHT));
        listMenu.add(new DefaultEditMenu(context.getString(R.string.underline), "menu_under", Annotation.Type.UNDERLINE));
        listMenu.add(new DefaultEditMenu(context.getString(R.string.strikethrough), "menu_strike", Annotation.Type.STRIKEOUT));
        createBitmap();

    }
    public void setItemMenuEditCopy() {
        listMenu.clear();
        listMenu.add(new DefaultEditMenu(context.getString(R.string.copy), "menu_copy", Annotation.Type.DEFAULT));
        createBitmap();

    }

    public void showMenu(float x, float y) {
        ReaderView.allowsPopupMenu = true;
        drawX = x;
        drawY = y;
        isDrawMenu = true;
        calculateSizeMenu(x, y);
    }

    public void hideMenu() {
        ReaderView.allowsPopupMenu = false;
        isDrawMenu = false;
    }

    private void calculateSizeMenu(float x, float y) {
        y = y + marginTopBottom*2;
        x = x + marginTopBottom;

        if (width != 0 && height != 0) {
            if (y + listMenu.size() * itemMenuHeight > height)
                y = height - (listMenu.size() + 1) * itemMenuHeight;
            if (x + itemMenuWidth > width) x = width - itemMenuWidth * 1.2f;

            if(y + listMenu.size() * itemMenuHeight > height - convertDpToPixel(80)&&x + itemMenuWidth > width - convertDpToPixel(70)){
                x = width - itemMenuWidth * 1.2f - convertDpToPixel(70);
            }
            float space = convertDpToPixel(10);
            float sizeHeight = listMenu.size() * itemMenuHeight;
            if(pointF1 != null&&pointF2 != null) {
                float minX = Math.max(Math.min(pointF1.x,pointF2.x),0);
                float maxX = Math.min(Math.max(pointF1.x,pointF2.x),width);
                float minY = Math.max(Math.min(pointF1.y,pointF2.y),0);
                float maxY = Math.min(Math.max(pointF1.y,pointF2.y),height);
                if(maxX - minX - 2*space - itemMenuWidth > 0) x = minX + (maxX - minX - itemMenuWidth)/2;
                if(maxY - minY - 2*space - sizeHeight > 0) y = minY + (maxY - minY - sizeHeight)/2;
                if(checkInside(x,y,x+itemMenuWidth,y+sizeHeight,pointF1)||checkInside(x,y,x+itemMenuWidth,y+sizeHeight,pointF2)){
                     minX = Math.max(Math.min(pointF1.x,pointF2.x),0);
                     maxX = Math.min(Math.max(pointF1.x,pointF2.x),width);
                    if(maxX+ itemMenuWidth + space < width){
                        x = maxX  + space;
                    }else if(minX - itemMenuWidth - space > 0){
                        x = minX - itemMenuWidth - space;
                    }else {
                        x = minX + (maxX - minX - itemMenuWidth)/2;
                    }

                }
                if(checkInside(x,y,x+itemMenuWidth,y+sizeHeight,pointF2)||checkInside(x,y,x+itemMenuWidth,y+sizeHeight,pointF1)){
                     minY = Math.max(Math.min(pointF1.y,pointF2.y),0);
                     maxY = Math.min(Math.max(pointF1.y,pointF2.y),height);

                    if(minY - space - sizeHeight > 0){
                        y = minY - space - sizeHeight;
                    }else if(maxY + space + sizeHeight < height){
                        y = maxY + space ;
                    }else {
                        y = minY + (maxY - minY - sizeHeight)/2;
                    }
                }
                if(y + listMenu.size() * itemMenuHeight > height - convertDpToPixel(80)&&x + itemMenuWidth > width - convertDpToPixel(70)){
                    if(minY - space - sizeHeight > 0) y = minY - space - sizeHeight;
                    else y = minY + space;
                    if(checkInside(x,y,x+itemMenuWidth,y+sizeHeight,pointF2)||checkInside(x,y,x+itemMenuWidth,y+sizeHeight,pointF1)){
                        if(minX - space - itemMenuWidth > 0) x = minX - space - itemMenuWidth;
                        else x = minX + space;

                    }
                }
            }
        }
        for (int i = 0; i < listMenu.size(); i++) {
            DefaultEditMenu menu = listMenu.get(i);
            menu.setRect(new RectF(x, y + i * itemMenuHeight, x + itemMenuWidth, y + (i + 1) * itemMenuHeight));
        }
    }
    private boolean checkInside(float x1,float y1,float x2,float y2,PointF pointF){
        if(x1 < pointF.x && x2 > pointF.x&&y1 < pointF.y && y2 > pointF.y){
            return true;
        }
        return false;
    }

    private static final String TAG = "MyPopupMenu";
    public void setPoint(PointF point1,PointF point2){
        pointF1 = point1;
        pointF2 = point2;
    }
    public void setPoint(float x1,float y1,float x2,float y2){
        if(pointF1 == null) pointF1 = new PointF(x1,y1);
        else pointF1.set(x1,y1);
        if (pointF2 == null) pointF2 = new PointF(x2,y2);
        else pointF2.set(x2,y2);
    }
    private PointF pointF1 ,pointF2 ;
    public void drawMenu(Canvas canvas) {
//        Log.d(TAG, "drawMenu: ");
        if (isDrawMenu&&listMenu.size()>0) {
//            Log.d(TAG, "drawMenu2: ");
            canvas.drawBitmap(bitmap,listMenu.get(0).getRect().left - shadowWidth,listMenu.get(0).getRect().top - shadowWidth - marginTopBottom,null);
            for (DefaultEditMenu editMenu : listMenu) {
                textPaintMenu.setColor(Color.BLACK);
                textPaintMenu.setUnderlineText(false);
                textPaintMenu.setStrikeThruText(false);
                textPaintMenu.setTextAlign(Paint.Align.CENTER);
                backgroundPaintMenu.setColor(Color.WHITE);
//                canvas.drawRect(editMenu.getRect(), backgroundPaintMenu);

                switch (editMenu.getType()) {
                    case HIGHLIGHT:
                        backgroundPaintMenu.setColor(Color.parseColor("#F2C94C"));
                        Rect rect = new Rect();
                        textPaintMenu.getTextBounds(editMenu.getText(), 0, editMenu.getText().length(), rect);
//                        Log.d(TAG, "drawMenu: "+rect.toString());
                        float cX = editMenu.getRect().centerX();
                        float cY = editMenu.getRect().centerY();
                        canvas.drawRect(cX - rect.width()/2f,cY - convertDpToPixel(6),cX + rect.width()/2f,cY + convertDpToPixel(8), backgroundPaintMenu);
                        break;
                    case UNDERLINE:
                        textPaintMenu.setUnderlineText(true);
                        break;
                    case STRIKEOUT:
                        textPaintMenu.setStrikeThruText(true);
                        break;
                    default:
                        break;
                }
                canvas.drawText(editMenu.getText(), editMenu.getRect().centerX(), editMenu.getRect().centerY() + convertDpToPixel(6), textPaintMenu);
            }
        }

    }
    private boolean oldIsDrawMenu = false;
    public Boolean setTouch(MotionEvent event){
        if(event.getAction() == MotionEvent.ACTION_DOWN ) {
            oldIsDrawMenu = isDrawMenu;
            Log.d(TAG, "setTouch: " );
            if(isDrawMenu && event.getPointerCount() == 1) {
                for (DefaultEditMenu editMenu : listMenu) {
                    if (editMenu.getRect().left < event.getX() && editMenu.getRect().right > event.getX() && editMenu.getRect().top < event.getY() && editMenu.getRect().bottom > event.getY()) {
                        if (clickListener != null) clickListener.clickItem(editMenu);
                        Log.d(TAG, "setTouch: " + editMenu.getText());
                        if(isDrawMenu) {
                            checkDouble();
                            isDrawMenu = false;
                        }
                        return true;
                    }
                }
            }
            if(isDrawMenu) {
                checkDouble();
                isDrawMenu = false;
            }
        }
        return false;
    }
    private boolean isCheckDouble = false;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            isCheckDouble = false;
        }
    };
    private void checkDouble(){
        isCheckDouble = true;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable,500);
    }

    public boolean isCheckDouble() {
        return isCheckDouble;
    }

    public boolean isOldIsDrawMenu() {
        return oldIsDrawMenu;
    }

    public interface ClickListener{
        void clickItem(DefaultEditMenu defaultEditMenu);
    }

}
