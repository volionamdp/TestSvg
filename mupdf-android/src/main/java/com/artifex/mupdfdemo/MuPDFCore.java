
package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFPage;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.artifex.utils.MuPdfConstant;

import java.util.ArrayList;

public class MuPDFCore {

    private int resolution;
    private Document doc;
    private Outline[] outline;
    private int pageCount = -1;
    private int currentPage;
    private Page page;
    private float pageWidth;
    private float pageHeight;
    private DisplayList displayList;
    private String path;
    private boolean isUpdate = true;

    /* Default to "A Format" pocket book size. */
    private int layoutW = 1080;
    private int layoutH = 1920;
    private int layoutEM = 20;

    /* Readable members */
    private int numPages = -1;
    //    private float pageWidth;
//    private float pageHeight;
    private long globals;
    private byte fileBuffer[];
    private String file_format;
    private boolean isUnencryptedPDF;
    private final boolean wasOpenedFromBuffer;

    /* The native functions */


//    private native long openBuffer(String magic);

//    private native String fileFormatInternal();

//    private native boolean isUnencryptedPDFInternal();

    private native int countPagesInternal();

    private native void gotoPageInternal(int localActionPageNum);

    private native float getPageWidth();

    private native float getPageHeight();

    private native void drawPage(Bitmap bitmap,
                                 int pageW, int pageH,
                                 int patchX, int patchY,
                                 int patchW, int patchH,
                                 long cookiePtr);

    private native void updatePageInternal(Bitmap bitmap,
                                           int page,
                                           int pageW, int pageH,
                                           int patchX, int patchY,
                                           int patchW, int patchH,
                                           long cookiePtr);

//    private native RectF[] searchPage(String text);

//    private native TextChar[][][][] text();

//    private native byte[] textAsHtml();

//    private native void addMarkupAnnotationInternal(PointF[] quadPoints, int type);
//
//    private native void addInkAnnotationInternal(PointF[][] arcs);
//
//    private native void deleteAnnotationInternal(int annot_index);
//
//    private native int passClickEventInternal(int page, float x, float y);
//
//    private native void setFocusedWidgetChoiceSelectedInternal(String[] selected);

    private native String[] getFocusedWidgetChoiceSelected();

    private native String[] getFocusedWidgetChoiceOptions();

    private native int getFocusedWidgetSignatureState();

//    private native String checkFocusedSignatureInternal();

//    private native boolean signFocusedSignatureInternal(String keyFile, String password);

//    private native int setFocusedWidgetTextInternal(String text);

    private native String getFocusedWidgetTextInternal();

    private native int getFocusedWidgetTypeInternal();

//    private native LinkInfo[] getPageLinksInternal(int page);

//    private native RectF[] getWidgetAreasInternal(int page);

//    private native Annotation[] getAnnotationsInternal(int page);
//
//    private native OutlineItem[] getOutlineInternal();

//    private native boolean hasOutlineInternal();
//
//    private native boolean needsPasswordInternal();
//
//    private native boolean authenticatePasswordInternal(String password);

//    private native MuPDFAlertInternal waitForAlertInternal();
//
//    private native void replyToAlertInternal(MuPDFAlertInternal alert);

    private native void startAlertsInternal();

//    private native void stopAlertsInternal();

    private native void destroying();

//    private native boolean hasChangesInternal();
//
//    private native void saveInternal();
//
//    private native long createCookie();
//
//    private native void destroyCookie(long cookie);
//
//    private native void abortCookie(long cookie);


    private native String startProofInternal(int resolution);

    private native void endProofInternal(String filename);

    private native int getNumSepsOnPageInternal(int page);

    private native int controlSepOnPageInternal(int page, int sep, boolean disable);

    private native Separation getSepInternal(int page, int sep);

//    public native boolean javascriptSupported();

    public boolean isNeedPassword() {
        if (doc != null) {
            return doc.needsPassword();
        } else {
            return false;
        }
    }

    public boolean authenticatePassword(String password) {
        return doc.authenticatePassword(password);
    }


