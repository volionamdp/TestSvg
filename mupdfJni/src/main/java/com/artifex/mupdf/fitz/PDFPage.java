package com.artifex.mupdf.fitz;

public class PDFPage extends Page
{
	static {
		Context.init();
	}

	private PDFPage(long p) { super(p); }

	public synchronized native PDFAnnotation[] getAnnotations();
	public synchronized native PDFAnnotation createAnnotation(int subtype);
	public synchronized native void deleteAnnotation(PDFAnnotation annot);

	public native boolean applyRedactions();

	public synchronized native boolean update();

	private PDFWidget[] widgets;
	private native PDFWidget[] getWidgetsNative();

	public PDFWidget[] getWidgets() {
		if (widgets == null)
			widgets = getWidgetsNative();
		return widgets;
	}

	public PDFWidget activateWidgetAt(float pageX, float pageY) {
		for (PDFWidget widget : getWidgets()) {
			if (widget.getBounds().contains(pageX, pageY)) {
				widget.eventEnter();
				widget.eventDown();
				widget.eventFocus();
				widget.eventUp();
				widget.eventExit();
				widget.eventBlur();
				return widget;
			}
		}
		return null;
	}
}
