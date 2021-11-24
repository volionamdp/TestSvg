package com.artifex.mupdfdemo;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.artifex.callback.SingleClickCallback;
import com.artifex.menu.CustomMenuAdapter;
import com.artifex.menu.DefaultEditMenu;
import com.artifex.menu.MyPopupMenu;
import com.artifex.menu.PhotorThread;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.mupdfdemo.MuPDFCore.Cookie;
import com.skydoves.powermenu.CustomPowerMenu;

import java.util.ArrayList;
import java.util.HashMap;

import static com.artifex.mupdfdemo.ReaderView.ModeAuto.AUTO_DELETE;
import static com.artifex.mupdfdemo.ReaderView.ModeAuto.AUTO_HIGHLIGHT;
import static com.artifex.mupdfdemo.ReaderView.ModeAuto.AUTO_STRIKE_THROUGH;
import static com.artifex.mupdfdemo.ReaderView.ModeAuto.AUTO_UNDERLINE;

/* This enum should be kept in line with the cooresponding C enum in mupdf.c */
enum SignatureState {
    NoSupport,
    Unsigned,
    Signed
}

abstract class PassClickResultVisitor {
    public abstract void visitText(PassClickResultText result);

    public abstract void visitChoice(PassClickResultChoice result);

    public abstract void visitSignature(PassClickResultSignature result);
}

class PassClickResult {
    public final boolean changed;

    public PassClickResult(boolean _changed) {
        changed = _changed;
    }

    public void acceptVisitor(PassClickResultVisitor visitor) {
    }
}

class PassClickResultText extends PassClickResult {
    public final String text;

    public PassClickResultText(boolean _changed, String _text) {
        super(_changed);
        text = _text;
    }

    public void acceptVisitor(PassClickResultVisitor visitor) {
        visitor.visitText(this);
    }
}

class PassClickResultChoice extends PassClickResult {
    public final String[] options;
    public final String[] selected;

    public PassClickResultChoice(boolean _changed, String[] _options, String[] _selected) {
        super(_changed);
        options = _options;
        selected = _selected;
    }

    public void acceptVisitor(PassClickResultVisitor visitor) {
        visitor.visitChoice(this);
    }
}

class PassClickResultSignature extends PassClickResult {
    public final SignatureState state;

    public PassClickResultSignature(boolean _changed, int _state) {
        super(_changed);
        state = SignatureState.values()[_state];
    }

    public void acceptVisitor(PassClickResultVisitor visitor) {
        visitor.visitSignature(this);
    }
}

public class MuPDFPageView extends PageView implements MuPDFView {

    private static final String TAG = "MuPDFPageView111";

    //    private final MuPDFCore mCore;
    private AsyncTask<Void, Void, PassClickResult> mPassClick;
    private PDFAnnotation mAnnotations[];
    private int mSelectedAnnotationIndex = -1;
    private AsyncTask<Void, Void, RectF[]> mLoadWidgetAreas;
    private AsyncTask<Void, Void, PDFAnnotation[]> mLoadAnnotations;

    private AsyncTask<String, Void, Boolean> mSetWidgetText;
    private AsyncTask<String, Void, Void> mSetWidgetChoice;
    private AsyncTask<PointF[], Void, Void> mAddStrikeOut;
    private AsyncTask<Integer, Void, Void> mDeleteAnnotation;
    private AsyncTask<Integer, Void, Void> mDeleteAnnotationAuto;

    public MuPDFPageView(Context c, FilePicker.FilePickerSupport filePickerSupport, MuPDFCore core, Point parentSize, MuPDFPageAdapter adapter) {
        super(c, parentSize, adapter);
        mCore = core;
    }


    public LinkInfo hitLink(float x, float y) {
        // Since link highlighting was implemented, the super class
        // PageView has had sufficient information to be able to
        // perform this method directly. Making that change would
        // make MuPDFCore.hitLinkPage superfluous.
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        for (LinkInfo l : mLinks)
            if (l.rect.contains(docRelX, docRelY))
                return l;

        return null;
    }


    public void setChangeReporter(Runnable reporter) {
    }

    //    private static final String TAG = "MuPDFPageView2";
    private int index = 0;

    public void dissMissView() {
    }