    public void addAnnotationNew(int numPages, final ArrayList<Quad> listQuad, Annotation.Type type) {
        MuPdfConstant.isEdit = true;

//        final PDFPage p = (PDFPage) doc.loadPage(page);
//        isUpdate = true;
//        gotoPage(numPages);
        Page page = doc.loadPage(numPages);

        int typeAno = 0;
        switch (type) {
            case HIGHLIGHT:
                typeAno = PDFAnnotation.TYPE_HIGHLIGHT;
                break;
            case UNDERLINE:
                typeAno = PDFAnnotation.TYPE_UNDERLINE;
                break;
            case STRIKEOUT:
                typeAno = PDFAnnotation.TYPE_STRIKE_OUT;
                break;
        }
        if (page != null) {
            PDFAnnotation annotation = ((PDFPage) page).createAnnotation(typeAno);
            Quad[] q = new Quad[listQuad.size()];

            for (int i = 0; i < listQuad.size(); i++) q[i] = listQuad.get(i);
            annotation.setQuadPoints(q);
            annotation.update();
            ((PDFPage) page).update();
        }
    }

    public class Cookie {
        private final long cookiePtr = 100;

        public Cookie() {
            new com.artifex.mupdf.fitz.Cookie();

//            cookiePtr = createCookie();
//            if (cookiePtr == 0)
//                throw new OutOfMemoryError();
        }

        public void abort() {
//            abortCookie(cookiePtr);
        }

        public void destroy() {
            // We could do this in finalize, but there's no guarantee that
            // a finalize will occur before the muPDF context occurs.
//            destroyCookie(cookiePtr);
        }
    }

    public MuPDFCore(Context context, String filename) {
        try {
            Log.d(TAG, "MuPDFCore: "+filename);
            doc = Document.openDocument(filename);
            doc.layout(layoutW, layoutH, layoutEM);
            pageCount = doc.countPages();
        } catch (Error e) {
            e.printStackTrace();
        }


        resolution = 160;
        currentPage = -1;
        wasOpenedFromBuffer = false;
        path = filename;
    }

    public MuPDFCore(Context context, byte buffer[], String magic) throws Exception {
        doc = Document.openDocument(buffer, magic);
        doc.layout(layoutW, layoutH, layoutEM);
        pageCount = doc.countPages();
        resolution = 160;
        currentPage = -1;
        fileBuffer = buffer;
        wasOpenedFromBuffer = true;
    }


    public synchronized StructuredText.TextBlock[] getTextBlock(int numPages) {
        Page page = doc.loadPage(numPages);
        return page.toStructuredText().getBlocks();
    }

    public int countPages() {
        if (numPages < 0)
            numPages = countPagesSynchronized();
        return numPages;
    }


    private synchronized int countPagesSynchronized() {
        if(doc == null) return 0;
        return doc.countPages();
//        return countPagesInternal();
    }

    /* Shim function */


