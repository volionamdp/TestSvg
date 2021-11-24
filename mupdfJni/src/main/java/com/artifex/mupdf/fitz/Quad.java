package com.artifex.mupdf.fitz;

import android.graphics.RectF;

public class Quad
{
	public float ul_x, ul_y; // u up l = le
	public float ur_x, ur_y;
	public float ll_x, ll_y;
	public float lr_x, lr_y;

//	public Quad() {
//		ul_x = Float.MAX_VALUE;
//
//	}

	public Quad(Quad quad){
		this.ul_x = quad.ul_x;
		this.ul_y = quad.ul_y;
		this.ur_x = quad.ur_x;
		this.ur_y = quad.ur_y;
		this.ll_x = quad.ll_x;
		this.ll_y = quad.ll_y;
		this.lr_x = quad.lr_x;
		this.lr_y = quad.lr_y;
	}

	public Quad(float ul_x, float ul_y, float ur_x, float ur_y, float ll_x, float ll_y, float lr_x, float lr_y) {
		this.ul_x = ul_x;
		this.ul_y = ul_y;
		this.ur_x = ur_x;
		this.ur_y = ur_y;
		this.ll_x = ll_x;
		this.ll_y = ll_y;
		this.lr_x = lr_x;
		this.lr_y = lr_y;
	}

	public Rect toRect() {
		float x0 = Math.min(Math.min(ul_x, ur_x), Math.min(ll_x, lr_x));
		float y0 = Math.min(Math.min(ul_y, ur_y), Math.min(ll_y, lr_y));
		float x1 = Math.max(Math.max(ul_x, ur_x), Math.max(ll_x, lr_x));
		float y1 = Math.max(Math.max(ul_y, ur_y), Math.max(ll_y, lr_y));
		return new Rect(x0, y0, x1, y1);
	}

	public RectF toRectF() {
		float x0 = Math.min(Math.min(ul_x, ur_x), Math.min(ll_x, lr_x));
		float y0 = Math.min(Math.min(ul_y, ur_y), Math.min(ll_y, lr_y));
		float x1 = Math.max(Math.max(ul_x, ur_x), Math.max(ll_x, lr_x));
		float y1 = Math.max(Math.max(ul_y, ur_y), Math.max(ll_y, lr_y));
		return new RectF(x0, y0, x1, y1);
	}


	public Quad transformed(Matrix m) {
		float t_ul_x = ul_x * m.a + ul_y * m.c + m.e;
		float t_ul_y = ul_x * m.b + ul_y * m.d + m.f;
		float t_ur_x = ur_x * m.a + ur_y * m.c + m.e;
		float t_ur_y = ur_x * m.b + ur_y * m.d + m.f;
		float t_ll_x = ll_x * m.a + ll_y * m.c + m.e;
		float t_ll_y = ll_x * m.b + ll_y * m.d + m.f;
		float t_lr_x = lr_x * m.a + lr_y * m.c + m.e;
		float t_lr_y = lr_x * m.b + lr_y * m.d + m.f;
		return new Quad(
			t_ul_x, t_ul_y,
			t_ur_x, t_ur_y,
			t_ll_x, t_ll_y,
			t_lr_x, t_lr_y
		);
	}

	public Quad transform(Matrix m) {
		float t_ul_x = ul_x * m.a + ul_y * m.c + m.e;
		float t_ul_y = ul_x * m.b + ul_y * m.d + m.f;
		float t_ur_x = ur_x * m.a + ur_y * m.c + m.e;
		float t_ur_y = ur_x * m.b + ur_y * m.d + m.f;
		float t_ll_x = ll_x * m.a + ll_y * m.c + m.e;
		float t_ll_y = ll_x * m.b + ll_y * m.d + m.f;
		float t_lr_x = lr_x * m.a + lr_y * m.c + m.e;
		float t_lr_y = lr_x * m.b + lr_y * m.d + m.f;
		ul_x = t_ul_x;
		ul_y = t_ul_y;
		ur_x = t_ur_x;
		ur_y = t_ur_y;
		ll_x = t_ll_x;
		ll_y = t_ll_y;
		lr_x = t_lr_x;
		lr_y = t_lr_y;
		return this;
	}

	public String toString() {
		return "["
			+ ul_x + " " + ul_y + " "
			+ ur_x + " " + ur_y + " "
			+ ll_x + " " + ll_y + " "
			+ lr_x + " " + lr_y
			+ "]";
	}

	public void union(Quad quad) {
		if(ll_x > quad.ll_x) ll_x = quad.ll_x;
		if(ll_y < quad.ll_y) ll_y = quad.ll_y;

		if(lr_y < quad.lr_y) lr_y = quad.lr_y;
		if(lr_x < quad.lr_x) lr_x = quad.lr_x;

		if(ul_x > quad.ul_x) ul_x = quad.ul_x;
		if(ul_y > quad.ul_y) ul_y = quad.ul_y;

		if(ur_x < quad.ur_x) ur_x = quad.ur_x;
		if(ur_y > quad.ur_y) ur_y = quad.ur_y;

	}
}