    public void showPopupMenuDelete(MotionEvent event) {
        Log.d(TAG, "showPopupMenuDelete: ");
        ArrayList<DefaultEditMenu> list = new ArrayList<>();
        Integer indexHighlight = hashMapAnnotations.get(PDFAnnotation.TYPE_HIGHLIGHT);
        Integer indexUnderline = hashMapAnnotations.get(PDFAnnotation.TYPE_UNDERLINE);
        Integer indexStrike = hashMapAnnotations.get(PDFAnnotation.TYPE_STRIKE_OUT);

        if (indexHighlight != null) {
            list.add(new DefaultEditMenu(getContext().getString(R.string.unhighlight), "menu_hl", Annotation.Type.DEFAULT, indexHighlight));
        }
        if (indexUnderline != null) {
            list.add(new DefaultEditMenu(getContext().getString(R.string.stop_underlining), "menu_under", Annotation.Type.DEFAULT, indexUnderline));
        }
        if (indexStrike != null) {
            list.add(new DefaultEditMenu(getContext().getString(R.string.stop_strikethrough), "menu_strike", Annotation.Type.DEFAULT, indexStrike));
        }

        MyPopupMenu myPopupMenu = MyPopupMenu.getInstance(getContext());
        myPopupMenu.setListItem(list);
        myPopupMenu.setClickListener(new MyPopupMenu.ClickListener() {
            @Override
            public void clickItem(DefaultEditMenu defaultEditMenu) {
                mSelectedAnnotationIndex = defaultEditMenu.getIndex();
                deleteSelectedAnnotation();
            }
        });
        if (mSelectedAnnotationIndex >= 0 && mAnnotations != null) {
            myPopupMenu.showMenu((int) event.getX() + 50, (int) event.getY());
        }
        if (mAnnotations != null && mSelectedAnnotationIndex >= 0 && mSelectedAnnotationIndex < mAnnotations.length) {
            setItemSelectBox(null);
            setItemSelectBox(mAnnotations[mSelectedAnnotationIndex].getRect().toRectF());
        }
    }

    public void updatePosition(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        MyPopupMenu.getInstance(getContext()).setPoint(mOriginalFirstPoint.x * scale + getLeft(),
                mOriginalFirstPoint.y * scale + getTop(), mOriginalSecondPoint.x * scale + this.getLeft(), mOriginalSecondPoint.y * scale + this.getTop());


//        if (mSelectedAnnotationIndex >= 0 && mAnnotations != null) {
//            }
//
//        if (mAnnotations != null && mSelectedAnnotationIndex >= 0 && mSelectedAnnotationIndex < mAnnotations.length) {
//            setItemSelectBox(null);
//            setItemSelectBox(mAnnotations[mSelectedAnnotationIndex].getRect().toRectF());
//        }
    }

    @Override
    public boolean checkClickMenu(MotionEvent event) {
//        Log.d(TAG, "checkClickMenu: " + (mSelectBox != null) + "  " + (mSelectedAnnotationIndex >= 0) + "  " + mOriginalFirstPoint.x);
        return (mSelectedAnnotationIndex >= 0) || (mOriginalFirstPoint != null && (mOriginalFirstPoint.x != 0 || mOriginalFirstPoint.y != 0));
    }


    public ReaderView.ModeAuto getAutoMode() {
        return ((ReaderView) getParent()).mModeAuto;
    }


    private int i;
    private boolean hit = false;

    public Hit passClickEvent(float x, float y) {
        return Hit.Nothing;
    }