    public synchronized void gotoPage(int pageNum) {

        if (pageNum > pageCount - 1)
            pageNum = pageCount - 1;
        else if (pageNum < 0)
            pageNum = 0;

        if (pageNum != currentPage || isUpdate) {
            Log.d(TAG, "gotoPage: " + pageNum);
            currentPage = pageNum;
            if (page != null)
                page.destroy();
            page = null;
            if (displayList != null)
                displayList.destroy();
            displayList = null;
            try {
                page = doc.loadPage(pageNum);
                Rect b = page.getBounds();
                pageWidth = b.x1 - b.x0;
                pageHeight = b.y1 - b.y0;
                isUpdate = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized PointF getPageSize(int page) {
        Log.d(TAG, "getPageSize: " + page);
        gotoPage(page);
        return new PointF(pageWidth, pageHeight);
    }


    public synchronized void drawPage(Bitmap bm, int pageNum,
                                      int pageW, int pageH,
                                      int patchX, int patchY,
                                      int patchW, int patchH) {
        Log.d(TAG, "drawPage: " + pageNum);
        try {
            gotoPage(pageNum);

            if (page != null && displayList == null)
                displayList = page.toDisplayList();

            float zoom = resolution / 72f;
            Matrix ctm = new Matrix(zoom, zoom);
            RectI bbox = new RectI(page.getBounds().transform(ctm));
            float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
            float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);

            ctm.scale(xscale, yscale);

            if (displayList != null && !bm.isRecycled()) {
                try {
                    AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
                    displayList.run(dev, ctm, new com.artifex.mupdf.fitz.Cookie());
                    dev.close();
                    dev.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized void updatePage(Bitmap bm, int pageNum,
                                        int pageW, int pageH,
                                        int patchX, int patchY,
                                        int patchW, int patchH,
                                        Cookie cookie) {
        Log.d(TAG, "updatePage: " + pageNum);
        isUpdate = true;
        drawPage(bm, pageNum, pageW, pageH, patchX, patchY, patchW, patchH);
    }


    public synchronized LinkInfo[] getPageLinks(int page) {
        Log.d(TAG, "getPageLinks: ");
//        return getPageLinksInternal(page);
        return null;
    }


    public synchronized PDFAnnotation[] getPDFAnnotation(int numPages) {
        try {
            Log.d(TAG, "getPDFAnnotation: " + numPages);
            Page page = doc.loadPage(numPages);
            if (((PDFPage) page) != null) {
                PDFAnnotation[] list = ((PDFPage) page).getAnnotations();
                return list;
            } else {
                return null;
            }
        }catch (Exception e){
            return null;
        }

    }

    private static final String TAG = "MuPDFCore111";

    public synchronized void deletePDFAnnotations(int numPages, PDFAnnotation pdfAnnotation) {
        MuPdfConstant.isEdit = true;
        Log.d(TAG, "deletePDFAnnotations: ");
//        isUpdate = true;
//        gotoPage(numPages);
//        Page page =doc.loadPage(numPages);
        ((PDFPage) page).deleteAnnotation(pdfAnnotation);
        ((PDFPage) page).update();
    }

    public synchronized RectF[] searchPage(int page, String text) {
        gotoPage(page);
        return null;
    }

    private Page currentSearchPage;

    private final int spaceRect = 10;

    public synchronized ArrayList<RectF> searchPageNew2(int pageNumber, String text) {
//        gotoPage(pageNumber);
        RectF lastRect = null;
        ArrayList<RectF> listFinal = new ArrayList<>();
        if (pageNumber < 0 || pageNumber >= doc.countPages())
            return null;
        currentSearchPage = doc.loadPage(pageNumber);
        Quad[] list = currentSearchPage.search(text);
        RectF[] listRect = getRectFromQuadNew(list);

        for (RectF rect : listRect) {
            if (lastRect == null) {
                lastRect = rect;
            } else {
                if (Math.abs(rect.left - lastRect.right) < spaceRect && Math.abs(rect.top - lastRect.top) < spaceRect * 2) {
                    // truong hop bi dinh
                    lastRect.right = rect.right;
                } else {
                    listFinal.add(lastRect);
                    lastRect = rect;
                }
            }
        }
        return listFinal;
    }


    private RectF[] getRectFromQuadNew(Quad[] list) {
        RectF[] listRect = new RectF[list.length + 1];
        for (int i = 0; i < list.length; i++) {
            listRect[i] = list[i].toRectF();
        }
        listRect[list.length] = new RectF();
        return listRect;
    }


    public synchronized byte[] html(int page) {
        gotoPage(page);
        return null;
    }

    public synchronized TextWord[][] textLines(int page) {
        gotoPage(page);
        TextChar[][][][] chars = null;

        // The text of the page held in a hierarchy (blocks, lines, spans).
        // Currently we don't need to distinguish the blocks level or
        // the spans, and we need to collect the text into words.
        ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

        for (TextChar[][][] bl : chars) {
            if (bl == null)
                continue;
            for (TextChar[][] ln : bl) {
                ArrayList<TextWord> wds = new ArrayList<TextWord>();
                TextWord wd = new TextWord();

                for (TextChar[] sp : ln) {
                    for (TextChar tc : sp) {
                        if (tc.c != ' ') {
                            wd.Add(tc);
                        } else if (wd.w.length() > 0) {
                            wd.Add(tc);
                            wds.add(wd);
                            wd = new TextWord();
                        }
                    }
                }

                if (wd.w.length() > 0)
                    wds.add(wd);

                if (wds.size() > 0)
                    lns.add(wds.toArray(new TextWord[wds.size()]));
            }
        }

        return lns.toArray(new TextWord[lns.size()][]);
    }

    public synchronized boolean hasChanges() {
        return true;
    }


    public boolean hasEditedPdf() {
        try {
            return doc.hasUnsavedChanges();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }


    public void savePDFNew(String path) {
        doc.save(path, "l");

//        doc.save(path);
//        doc.saveAccelerator(path);
    }


}