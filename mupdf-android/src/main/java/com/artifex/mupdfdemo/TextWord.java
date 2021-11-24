package com.artifex.mupdfdemo;

import android.graphics.RectF;

import java.util.ArrayList;

public class TextWord extends RectF {
	public String w;
	public ArrayList<TextChar> list;
	public TextWord() {
		super();
		w = new String();
		list  = new ArrayList<>();
	}

	//	public void Add(TextChar tc) {
//		super.union(tc);
//		w = w.concat(new String(new char[]{tc.c}));
//	}
	public void Add(TextChar tc) {
		super.union(tc);
		w = w.concat(new String(new char[]{tc.c}));
		list.add(tc);
	}
}