    @Override
    public void passClickEvent(float x, float y, final SingleClickCallback singleClickCallback) {
        mSelectBox = null;
        listOldRect.clear();
        listQuad.clear();
        listRect.clear();
        if (mSearchView != null) {
            mSearchView.invalidate();
        }

        if (((ReaderView) getParent()).mModeAuto == AUTO_HIGHLIGHT &&
                ((ReaderView) getParent()).mModeAuto == AUTO_STRIKE_THROUGH && ((ReaderView) getParent()).mModeAuto == AUTO_UNDERLINE) {
            setItemSelectBox(null);
            if (singleClickCallback != null) singleClickCallback.onClickItem(Hit.Nothing);
            return;
        }
        hashMapAnnotations.clear();
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        final float docRelX = (x - getLeft()) / scale;
        final float docRelY = (y - getTop()) / scale;

        hit = false;
        index = -1;
        if (mAnnotations != null) {
            actionHasAnnotation(docRelX, docRelY, singleClickCallback);
        } else {
            PhotorThread.getInstance().runBackground(new PhotorThread.IBackground() {
                @Override
                public void doingBackground() {
                    try {
                        mAnnotations = mCore.getPDFAnnotation(mPageNumber);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCompleted() {
                    if (mAnnotations == null) {
                        mAnnotations = new PDFAnnotation[0];
                    }
                    actionHasAnnotation(docRelX, docRelY, singleClickCallback);
                }

                @Override
                public void onCancel() {

                }
            });
        }
    }

    private void actionHasAnnotation(float docRelX, float docRelY, SingleClickCallback singleClickCallback) {
        if (mAnnotations == null || mAnnotations.length == 0) {
            if (singleClickCallback != null) {
                singleClickCallback.onClickItem(Hit.Nothing);
            }
            return;
        }
        for (i = 0; i < mAnnotations.length; i++) {
            try {
                if (mAnnotations[i].getBounds().contains(docRelX, docRelY)) {
                    hit = true;
                    hashMapAnnotations.put(mAnnotations[i].getType(), i);
                    index = i;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if (hit && index < mAnnotations.length) {
            switch (mAnnotations[index].getType()) {
                case PDFAnnotation.TYPE_HIGHLIGHT:
                case PDFAnnotation.TYPE_UNDERLINE:
                case PDFAnnotation.TYPE_SQUARE:
                case PDFAnnotation.TYPE_STRIKE_OUT:
                case PDFAnnotation.TYPE_INK:
                    mSelectedAnnotationIndex = index;
                    if (((ReaderView) getParent()).mModeAuto == AUTO_DELETE) {
                        deleteSelectedAnnotation();
                    } else {
//                        setItemSelectBox(mAnnotations[index].getRect().toRectF());
                    }
                    if (singleClickCallback != null) {
                        singleClickCallback.onClickItem(Hit.Annotation);
                    }
            }
        } else {
//            setItemSelectBox(null);
            if (singleClickCallback != null) {
                singleClickCallback.onClickItem(Hit.Nothing);
            }
        }
    }


    public boolean copySelection() {
        final ArrayList<StructuredText.TextChar> listText = new ArrayList<>();
        processSelectedText(new TextProcessor() {
            StringBuilder line;

            @Override
            public void start() {

            }

            public void onStartLine() {
                line = new StringBuilder();
            }

            @Override
            public void onWord(StructuredText.TextChar word) {
                listText.add(word);
            }

            public void onWord(TextWord word) {
//                if (line.length() > 0)
//                    line.append(' ');
//                line.append(word.w);
            }

            public void onEndLine() {
//                if (text.length() > 0)
//                    text.append('\n');
//                text.append(line);
            }

            @Override
            public void onFirstPosition(StructuredText.TextChar textChar) {

            }

            @Override
            public void onSecondPosition(StructuredText.TextChar textChar) {

            }
        });


        StringBuilder stringBuilder = new StringBuilder();
        for (StructuredText.TextChar textChar1 : listText) {
            stringBuilder.append((char) textChar1.c);
        }


        Toast.makeText(getContext(), "Copy text to clipboard", Toast.LENGTH_SHORT).show();
        android.content.ClipboardManager cm = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("MuPDF", stringBuilder));

        deselectText();

        return true;
    }

    public boolean markupSelection(final Annotation.Type type) {


//        final ArrayList<Quad> listQuad = new ArrayList<>();
//        PhotorThread.getInstance().runBackground(new PhotorThread.IBackground() {
//            @Override
//            public void doingBackground() {
//                processSelectedText(new TextProcessor() {
//                    Quad quad;
//
//                    @Override
//                    public void start() {
//
//                    }
//
//                    public void onStartLine() {
//
//                    }
//
//                    @Override
//                    public void onWord(StructuredText.TextChar word) {
//                        if (quad == null) quad = new Quad(word.quad);
//                        quad.union(word.quad);
//                    }
//
//
//                    public void onEndLine() {
//                        Log.d("dsk2", "onEndLine: ");
//                        if (quad != null) {
//                            listQuad.add(quad);
//                            quad = null;
//                        }
//                    }
//
//                    @Override
//                    public void onFirstPosition(StructuredText.TextChar textChar) {
//
//                    }
//
//                    @Override
//                    public void onSecondPosition(StructuredText.TextChar textChar) {
//
//                    }
//                });
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//
//            @Override
//            public void onCancel() {
//
//            }
//        });

//        Log.d("tri1", "listQuad: " + listQuad.size() + " - "+mPageNumber);
        if (listQuad == null || listQuad.isEmpty()) return false;
        switch (type) {
            case HIGHLIGHT:
            case UNDERLINE:
            case SQUIGGLY:
            case STRIKEOUT:
            case INK:
                if (callbackEdit != null) {
                    callbackEdit.edited();
                }
        }

        PhotorThread.getInstance().runBackground(new PhotorThread.IBackground() {
            @Override
            public void doingBackground() {
                try {
                    mCore.addAnnotationNew(mPageNumber, listQuad, type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCompleted() {
                deselectText();
                update();
                loadAnnotationsNew();

            }

            @Override
            public void onCancel() {

            }
        });

        return true;
    }

    private final HashMap<Integer, Boolean> hastMapAutoDelete = new HashMap<>();

    public void autoDelete(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        final float docRelX = (x - getLeft()) / scale;
        final float docRelY = (y - getTop()) / scale;
        int i;

        if (mAnnotations != null) {
            actionDeleteAnotation(docRelX, docRelY);
        } else {
            PhotorThread.getInstance().runBackground(new PhotorThread.IBackground() {
                @Override
                public void doingBackground() {
                    mAnnotations = mCore.getPDFAnnotation(mPageNumber);
                }

                @Override
                public void onCompleted() {
                    if (mAnnotations != null) {
                        actionDeleteAnotation(docRelX, docRelY);
                    }
                }

                @Override
                public void onCancel() {

                }
            });
        }
    }

    private boolean isDeleteAnotation = false;

    private synchronized void actionDeleteAnotation(float docRelX, float docRelY) {
        if (isDeleteAnotation) return;
        for (i = 0; i < mAnnotations.length; i++) {
            if (mAnnotations[i].getBounds().contains(docRelX, docRelY)) {
                mSelectedAnnotationIndex = i;
                if (isDeleteAnotation) return;
                isDeleteAnotation = true;
//                Boolean status = hastMapAutoDelete.get(i);
//                if (status == null || !status) {
//                    hastMapAutoDelete.put(i, true);


//                if (mDeleteAnnotationAuto != null)
//                    mDeleteAnnotationAuto.cancel(true);

                if (mSelectedAnnotationIndex != -1) {
                    if (callbackEdit != null) callbackEdit.edited();
                    if (mDeleteAnnotation != null)
                        mDeleteAnnotation.cancel(true);
                    if (mAnnotations != null) {
                        sizeAnotation = mAnnotations.length;
                    }
                    mDeleteAnnotation = new AsyncTask<Integer, Void, Void>() {
                        @Override
                        protected Void doInBackground(Integer... params) {
                            mCore.deletePDFAnnotations(mPageNumber, mAnnotations[params[0]]);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            update();

                            mLoadAnnotations = new AsyncTask<Void, Void, PDFAnnotation[]>() {
                                @Override
                                protected PDFAnnotation[] doInBackground(Void... params) {
                                    return mCore.getPDFAnnotation(mPageNumber);
                                }

                                @Override
                                protected void onPostExecute(PDFAnnotation[] result) {
                                    mAnnotations = result;
                                    isDeleteAnotation = false;

//                                    if (mAnnotations == null) return;
//                                    if (sizeAnotation == mAnnotations.length) {
//                                        mCore.deletePDFAnnotations(mPageNumber, mAnnotations[mSelectedAnnotationIndex]);
//                                        update();
//                                        loadAnnotations(mPageNumber);
//                                    }
                                }
                            };
                            mLoadAnnotations.execute();
                        }
                    };

                    mDeleteAnnotation.execute(mSelectedAnnotationIndex);
                    return;
//            mSelectedAnnotationIndex = -1;
                }


                mDeleteAnnotationAuto = new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        mCore.deletePDFAnnotations(mPageNumber, mAnnotations[params[0]]);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
//                        update();
                        updateEntireCanvas(true);

//                        mAnnotations = null;
                        mLoadAnnotations = new AsyncTask<Void, Void, PDFAnnotation[]>() {
                            @Override
                            protected PDFAnnotation[] doInBackground(Void... params) {
                                return mCore.getPDFAnnotation(mPageNumber);
                            }

                            @Override
                            protected void onPostExecute(PDFAnnotation[] result) {
                                mAnnotations = result;
                                isDeleteAnotation = false;
                            }
                        };
                        mLoadAnnotations.execute();

                    }
                };

//                mDeleteAnnotationAuto.execute(i);
                return;
            }
        }

    }


    int sizeAnotation = 0;

    public void deleteSelectedAnnotation() {
        if (mSelectedAnnotationIndex != -1) {
            if (callbackEdit != null) callbackEdit.edited();
            if (mDeleteAnnotation != null)
                mDeleteAnnotation.cancel(true);
            if (mAnnotations != null) {
                sizeAnotation = mAnnotations.length;
            }
            mDeleteAnnotation = new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... params) {
                    mCore.deletePDFAnnotations(mPageNumber, mAnnotations[params[0]]);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    update();
                    mLoadAnnotations = new AsyncTask<Void, Void, PDFAnnotation[]>() {
                        @Override
                        protected PDFAnnotation[] doInBackground(Void... params) {
                            return mCore.getPDFAnnotation(mPageNumber);
                        }

                        @Override
                        protected void onPostExecute(PDFAnnotation[] result) {
                            mAnnotations = result;
                            if (mAnnotations == null) return;
                            if (sizeAnotation == mAnnotations.length) {
                                mCore.deletePDFAnnotations(mPageNumber, mAnnotations[mSelectedAnnotationIndex]);
                                update();
                                loadAnnotations(mPageNumber);
                            }
                        }
                    };
                    mLoadAnnotations.execute();
                }
            };

            mDeleteAnnotation.execute(mSelectedAnnotationIndex);
//            mSelectedAnnotationIndex = -1;
            setItemSelectBox(null);
        }
    }


    public void deselectAnnotation() {
        mSelectedAnnotationIndex = -1;
        setItemSelectBox(null);
    }

    public boolean saveDraw() {
        return true;
    }


    @Override
    protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                    final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {
            @Override
            public Void doInBackground(Cookie cookie, Void... params) {
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    bm.eraseColor(0);
                try {
                    if (bm != null && !bm.isRecycled()) {
//                        Log.d("dsk3", "getDrawPageTask: ");
                        mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

    }

    protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                      final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

            @Override
            public Void doInBackground(Cookie cookie, Void... params) {
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
                if (bm != null && !bm.isRecycled()) {
                    mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                }
                return null;
            }
        };
    }

    @Override
    protected LinkInfo[] getLinkInfo() {
        return mCore.getPageLinks(mPageNumber);
    }

    @Override
    protected TextWord[][] getText() {
        return mCore.textLines(mPageNumber);
    }

    @Override
    protected void addMarkup(PointF[] quadPoints, Annotation.Type type) {
        mSelectBox = null;
    }


    private void loadAnnotationsNew() {
        mAnnotations = null;
        PhotorThread.getInstance().runBackground(new PhotorThread.IBackground() {
            @Override
            public void doingBackground() {
                mAnnotations = mCore.getPDFAnnotation(mPageNumber);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onCancel() {

            }
        });
    }


    private void loadAnnotations(final int page) {
        mAnnotations = null;
        mLoadAnnotations = new AsyncTask<Void, Void, PDFAnnotation[]>() {
            @Override
            protected PDFAnnotation[] doInBackground(Void... params) {
                return mCore.getPDFAnnotation(page);
            }

            @Override
            protected void onPostExecute(PDFAnnotation[] result) {
                mAnnotations = result;
            }
        };

        mLoadAnnotations.execute();
    }

    @Override
    public void setPage(final int page, PointF size) {
        super.setPage(page, size);
    }

    public void setScale(float scale) {
        // This type of view scales automatically to fit the size
        // determined by the parent view groups during layout
    }

    @Override
    public void releaseResources() {
        if (mPassClick != null) {
            mPassClick.cancel(true);
            mPassClick = null;
        }

        if (mLoadWidgetAreas != null) {
            mLoadWidgetAreas.cancel(true);
            mLoadWidgetAreas = null;
        }

        if (mLoadAnnotations != null) {
            mLoadAnnotations.cancel(true);
            mLoadAnnotations = null;
        }

        if (mSetWidgetText != null) {
            mSetWidgetText.cancel(true);
            mSetWidgetText = null;
        }

        if (mSetWidgetChoice != null) {
            mSetWidgetChoice.cancel(true);
            mSetWidgetChoice = null;
        }

        if (mAddStrikeOut != null) {
            mAddStrikeOut.cancel(true);
            mAddStrikeOut = null;
        }

        if (mDeleteAnnotation != null) {
            mDeleteAnnotation.cancel(true);
            mDeleteAnnotation = null;
        }

        super.releaseResources();
    }

}
