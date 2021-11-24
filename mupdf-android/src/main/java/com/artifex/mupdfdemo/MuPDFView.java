package com.artifex.mupdfdemo;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

;import com.artifex.callback.SingleClickCallback;

public interface MuPDFView {
    public void setPage(int page, PointF size);

    public void setScale(float scale);

    public int getPage();

    public void blank(int page);

    public Hit passClickEvent(float x, float y);

    public void passClickEvent(float x, float y, SingleClickCallback singleClickCallback);


    public LinkInfo hitLink(float x, float y);

    public void selectText(float x0, float y0, float x1, float y1);

    public PageView.HIGHLIGHT_MODE selectTextOnDown(float x, float y);

    public void deselectText();

    public boolean copySelection();

    public boolean markupSelection(Annotation.Type type);

    public void deleteSelectedAnnotation();

    public void setSearchBoxes(RectF searchBoxes[]);

    public void setLinkHighlighting(boolean f);

    public void deselectAnnotation();

    public void startDraw(float x, float y);

    public void continueDraw(float x, float y);

    public void cancelDraw();

    public boolean saveDraw();

    public void setChangeReporter(Runnable reporter);

    public void update();

    public void updateHq(boolean update);

    public void removeHq();

    public void releaseResources();

    public void releaseBitmaps();

    public void touchUp();

    public void setModeEdit(boolean isEdit);

    public void showPopupMenuDelete(MotionEvent event);

    public void autoDelete(float x, float y);

    public void longSelectText(float x0, float y0, float x1, float y1);

    public void setSearchRect(RectF rect);

    public void dispatchDrawViewGroup(Canvas canvas, int width, int height);

    public boolean checkClickMenu(MotionEvent event);

    public void updatePosition(float x, float y);

}

