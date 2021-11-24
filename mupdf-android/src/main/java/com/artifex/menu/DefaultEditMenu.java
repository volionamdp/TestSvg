package com.artifex.menu;

import android.graphics.Rect;
import android.graphics.RectF;

import com.artifex.mupdfdemo.Annotation;

public class DefaultEditMenu {
    private String text;
    private String id;
    private Annotation.Type type;
    private int index;
    private RectF Rect = new RectF();

    public DefaultEditMenu(String text, Annotation.Type type) {
        this.text = text;
        this.type = type;
    }

    public DefaultEditMenu(String text, String id, Annotation.Type type) {
        this.text = text;
        this.id = id;
        this.type = type;
    }

    public DefaultEditMenu(String text, String id, Annotation.Type type, int index) {
        this.text = text;
        this.id = id;
        this.type = type;
        this.index = index;
    }

    public DefaultEditMenu(String text, String id, Annotation.Type type, RectF rect) {
        this.text = text;
        this.id = id;
        this.type = type;
        Rect = rect;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Annotation.Type getType() {
        return type;
    }

    public void setType(Annotation.Type type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public RectF getRect() {
        return Rect;
    }

    public void setRect(RectF rect) {
        Rect = rect;
    }
}
