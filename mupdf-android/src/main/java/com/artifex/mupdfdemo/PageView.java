package com.artifex.mupdfdemo;

import static com.artifex.mupdfdemo.ReaderView.ModeAuto.AUTO_DELETE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;

import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.utils.DigitalizedEventCallback;
import com.artifex.utils.MuPdfConstant;
import com.artifex.utils.PdfBitmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends androidx.appcompat.widget.AppCompatImageView {

    public OpaqueImageView(Context context) {
        super(context);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}

interface TextProcessor {
    void start();

    void onStartLine();

    void onWord(StructuredText.TextChar word);

    void onEndLine();

    void onFirstPosition(StructuredText.TextChar textChar);

    void onSecondPosition(StructuredText.TextChar textChar);
}

class TextSelector {
    final private StructuredText.TextBlock[] mText;
    final private RectF mSelectBox;
    private boolean isLongClick = false;
    private boolean isFirstPosition;
    private boolean selectBox = false;

    enum SelectText {
        NONE, FIRST, SECOND, CENTER
    }


    public TextSelector(StructuredText.TextBlock[] text, RectF selectBox, boolean isLongClick, boolean isFirstPosition) {
        this.isLongClick = isLongClick;
        mText = text;
        mSelectBox = selectBox;
    }

    public void select(TextProcessor tp) {
        if (mText == null || mSelectBox == null)
            return;
        if (isLongClick) {
            longCheck(tp);
            return;
        }
        boolean checkFirstBox = false;
        boolean checkSecondBox = false;

        float fX0 = Float.MAX_VALUE, fX1 = Float.MIN_VALUE, fY0 = Float.MAX_VALUE, fY1 = Float.MIN_VALUE;
        float sX0 = Float.MAX_VALUE, sX1 = Float.MIN_VALUE, sY0 = Float.MAX_VALUE, sY1 = Float.MIN_VALUE;

        for (StructuredText.TextBlock textBlock : mText) {
            if (textBlock.bbox.contains(mSelectBox.left, mSelectBox.top)) {
                checkFirstBox = true;
            }
            if (textBlock.bbox.contains(mSelectBox.right, mSelectBox.bottom)) {
                checkSecondBox = true;
            }
            if (textBlock.bbox.checkX(mSelectBox.left)) {
                if (textBlock.bbox.y0 > mSelectBox.top) {
                    fY0 = Math.min(fY0, textBlock.bbox.y0);
                }
                if (textBlock.bbox.y1 < mSelectBox.top) {
                    fY1 = Math.max(fY1, textBlock.bbox.y1);
                }
            }
            if (textBlock.bbox.checkY(mSelectBox.top)) {
                if (textBlock.bbox.x0 > mSelectBox.left) {
                    fX0 = Math.min(fX0, textBlock.bbox.x0);
                }
                if (textBlock.bbox.x1 < mSelectBox.left) {
                    fX1 = Math.max(fX1, textBlock.bbox.x1);
                }
            }

            if (textBlock.bbox.checkX(mSelectBox.right)) {
                if (textBlock.bbox.y0 > mSelectBox.bottom) {
                    sY0 = Math.min(sY0, textBlock.bbox.y0);
                }
                if (textBlock.bbox.y1 < mSelectBox.bottom) {
                    sY1 = Math.max(sY1, textBlock.bbox.y1);
                }
            }
            if (textBlock.bbox.checkY(mSelectBox.bottom)) {
                if (textBlock.bbox.x0 > mSelectBox.right) {
                    sX0 = Math.min(sX0, textBlock.bbox.x0);
                }
                if (textBlock.bbox.x1 < mSelectBox.right) {
                    sX1 = Math.max(sX1, textBlock.bbox.x1);
                }
            }

        }
        if (!checkFirstBox) {
            checkFirst(fX0, fX1, fY0, fY1);
        }
        if (!checkSecondBox) {
            checkSecond(sX0, sX1, sY0, sY1);
        }
        ArrayList<StructuredText.TextLine> lines = new ArrayList<StructuredText.TextLine>();
        if (!isFirstPosition) {
            for (StructuredText.TextBlock textBlock : mText) {
                if (textBlock.bbox.contains(mSelectBox.left, mSelectBox.top) && textBlock.bbox.contains(mSelectBox.right, mSelectBox.bottom)) {
                    for (StructuredText.TextLine textLine : textBlock.lines) {
                        if (textLine.bbox.y1 >= mSelectBox.top && textLine.bbox.y0 <= mSelectBox.bottom) {
                            lines.add(textLine);
                        }
                    }
                } else if (textBlock.bbox.contains(mSelectBox.left, mSelectBox.top)) {
                    selectBox = true;
                    for (StructuredText.TextLine textLine : textBlock.lines) {
                        if (textLine.bbox.y1 >= mSelectBox.top) {
                            lines.add(textLine);
                        }
                    }
                } else if (textBlock.bbox.contains(mSelectBox.right, mSelectBox.bottom)) {
                    selectBox = false;
                    for (StructuredText.TextLine textLine : textBlock.lines) {
                        if (textLine.bbox.y0 <= mSelectBox.bottom) {
                            lines.add(textLine);
                        }
                    }
                } else if (selectBox) {
                    lines.addAll(Arrays.asList(textBlock.lines));
                }
            }

        }
        StructuredText.TextChar firstChar = null;
        StructuredText.TextChar secondChar = null;

        // fix loi boi den
        try {
            Collections.sort(lines, new Comparator<StructuredText.TextLine>() {
                @Override
                public int compare(StructuredText.TextLine o1, StructuredText.TextLine o2) {
                    if (o1.bbox.centerY() < o2.bbox.centerY()) {
                        return -1;
                    } else if (o1.bbox.centerY() > o2.bbox.centerY()) {
                        return 1;
                    }
                    return 0;
                }
            });

            Collections.sort(lines, new Comparator<StructuredText.TextLine>() {
                @Override
                public int compare(StructuredText.TextLine o1, StructuredText.TextLine o2) {
                    if (o1.bbox.x1 < o2.bbox.x0) {
                        return -1;
                    } else if (o1.bbox.x0 > o2.bbox.x1) {
                        return 1;
                    }
                    return 0;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < lines.size(); i++) {
            tp.onStartLine();

            for (StructuredText.TextChar word : lines.get(i).chars) {
                if (i == 0) {
                    if (lines.size() == 1){
                        if (word.quad.toRect().x1 > mSelectBox.left && word.quad.toRect().x0 < mSelectBox.right) {
//                        if (word.quad.toRect().x1 > mSelectBox.left) {
                            if (firstChar == null) firstChar = word;
                            secondChar = word;
                            tp.onWord(word);
                        }
                    }else {
                        if (word.quad.toRect().x1 > mSelectBox.left) {
                            if (firstChar == null) firstChar = word;
                            secondChar = word;
                            tp.onWord(word);
                        }
                    }
                } else if (i == lines.size() - 1) {
                    if (word.quad.toRect().x0 < mSelectBox.right) {
                        if (firstChar == null) firstChar = word;
                        secondChar = word;
                        tp.onWord(word);
                    }
                } else if (word.c != '\u00A0' && word.c != '\u2007' && word.c != '\u202F') {
                    if (firstChar == null) firstChar = word;
                    secondChar = word;
                    tp.onWord(word);
                }
                if (firstChar != null) tp.onFirstPosition(firstChar);
                if (secondChar != null) tp.onSecondPosition(secondChar);
            }
            tp.onEndLine();
        }

    }


    private void checkFirst(float x0, float x1, float y0, float y1) {
        float fx = mSelectBox.left;
        float fy = mSelectBox.top;
        float min = Float.MAX_VALUE;
        if (Math.abs(fx - x0) < min) {
            min = Math.abs(fx - x0);
            mSelectBox.top = fy;
            mSelectBox.left = x0;
        }
        if (Math.abs(fx - x1) < min) {
            min = Math.abs(fx - x1);
            mSelectBox.top = fy;
            mSelectBox.left = x1;
        }
        if (Math.abs(fy - y0) < min) {
            min = Math.abs(fy - y0);
            mSelectBox.top = y0;
            mSelectBox.left = fx;
        }
        if (Math.abs(fy - y1) < min) {
            min = Math.abs(fy - y1);
            mSelectBox.top = y1;
            mSelectBox.left = fx;
        }
    }

    private static final String TAG = "TextSelector";

    private void checkSecond(float x0, float x1, float y0, float y1) {
        float fx = mSelectBox.right;
        float fy = mSelectBox.bottom;
        float min = Float.MAX_VALUE;
        if (Math.abs(fx - x0) < min) {
            min = Math.abs(fx - x0);
            mSelectBox.bottom = fy;
            mSelectBox.right = x0;
        }
        if (Math.abs(fx - x1) < min) {
            min = Math.abs(fx - x1);
            mSelectBox.bottom = fy;
            mSelectBox.right = x1;
            Log.d(TAG, "checkSecond:X1 " + x1);
        }
        if (Math.abs(fy - y0) < min) {
            min = Math.abs(fy - y0);
            mSelectBox.bottom = y0;
            mSelectBox.right = fx;
            Log.d(TAG, "checkSecond:Y0 " + y0);
        }
        if (Math.abs(fy - y1) < min) {
            min = Math.abs(fy - y1);
            mSelectBox.bottom = y1;
            mSelectBox.right = fx;
            Log.d(TAG, "checkSecond:Y1 " + y1);
        }
    }


    // kiem tra vung text de chon
    public class TextWord {
        public ArrayList<StructuredText.TextChar> chars = new ArrayList<StructuredText.TextChar>();
        public com.artifex.mupdf.fitz.Rect bbox;

        public TextWord(StructuredText.TextChar textChar) {
            if (textChar != null) {
                chars.add(textChar);
                bbox = new com.artifex.mupdf.fitz.Rect(textChar.quad);
            }
        }

        public TextWord() {
        }

        public void addTextChar(StructuredText.TextChar textChar) {
            if (textChar != null) {
                chars.add(textChar);
                if (bbox == null) {
                    bbox = new com.artifex.mupdf.fitz.Rect(textChar.quad);
                } else {
                    bbox.contains(textChar.quad.toRect());
                }
            }
        }
    }

    public void longCheck(TextProcessor tp) {
        TextWord textWord = null;
        ArrayList<TextWord> listTextWords = new ArrayList<>();
        for (StructuredText.TextBlock textBlock : mText) {
            textBlock.getString();
            if (textBlock.bbox.contains(mSelectBox.left, mSelectBox.top)) {
                for (StructuredText.TextLine textLine : textBlock.lines) {
                    for (StructuredText.TextChar textChar : textLine.chars) {
                        if (Character.isWhitespace(textChar.c)) {
                            if (textWord != null && textWord.chars.size() > 1) {
                                listTextWords.add(textWord);
                            }
                            textWord = null;
                        } else {
                            if (textWord == null) textWord = new TextWord(textChar);
                            else textWord.addTextChar(textChar);
                        }
                    }
                }
            }
        }
        Collections.sort(listTextWords, new Comparator<TextWord>() {
            @Override
            public int compare(TextWord o1, TextWord o2) {
                return o1.bbox.getRange(mSelectBox.centerX(), mSelectBox.centerY()) - o2.bbox.getRange(mSelectBox.centerX(), mSelectBox.centerY());
            }
        });
        tp.start();
        if (listTextWords.size() > 0) {
            ArrayList<StructuredText.TextChar> list = listTextWords.get(0).chars;
            tp.onStartLine();
            for (int i = 0; i < list.size(); i++) {
                if (i == 0) tp.onFirstPosition(list.get(i));
                if (i == list.size() - 1) tp.onSecondPosition(list.get(i));
                tp.onWord(list.get(i));
            }
            tp.onEndLine();
        }
    }


    public boolean checkSelectBox(TextProcessor tp) {
        StructuredText.TextLine line = null;
        boolean check = false;
        for (StructuredText.TextBlock textBlock : mText) {
            textBlock.getString();
//            Log.d("textBox", textBlock.getString());
            if (textBlock.bbox.contains(mSelectBox.left, mSelectBox.top)) {
                for (StructuredText.TextLine textLine : textBlock.lines) {
                    if (textLine.bbox.y0 < mSelectBox.centerY() && textLine.bbox.y1 > mSelectBox.centerY() && textLine.chars != null && textLine.chars.length > 0) {
                        line = textLine;
                        check = true;
                        break;
                    }
                }
            }
            if (check) break;
        }
        if (line != null) {
            ArrayList<StructuredText.TextChar> listTextChar = new ArrayList<>();
            float start = line.bbox.x0;
            float end = 0;
            for (StructuredText.TextChar textChar : line.chars) {
                end = textChar.quad.toRect().x1;
                if (Character.isWhitespace(textChar.c)) {
                    if (start <= mSelectBox.centerX() && end >= mSelectBox.centerY()) {
                        break;
                    } else {
                        listTextChar.clear();
                    }
                } else {
                    listTextChar.add(textChar);
                }
            }
            if (listTextChar.size() > 0) {
                StructuredText.TextChar firstChar = null;
                StructuredText.TextChar secondChar = null;
                tp.onStartLine();
                for (StructuredText.TextChar textChar : listTextChar) {
                    if (firstChar == null) firstChar = textChar;
                    secondChar = textChar;
                    tp.onWord(textChar);
                }
                tp.onEndLine();
                if (firstChar != null) tp.onFirstPosition(firstChar);
                if (secondChar != null) tp.onSecondPosition(secondChar);
                return true;
            }

        }
        return false;
    }

    public void longClickSelect(TextProcessor tp) {
        Log.d("dsk", "longClickSelect: ");
        tp.start();
        if (mText == null || mSelectBox == null) return;
        boolean check;
        check = checkSelectBox(tp);
        if (!check) {
            mSelectBox.left = mSelectBox.left - 10;
            check = checkSelectBox(tp);
            if (check) return;
        }
        if (!check) {
            mSelectBox.left = mSelectBox.left + 10;
            mSelectBox.right = mSelectBox.right + 10;
            check = checkSelectBox(tp);
            if (check) return;

        }
        if (!check) {
            mSelectBox.right = mSelectBox.right - 10;
            mSelectBox.top = mSelectBox.top - 10;
            mSelectBox.bottom = mSelectBox.bottom - 10;
            check = checkSelectBox(tp);
            if (check) return;

        }
        if (!check) {
            mSelectBox.top = mSelectBox.top + 20;
            mSelectBox.bottom = mSelectBox.bottom + 20;
            check = checkSelectBox(tp);
            if (check) return;

        }
    }
}

public abstract class   PageView extends ViewGroup {
    private static final int HIGHLIGHT_COLOR = 0x33179CD7;

    private static final int FIND_COLOR = 0x4DEB4747;

    private static final int LINK_COLOR = 0x4DF2C94C;
    private static final int BOX_COLOR = Color.RED; //0xFF4444FF;
    private static final int INK_COLOR = 0x0EC45B;
    private static final float INK_THICKNESS = 10.0f;
    private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_DIALOG_DELAY = 200;
    private static final String TAG = "PageView111";

    private static final int SIGN_HEIGHT = 50;
    private static final int SIGN_WIDTH = 100;

    protected final Context mContext;
    protected int mPageNumber;
    private Point mParentSize; // Size of the view containing the pdf viewer. It could be the same as the screen if this view is full screen.
    protected Point mSize;   // Size of page at minimum zoom
    protected float mSourceScale;

    private ImageView mEntire; // Image rendered at minimum zoom
    private Bitmap mEntireBm; // Bitmap used to draw the entire page at minimum zoom.
    private Matrix mEntireMat;
    private AsyncTask<Void, Void, StructuredText.TextBlock[]> mGetText;
    private AsyncTask<Void, Void, LinkInfo[]> mGetLinkInfo;
    private CancellableAsyncTask<Void, Void> mDrawEntire;

    private Point mPatchViewSize; // View size on the basis of which the patch was created. After zoom.
    private Rect mPatchArea; // Area of the screen zoomed.
    private ImageView mPatch; // Image rendered at zoom resolution.
    private Bitmap mPatchBm; // Bitmap used to draw the zoomed image.
    private CancellableAsyncTask<Void, Void> mDrawPatch;
    private RectF mSearchBoxes[];
    protected LinkInfo mLinks[];
    protected RectF mSelectBox;
    private StructuredText.TextBlock mText[];
    public RectF mItemSelectBox;
    protected ArrayList<ArrayList<PointF>> mDrawing;
    View mSearchView;
    private boolean mIsBlank;
    private boolean mHighlightLinks;
    ArrayList<RectF> listRect = new ArrayList<>();
    ArrayList<RectF> listOldRect = new ArrayList<>();
    ArrayList<RectF> listRectTest = new ArrayList<>();

    PointF mOriginalFirstPoint = new PointF();
    PointF mFirstPoint = new PointF();
    PointF mFirstPointBelow = new PointF();
    PointF mOriginalSecondPoint = new PointF();
    PointF mSecondPoint = new PointF();
    PointF mSecondPointBelow = new PointF();
    RectF currentSearchRect;
    boolean isFirstPosition = false;
    protected HashMap<Integer, Integer> hashMapAnnotations = new HashMap<>();


    private ProgressBar mBusyIndicator;
    private View mBusyIndicatorNew;
    private final Handler mHandler = new Handler();

    private static boolean flagPositions = true; // Concurrency flag to avoid entering twice onDoubleTap method.
    private Bitmap signBitmap; // Bitmap for signature at higher resolution. // *BACKWARD COMPATIBILITY*
    private Point signBitmapSize; // Bitmap size, scaled to screen size and pdf.
    private static DigitalizedEventCallback eventCallback; // Callback for the app. The library fires an event when the user touched longPress or doubleTap, and the app can manage the behaviour.

    private Paint mBitmapPaint;
    private MuPDFPageAdapter mAdapter;
    protected CallbackEdit callbackEdit;
    public PointF pdfSize;
    private PdfBitmap picturePdfBitmap; // *BACKWARD COMPATIBILITY*

    protected MuPDFCore mCore;
    private boolean isLongClick = false;
    private boolean longCickTouchUp = true;


    public ArrayList<RectF> getListOldRect() {
        return listOldRect;
    }

    public void setCurrentSearchRect(RectF currentSearchRect) {
        this.currentSearchRect = currentSearchRect;
//        invalidate();
        if (mSearchView != null) {
            mSearchView.invalidate();
        }
    }

    public PageView(Context c, Point parentSize, MuPDFPageAdapter adapter) {
        super(c);
        mContext = c;
        flagPositions = true;
        mParentSize = parentSize;
        setBackgroundColor(BACKGROUND_COLOR);
        mEntireMat = new Matrix();
        mAdapter = adapter;
        init();
    }

    private void init() {
        paint = new Paint();
        paintMinh = new Paint();
        paintMinh.setStrokeWidth(3f);
        paintMinh.setColor(Color.RED);
    }

    public RectF getSelectBox() {
        return mSelectBox;
    }


    public void setCallbackEdit(CallbackEdit callbackEdit) {
        this.callbackEdit = callbackEdit;
    }

    protected abstract CancellableTaskDefinition<Void, Void> getDrawPageTask(Bitmap bm, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);

    protected abstract CancellableTaskDefinition<Void, Void> getUpdatePageTask(Bitmap bm, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);

    protected abstract LinkInfo[] getLinkInfo();

    protected abstract TextWord[][] getText();

    protected abstract void addMarkup(PointF[] quadPoints, Annotation.Type type);

    private void reinit() {
        resetValuePointSelect();
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        if (mGetLinkInfo != null) {
            mGetLinkInfo.cancel(true);
            mGetLinkInfo = null;
        }

        if (mGetText != null) {
            mGetText.cancel(true);
            mGetText = null;
        }

        mIsBlank = true;
        mPageNumber = 0;

        if (mSize == null)
            mSize = mParentSize;

        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }

        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }

        mPatchViewSize = null;
        mPatchArea = null;

        mSearchBoxes = null;
        mLinks = null;
        mSelectBox = null;
        mText = null;
        mItemSelectBox = null;

    }

    public void releaseResources() {
        releaseBitmaps();

        reinit();

//        if (mBusyIndicator != null) {
//            removeView(mBusyIndicator);
//            mBusyIndicator = null;
//        }

        if (mBusyIndicatorNew != null) {
            isLoadSuccess = false;
            removeView(mBusyIndicatorNew);
            mBusyIndicatorNew = null;
        }
    }

    public void releaseBitmaps() {
        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }

        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }

        Log.i(TAG, "Recycle mEntire on releaseBitmaps: " + mEntireBm);

        recycleBitmap(mEntireBm);
        mEntireBm = null;
        recycleBitmap(mPatchBm);
        mPatchBm = null;


        Log.i(TAG, "Recycle mPathBm on releaseBitmaps: " + mPatchBm);

        paintGlass.setShader(null);
        paintGlass2.setShader(null);
        bitmapShader = null;
        bitmapShader2 = null;
        canvasGlass2 = null;
        recycleBitmap(bitmapShadow);
        bitmapShadow = null;
        recycleBitmap(bmGlass2);
        bmGlass2 = null;


    }

    public void blank(int page) {
        reinit();
        mPageNumber = page;

//        if (mBusyIndicator == null) {
//            mBusyIndicator = new ProgressBar(mContext);
//            mBusyIndicator.setIndeterminate(true);
//            mBusyIndicator.setBackgroundResource(R.drawable.busy);
//            addView(mBusyIndicator);
//        }

//        if (mBusyIndicatorNew == null) {
//            isLoadSuccess = false;
//            mBusyIndicatorNew = LayoutInflater.from(getContext()).inflate(R.layout.layout_loading_content, null, false);
//            RotateAnimation anim = new RotateAnimation(
//                    0f, 360f,
//                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
//            );
//            anim.setInterpolator(new LinearInterpolator());
//            anim.setDuration(500);
//            anim.setFillAfter(true);
//            anim.setRepeatCount(Animation.INFINITE);
//            mBusyIndicatorNew.findViewById(R.id.progressBar).startAnimation(anim);
//            addView(mBusyIndicatorNew);
//        }

        setBackgroundColor(BACKGROUND_COLOR);
    }

    private BitmapShader bitmapShader;
    private Matrix matrixGlass = new Matrix();
    private Paint paintGlass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float glassScale = 1f;

    private Bitmap bmGlass2;
    private Canvas canvasGlass2;
    private BitmapShader bitmapShader2;
    private Matrix matrixGlass2 = new Matrix();
    private Paint paintGlass2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float glassScale2 = 1f;
    private float readSale = 1f;
    private StructuredText.TextChar first;
    private StructuredText.TextChar second;


    Paint paint;
    Paint paintMinh;

    public void setPage(int page, PointF size) {
        pdfSize = correctBugMuPdf(size);
        if (mEntireBm == null) {
            try {
                Bitmap.Config config;
                if (android.os.Build.VERSION.SDK_INT <= 24) config = Config.ARGB_4444;
                else config = Config.ARGB_8888;
                mEntireBm = Bitmap.createBitmap(mParentSize.x, mParentSize.y, config);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }

        // Cancel pending render task
        if (mDrawEntire != null && page > 1) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }


        mIsBlank = false;
        // Highlights may be missing because mIsBlank was true on last draw
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }

        mPageNumber = page;
        if (mEntire == null) {
            mEntire = new OpaqueImageView(mContext);
            mEntire.setScaleType(ImageView.ScaleType.MATRIX);
            addView(mEntire);
        }

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        mSourceScale = Math.min(mParentSize.x / size.x, mParentSize.y / size.y);
        Point newSize = new Point((int) (size.x * mSourceScale), (int) (size.y * mSourceScale));
        mSize = newSize;

        mEntire.setImageBitmap(null);
        mEntire.invalidate();


        updateEntireCanvas(false);
        initSearchView();
//        invalidate();
        requestLayout();
    }


    private void initSearchView() {
        if (mSearchView == null) {
            mSearchView = new View(mContext) {
                @Override
                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    // Work out current total scale factor
                    // from source to view
                    final float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
                    if (canvasGlass2 != null)
                        canvasGlass2.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    final float scale2 = (bmGlass2 != null) ? (float) bmGlass2.getWidth() * scale / mSearchView.getWidth() : 1f;

                    if (isLoadSuccess && mPageNumber == ReaderView.currentSearchPage && ReaderView.currentSearchRect != null) {
                        paint.setColor(FIND_COLOR);
                        canvas.drawRect(ReaderView.currentSearchRect.left * scale, ReaderView.currentSearchRect.top * scale,
                                ReaderView.currentSearchRect.right * scale, ReaderView.currentSearchRect.bottom * scale,
                                paint);
                        if (canvasGlass2 != null)
                            canvasGlass2.drawRect(ReaderView.currentSearchRect.left * scale2, ReaderView.currentSearchRect.top * scale2,
                                    ReaderView.currentSearchRect.right * scale2, ReaderView.currentSearchRect.bottom * scale2,
                                    paint);

                    }


                    if (!mIsBlank && mLinks != null && mHighlightLinks) {
                        paint.setColor(LINK_COLOR);
                        for (LinkInfo link : mLinks) {
                            canvas.drawRect(link.rect.left * scale, link.rect.top * scale,
                                    link.rect.right * scale, link.rect.bottom * scale,
                                    paint);
                            if (canvasGlass2 != null)
                                canvasGlass2.drawRect(link.rect.left * scale2, link.rect.top * scale2,
                                        link.rect.right * scale2, link.rect.bottom * scale2,
                                        paint);
                        }
                    }

                    if (mSelectBox != null && mText != null) {
                        if (((MuPDFPageView) getParent()).getAutoMode() == AUTO_DELETE) return;
                        paint.setColor(HIGHLIGHT_COLOR);
                        paint.setStyle(Paint.Style.FILL);

                        listQuad.clear();
                        processSelectedText(new TextProcessor() {
                            RectF rect;
                            Quad quad1;

                            @Override
                            public void start() {
                                first = null;
                                second = null;
                            }

                            public void onStartLine() {
                                rect = new RectF();
                            }

                            @Override
                            public void onWord(StructuredText.TextChar word) {
                                if (quad1 == null) quad1 = new Quad(word.quad);
                                quad1.union(word.quad);
                                rect.union(word.quad.toRect().x0, word.quad.toRect().y0, word.quad.toRect().x1, word.quad.toRect().y1);
                            }

                            public void onEndLine() {
                                if (!rect.isEmpty()) {
                                    canvas.drawRect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale, paint);
                                    if (canvasGlass2 != null)
                                        canvasGlass2.drawRect(rect.left * scale2, rect.top * scale2, rect.right * scale2, rect.bottom * scale2, paint);
                                }
                                if (!rect.isEmpty()) {
                                    spaceY = Math.abs(rect.top - rect.bottom);
                                    listRect.add(rect);
                                }

                                if (quad1 != null) {
                                    listQuad.add(quad1);
                                    quad1 = null;
                                }
                            }

                            @Override
                            public void onFirstPosition(StructuredText.TextChar textChar) {
                                first = textChar;
                                mOriginalFirstPoint.set(textChar.quad.toRect().x0, textChar.quad.toRect().centerY());
                                mFirstPoint.set(textChar.quad.toRect().x0, textChar.quad.toRect().y0 - 50);
                                mFirstPointBelow.set(textChar.quad.toRect().x0, textChar.quad.toRect().y1 + 50);
                            }

                            @Override
                            public void onSecondPosition(StructuredText.TextChar textChar) {
                                second = textChar;
                                mOriginalSecondPoint.set(textChar.quad.toRect().x1, textChar.quad.toRect().centerY());
                                mSecondPoint.set(textChar.quad.toRect().x1, textChar.quad.toRect().y0 - 50);
                                mSecondPointBelow.set(textChar.quad.toRect().x1, textChar.quad.toRect().y1 + 50);

                            }
                        });

                        int circleSize = (int) (12);
                        listOldRect.clear();
                        listOldRect.addAll(listRect);
                        listRect.clear();
                        float newScale = scale * 0.9f;
                        if (scale > 1.8) newScale = 2.0f * 0.9f;

                        for (int i = 0; i < listOldRect.size(); i++) {
                            RectF rect = listOldRect.get(i);
                            canvas.drawRect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale, paint);
                        }
                        if (first != null) {
                            canvas.drawCircle(first.quad.ul_x * scale, (first.quad.ul_y) * scale - circleSize * newScale, circleSize * newScale, paintMinh);
                            canvas.drawLine(first.quad.ul_x * scale, first.quad.ul_y * scale, first.quad.ul_x * scale, first.quad.ll_y * scale, paintMinh);
                        }
                        if (second != null) {
                            canvas.drawCircle(second.quad.lr_x * scale, (second.quad.lr_y) * scale + circleSize * newScale, circleSize * newScale, paintMinh);
                            canvas.drawLine(second.quad.lr_x * scale, second.quad.ur_y * scale, second.quad.lr_x * scale, second.quad.lr_y * scale, paintMinh);
                        }

                    }

                    if (mItemSelectBox != null) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(3);
                        paint.setColor(BOX_COLOR);
                        canvas.drawRect(mItemSelectBox.left * scale, mItemSelectBox.top * scale, mItemSelectBox.right * scale, mItemSelectBox.bottom * scale, paint);
                        if (canvasGlass2 != null)
                            canvasGlass2.drawRect(mItemSelectBox.left * scale2, mItemSelectBox.top * scale2, mItemSelectBox.right * scale2, mItemSelectBox.bottom * scale2, paint);

                    }


                    if (mDrawing != null) {
                        Path path = new Path();
                        PointF p;

                        paint.setAntiAlias(true);
                        paint.setDither(true);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setStrokeCap(Paint.Cap.ROUND);

                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(INK_THICKNESS * scale);
                        paint.setColor(INK_COLOR);

                        Iterator<ArrayList<PointF>> it = mDrawing.iterator();
                        while (it.hasNext()) {
                            ArrayList<PointF> arc = it.next();
                            if (arc.size() >= 2) {
                                Iterator<PointF> iit = arc.iterator();
                                p = iit.next();
                                float mX = p.x * scale;
                                float mY = p.y * scale;
                                path.moveTo(mX, mY);
                                while (iit.hasNext()) {
                                    p = iit.next();
                                    float x = p.x * scale;
                                    float y = p.y * scale;
                                    path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                                    mX = x;
                                    mY = y;
                                }
                                path.lineTo(mX, mY);
                            } else {
                                p = arc.get(0);
                                canvas.drawCircle(p.x * scale, p.y * scale, INK_THICKNESS * scale / 2, paint);
                                canvasGlass2.drawCircle(p.x * scale2, p.y * scale2, INK_THICKNESS * scale2 / 2, paint);
                            }
                        }

                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(path, paint);

                    }
                }
            };
            addView(mSearchView);
            mSearchView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onGlobalLayout() {
                    searchViewWidth = mSearchView.getWidth();
                    searchViewHeight = mSearchView.getHeight();
                    mSearchView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                }
            });
        }
    }

    private int searchViewWidth = 1, searchViewHeight = 1;

    private void createGlassBitmap() {
        if (bmGlass2 == null) {
            try {
                glassRadius = (int) convertDpToPixel(65f, getContext());
                glassPadding = (int) convertDpToPixel(5f, getContext());
                glassShadow = (int) convertDpToPixel(3f, getContext());
                recycleBitmap(bmGlass2);
                Bitmap.Config config;
                if (android.os.Build.VERSION.SDK_INT <= 24) config = Config.ARGB_4444;
                else config = Config.ARGB_8888;
                bmGlass2 = Bitmap.createBitmap(searchViewWidth, searchViewHeight, config);
                canvasGlass2 = new Canvas(bmGlass2);
                bitmapShader2 = new BitmapShader(bmGlass2, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                paintGlass2.setShader(bitmapShader2);
                if (mEntireBm != null) {
                    bitmapShader = new BitmapShader(mEntireBm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    paintGlass.setShader(bitmapShader);
                    bitmapShadow = generateBallBitmap();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
    }

    public void dispatchDrawViewGroup(Canvas canvas, int width, int height) {
        if (bitmapShader != null && isShowGlass && listOldRect != null && listOldRect.size() > 0) {

//                        if (mSelectBox.right > mSelectBox.left || mSelectBox.bottom > mSelectBox.top) {
            canvas.save();
            canvas.translate(getLeft(), getTop());
            final float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
            float s = (float) mSearchView.getWidth() / mEntireBm.getWidth();
            float s2 = (float) mSearchView.getWidth() / bmGlass2.getWidth();

            float tx = rx * scale;
            float ty = ry * scale;

            Log.d(TAG, "onDrawTest: " + getTop() + " " + getLeft());

            matrixGlass.setScale(s, s);
            matrixGlass2.setScale(s2, s2);
            matrixGlass.postScale(2f, 2f, tx, ty);
            matrixGlass2.postScale(2f, 2f, tx, ty);
            float valueY = (glassRadius + glassPadding * 2 + glassShadow);
            float valueX = 0;
            if (dy < (glassRadius + glassPadding * 2 + glassShadow) * 2.5f)
                valueY = -(glassRadius + glassPadding * 2 + glassShadow);
            if (dx < (glassRadius + glassPadding * 2 + glassShadow))
                valueX = -(glassRadius + glassPadding * 2 + glassShadow);
            if (dx > width - (glassRadius + glassPadding * 2 + glassShadow))
                valueX = (glassRadius + glassPadding * 2 + glassShadow);
            matrixGlass.postTranslate(-valueX, -valueY);
            matrixGlass2.postTranslate(-valueX, -valueY);
            bitmapShader.setLocalMatrix(matrixGlass);
            bitmapShader2.setLocalMatrix(matrixGlass2);
            if (bitmapShadow != null)
                canvas.drawBitmap(bitmapShadow, tx - valueX - (glassPadding + glassRadius + glassShadow), ty - valueY - (glassPadding + glassRadius + glassShadow), null);
            canvas.drawCircle(tx - valueX, ty - valueY, glassRadius, paintGlass);
            canvas.drawCircle(tx - valueX, ty - valueY, glassRadius, paintGlass2);
            canvas.restore();

//                        }
        }
    }

    public static float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }


    private Bitmap bitmapShadow = null;
    private int glassRadius = 200;
    private int glassPadding = 20;
    private int glassShadow = 12;



    private Bitmap generateBallBitmap() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(glassShadow, 0, 0, Color.argb(80, 0, 0, 0));
        int x = glassRadius + glassPadding + glassShadow;
        Bitmap.Config config;
        if (android.os.Build.VERSION.SDK_INT <= 24) config = Config.ARGB_4444;
        else config = Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(2 * x, 2 * x, config);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawCircle(x, x, glassRadius + glassPadding, paint);
        canvas.drawCircle(x, x, glassRadius, paint);
        return bitmap;
    }

    private boolean isShowGlass = false;

    public void updateEntireCanvas(final boolean updateZoomed) {

        Bitmap bm = MuPdfConstant.mapBitmap.get(mPageNumber);
        if (bm != null) {
            isLoadSuccess = true;
            if (mSearchView != null) mSearchView.invalidate();
            if (mBusyIndicatorNew != null) {
                removeView(mBusyIndicatorNew);
                mBusyIndicatorNew = null;
            }
            mEntire.setImageBitmap(bm);
            // Draws the signatures on EntireCanvas after changing pages (post loading).
            flagHQ = false;
            mEntire.invalidate();
            setBackgroundColor(Color.TRANSPARENT);
            invalidate();
            return;
        }
        if (bitmap == null || bitmap.isRecycled()) {
            if (mEntireBm != null) {
                try {
                    bitmap = Bitmap.createBitmap(mEntireBm.getWidth(), mEntireBm.getHeight(), Config.ARGB_8888);
                }catch (Exception e){

                }

            }
        }

        Log.d("dsk3", "updateEntireCanvas: ");
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getDrawPageTask(bitmap, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void cancelAndWait() {
                super.cancelAndWait();
                flagHQ = false;
            }

            @Override
            public void onPreExecute() {
                isLoadSuccess = false;
                setBackgroundColor(BACKGROUND_COLOR);
                mEntire.setImageBitmap(null);
                mEntire.invalidate();

                if (mBusyIndicatorNew == null) {
                    mBusyIndicatorNew = LayoutInflater.from(getContext()).inflate(R.layout.layout_loading_content, null, false);
                    RotateAnimation anim = new RotateAnimation(
                            0f, 360f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                    );
                    anim.setInterpolator(new LinearInterpolator());
                    anim.setDuration(500);
                    anim.setFillAfter(true);
                    anim.setRepeatCount(Animation.INFINITE);
                    mBusyIndicatorNew.findViewById(R.id.progressBar).startAnimation(anim);

                    addView(mBusyIndicatorNew);
//                    mBusyIndicatorNew.setVisibility(INVISIBLE);
//                    mHandler.postDelayed(new Runnable() {
//                        public void run() {
//                            if (mBusyIndicatorNew != null)
//                                mBusyIndicatorNew.setVisibility(VISIBLE);
//                        }
//                    }, PROGRESS_DIALOG_DELAY);
                }

            }

            @Override
            public void onPostExecute(Void result) {
                try {
                    mEntireBm = bitmap.copy(bitmap.getConfig(), true);
//                MuPdfConstant.mapBitmap.put(mPageNumber,bitmap.copy(bitmap.getConfig(), true));

                    isLoadSuccess = true;
                    if (mSearchView != null) mSearchView.invalidate();
                    if (mBusyIndicatorNew != null) {
                        removeView(mBusyIndicatorNew);
                        mBusyIndicatorNew = null;
                    }
                    mEntire.setImageBitmap(mEntireBm);
                    // Draws the signatures on EntireCanvas after changing pages (post loading).

                    if (updateZoomed && (mPatchBm != null) && !mPatchBm.isRecycled()) {
                        Canvas zoomedCanvas = new Canvas(mPatchBm);
                    }
                    flagHQ = false;
                    mEntire.invalidate();
                    setBackgroundColor(Color.TRANSPARENT);
                    invalidate();

                }catch (Exception e){

                }


            }
        };
        mDrawEntire.execute();

    }

    private boolean isLoadSuccess = true;


    public void setSearchRect(RectF rect) {
        currentSearchRect = rect;
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }
    }

    ;

    public void setSearchBoxes(RectF searchBoxes[]) {
        mSearchBoxes = searchBoxes;
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }
    }

    public void setLinkHighlighting(boolean f) {
        mHighlightLinks = f;
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }
    }

    public void deselectText() {
        resetValuePointSelect();
        mSelectBox = null;
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }
    }

    private float dx, dy;
    private float rx, ry;

    float mX = 0;
    float mY = 0;

    public enum HIGHLIGHT_MODE {
        FIRST_POINT_TOUCH,
        SECOND_POINT_TOUCH,
        NORMAL,
        TOUCH,
        UNDEFINED
    }

    private HIGHLIGHT_MODE highLightMode = HIGHLIGHT_MODE.NORMAL;

    private float downX = 0, downY = 0;
    private float spaceDownX = 0, spaceDownY = 0;

    public HIGHLIGHT_MODE selectTextOnDown(float x, float y) {

        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX0 = (x - getLeft()) / scale;
        float docRelY0 = (y - getTop()) / scale;
        downX = docRelX0;
        downY = docRelY0;
        float checkY = 30 / scale;
        float checkX1 = 20 / scale;
        float checkX2 = 120 / scale;
        if ((docRelX0 > mSecondPoint.x - checkX1 && docRelX0 < mSecondPoint.x + checkX2) && (docRelY0 > mSecondPoint.y - checkY && docRelY0 < mSecondPointBelow.y + checkY) &&
                (docRelX0 > mFirstPoint.x - checkX2 && docRelX0 < mFirstPoint.x + checkX1) && (docRelY0 > mFirstPoint.y - checkY && docRelY0 < mFirstPointBelow.y + checkY)
        ) {
            highLightMode = HIGHLIGHT_MODE.UNDEFINED;
        } else if ((docRelX0 > mSecondPoint.x - checkX1 && docRelX0 < mSecondPoint.x + checkX2) && (docRelY0 > mSecondPoint.y - checkY && docRelY0 < mSecondPointBelow.y + checkY)) {
            highLightMode = HIGHLIGHT_MODE.SECOND_POINT_TOUCH;
            spaceDownX = docRelX0 - mOriginalSecondPoint.x;
            spaceDownY = docRelY0 - mOriginalSecondPoint.y;
        } else if ((docRelX0 > mFirstPoint.x - checkX2 && docRelX0 < mFirstPoint.x + checkX1) && (docRelY0 > mFirstPoint.y - checkY && docRelY0 < mFirstPointBelow.y + checkY)) {
            highLightMode = HIGHLIGHT_MODE.FIRST_POINT_TOUCH;
            spaceDownX = docRelX0 - mOriginalFirstPoint.x;
            spaceDownY = docRelY0 - mOriginalFirstPoint.y;
        } else {
            highLightMode = HIGHLIGHT_MODE.NORMAL;
        }
        return highLightMode;
    }

    private void resetValuePointSelect() {
        mFirstPoint = new PointF(0, 0);
        mFirstPointBelow = new PointF(0, 0);
        mOriginalFirstPoint = new PointF(0, 0);
        mSecondPoint = new PointF(0, 0);
        mSecondPointBelow = new PointF(0, 0);
        mOriginalSecondPoint = new PointF(0, 0);
    }

    private boolean isFirst = false;

    // bat dau boi dam chu
    public void longSelectText(float x0, float y0, float x1, float y1) {
        createGlassBitmap();
        longCickTouchUp = false;
        isLongClick = true;
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX0 = (x0 - getLeft()) / scale;
        float docRelY0 = (y0 - getTop()) / scale;
        float docRelX1 = (x1 - getLeft()) / scale;
        float docRelY1 = (y1 - getTop()) / scale;
        resetValuePointSelect();
        rx = docRelX1;
        ry = docRelY1;
        dx = x1;
        dy = y1;
        // Order on Y but maintain the point grouping
        if (docRelY0 <= docRelY1)
            mSelectBox = new RectF(docRelX0, docRelY0, docRelX1, docRelY1);
        else
            mSelectBox = new RectF(docRelX1, docRelY1, docRelX0, docRelY0);

        if (mSearchView != null) {
            setShowGlass(true);
            mSearchView.invalidate();
//            updateSearchView();
            highLightMode = HIGHLIGHT_MODE.TOUCH;

        }

        if (mGetText == null) {
            mGetText = new AsyncTask<Void, Void, StructuredText.TextBlock[]>() {
                @Override
                protected StructuredText.TextBlock[] doInBackground(Void... params) {
                    return mCore.getTextBlock(getPage());
                }

                @Override
                protected void onPostExecute(StructuredText.TextBlock[] result) {
                    mText = result;
                    if (mSearchView != null) {
                        mSearchView.invalidate();
//                        updateSearchView();
                    }
                }
            };

            mGetText.execute();
        }


    }

    float spaceY = 10f;

    public void setShowGlass(boolean isShow) {
        if (isShow != isShowGlass) {
            isShowGlass = isShow;
            mSearchView.invalidate();
        }
    }

    public void touchUp() {
        longCickTouchUp = true;
        setShowGlass(false);
        highLightMode = HIGHLIGHT_MODE.NORMAL;
//        resetValuePointSelect();
    }


    public void selectText(float x0, float y0, float x1, float y1) {
        if (mOriginalSecondPoint.x == 0 && mOriginalSecondPoint.y == 0 && mOriginalFirstPoint.x == 0 && mOriginalFirstPoint.y == 0)
            return;

        setShowGlass(true);
        isLongClick = false;
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        if (highLightMode == HIGHLIGHT_MODE.UNDEFINED) {
            if (x1 < x0 || y1 < y0) {
                highLightMode = HIGHLIGHT_MODE.FIRST_POINT_TOUCH;
                spaceDownX = downX - mOriginalFirstPoint.x;
                spaceDownY = downY - mOriginalFirstPoint.y;
            }
            if (x1 > x0 || y1 > y0) {
                highLightMode = HIGHLIGHT_MODE.SECOND_POINT_TOUCH;
                spaceDownX = downX - mOriginalSecondPoint.x;
                spaceDownY = downY - mOriginalSecondPoint.y;
            }

        }
        float docRelX0 = (x0 - getLeft()) / scale - spaceDownX;
        float docRelY0 = (y0 - getTop()) / scale - spaceDownY;
        float docRelX1 = (x1 - getLeft()) / scale - spaceDownX;
        float docRelY1 = (y1 - getTop()) / scale - spaceDownY;


        Log.d(TAG, "selectText: " + x0 + ":" + y0 + "  -  " + x1 + ":" + y1 + "  -  " + docRelX1 + ":" + docRelY1);

        // Order on Y but maintain the point grouping
//        if (docRelY0 <= docRelY1)
//            mSelectBox = new RectF(docRelX0, docRelY0, docRelX1, docRelY1);
//        else
//            mSelectBox = new RectF(docRelX1, docRelY1, docRelX0, docRelY0);

        float spaceRelY = spaceY;
        mX = docRelX1;
        mY = docRelY1;
        isLongClick = false;
        if (!longCickTouchUp && highLightMode != HIGHLIGHT_MODE.TOUCH) return;
        switch (highLightMode) {
            case NORMAL:
                return;
            case TOUCH:
                if (mOriginalFirstPoint.y > docRelY1) {
                    docRelY1 = mOriginalFirstPoint.y;
                }
                ry = docRelY1;
                if (docRelY1 > mFirstPoint.y && docRelY1 < mFirstPointBelow.y && docRelX1 < mOriginalFirstPoint.x + 10) {
                    break;
                } else {
                    mSelectBox = new RectF(mFirstPoint.x, mOriginalFirstPoint.y, docRelX1, docRelY1);
                }

                break;
            case FIRST_POINT_TOUCH:
                if (docRelY1 > mOriginalSecondPoint.y)
                    docRelY1 = mOriginalSecondPoint.y;

                if (docRelX1 >= mOriginalSecondPoint.x && docRelY1 >= mOriginalSecondPoint.y) {
                    docRelX1 = mOriginalSecondPoint.x - spaceRelY;
                    mSelectBox = new RectF(docRelX1, mOriginalSecondPoint.y, mOriginalSecondPoint.x, mOriginalSecondPoint.y);
                } else {
                    mSelectBox = new RectF(docRelX1, docRelY1, mOriginalSecondPoint.x, mOriginalSecondPoint.y);
                }
                break;
            case SECOND_POINT_TOUCH:
//                if (docRelY1 < mOriginalFirstPoint.y + spaceRelY * 0.5f)
                if (docRelY1 < mOriginalFirstPoint.y)
                    docRelY1 = mOriginalFirstPoint.y;

                if (docRelX1 <= mOriginalFirstPoint.x && docRelY1 <= mOriginalFirstPoint.y) {
                    docRelX1 = mOriginalFirstPoint.x + spaceRelY;
                    mSelectBox = new RectF(mFirstPoint.x, mOriginalFirstPoint.y, docRelX1, mOriginalFirstPoint.y);
                } else
                    mSelectBox = new RectF(mFirstPoint.x, mOriginalFirstPoint.y, docRelX1, docRelY1);
                break;
        }
        rx = docRelX1;
        ry = docRelY1;
        dx = x1;
        dy = y1;


        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }

    }



    public void startDraw(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;
        if (mDrawing == null)
            mDrawing = new ArrayList<>();

        ArrayList<PointF> arc = new ArrayList<PointF>();
        arc.add(new PointF(docRelX, docRelY));
        mDrawing.add(arc);
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }
    }

    public void continueDraw(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
            arc.add(new PointF(docRelX, docRelY));
            if (mSearchView != null) {
                mSearchView.invalidate();
                updateSearchView();
            }
        }
    }

    public void cancelDraw() {
        mDrawing = null;
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }

    }

    public void updateSearchView() {

    }


    ArrayList<Quad> listQuad = new ArrayList<>();

    protected void processSelectedText(TextProcessor tp) {
        (new TextSelector(mText, mSelectBox, isLongClick, isFirstPosition)).select(tp);
    }

    private boolean isEditing = false;

    public void setModeEdit(boolean isEdit) {
        isEditing = isEdit;
    }

    public void setItemSelectBox(RectF rect) {
//        if (isEditing) {
        Log.d("dsk2", "setItemSelectBox: " + rect);
        mItemSelectBox = rect;
        if (mItemSelectBox == null) {
//                ReaderView.allowsPopupMenu = false;
        }
        if (mSearchView != null) {
            mSearchView.invalidate();
            updateSearchView();
        }
//        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int x, y;
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                x = mSize.x;
                break;
            default:
                x = MeasureSpec.getSize(widthMeasureSpec);
        }
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                y = mSize.y;
                break;
            default:
                y = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(x, y);

//        if (mBusyIndicator != null) {
//            int limit = Math.min(mParentSize.x, mParentSize.y) / 2;
//            mBusyIndicator.measure(MeasureSpec.AT_MOST | limit, MeasureSpec.AT_MOST | limit);
//        }
        if (mBusyIndicatorNew != null) {
            int limit = Math.min(mParentSize.x, mParentSize.y) / 2;
            mBusyIndicatorNew.measure(MeasureSpec.EXACTLY | limit, MeasureSpec.EXACTLY | limit);
        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        Log.d(TAG, left + " - " + top + " - " + right + " - " + bottom);

        int w = right - left;
        int h = bottom - top;

        if (mEntire != null) {
            if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
                mEntireMat.setScale(w / (float) mSize.x, h / (float) mSize.y);
                mEntire.setImageMatrix(mEntireMat);
                mEntire.invalidate();
            }
            mEntire.layout(0, 0, w, h);
        }

        if (mSearchView != null) {
            mSearchView.layout(0, 0, w, h);
        }

        if (mPatchViewSize != null) {
            if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
                // Zoomed since patch was created
                mPatchViewSize = null;
                mPatchArea = null;
                if (mPatch != null) {
                    mPatch.setImageBitmap(null);
                    mPatch.invalidate();
                }
            } else {
                mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
            }
        }

        if (mBusyIndicatorNew != null) {
            int bw = mBusyIndicatorNew.getMeasuredWidth();
            int bh = mBusyIndicatorNew.getMeasuredHeight();
            mBusyIndicatorNew.layout(0, (h - bh) / 2, w, (h + bh) / 2);
        }
    }

    private boolean flagHQ = false;

    public void updateHq(boolean update) {
        if (!flagHQ) {
            flagHQ = true;
            Rect viewArea = new Rect(getLeft(), getTop(), getRight(), getBottom());

            if (viewArea.width() == mSize.x || viewArea.height() == mSize.y) {
                // If the viewArea's size matches the unzoomed size, there is no need for an hq patch
                if (mPatch != null) {
                    mPatch.setImageBitmap(null);
                    mPatch.invalidate();
                }
                flagHQ = false;
            } else {
                final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
                final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

                // Intersect and test that there is an intersection
                if (!patchArea.intersect(viewArea)) {
                    flagHQ = false;
                    return;
                }

                // Offset patch area to be relative to the view top left
                patchArea.offset(-viewArea.left, -viewArea.top);

                boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

                // If being asked for the same area as last time and not because of an update then nothing to do
//            if (area_unchanged && !update)
//                return;
//
//            boolean completeRedraw = !(area_unchanged && update);
                boolean completeRedraw = !area_unchanged || update;

                // Stop the drawing of previous patch if still going
                if (mDrawPatch != null) {
                    mDrawPatch.cancelAndWait();
                    mDrawPatch = null;
                }

                // Create and add the image view if not already done
                if (mPatch == null) {
                    mPatch = new OpaqueImageView(mContext);
                    mPatch.setScaleType(ImageView.ScaleType.MATRIX);
                    addView(mPatch);
                    if (mSearchView != null) {
                        mSearchView.bringToFront();
                    }
                }

                CancellableTaskDefinition<Void, Void> task;

                final Bitmap oldPatchBm = mPatchBm;
                try {
                    int mPatchAreaHeight = patchArea.bottom - patchArea.top;
                    int mPatchAreaWidth = patchArea.right - patchArea.left;
                    Bitmap.Config config;
                    if (android.os.Build.VERSION.SDK_INT <= 24) config = Config.ARGB_4444;
                    else config = Config.ARGB_8888;

                    mPatchBm = Bitmap.createBitmap(mPatchAreaWidth, mPatchAreaHeight, config);
                    Log.i(TAG, "Recycle oldPatchBm on updateHQ: " + oldPatchBm);
                    cancelDraw();
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, e.getMessage(), e);
                    flagHQ = false;
                }

                if (completeRedraw) {
                    task = getDrawPageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                            patchArea.left, patchArea.top,
                            patchArea.width(), patchArea.height());
                } else
                    task = getUpdatePageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                            patchArea.left, patchArea.top,
                            patchArea.width(), patchArea.height());

                mDrawPatch = new CancellableAsyncTask<Void, Void>(task) {

                    @Override
                    public void cancelAndWait() {
                        super.cancelAndWait();
                        flagHQ = false;
                    }

                    public void onPostExecute(Void result) {
                        mPatchViewSize = patchViewSize;
                        mPatchArea = patchArea;

                        if (mPatchBm != null && !mPatchBm.isRecycled()) {
                            Canvas zoomedCanvas = new Canvas(mPatchBm);
                            mPatch.setImageBitmap(mPatchBm);
                            mPatch.invalidate();
                        }

                        //requestLayout();
                        // Calling requestLayout here doesn't lead to a later call to layout. No idea
                        // why, but apparently others have run into the problem.
                        mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);

                        if (mPatchBm != null && !mPatchBm.equals(oldPatchBm)) {
                            recycleBitmap(oldPatchBm);
                        }
                        flagHQ = false;
                    }
                };

                mDrawPatch.execute();
            }
        }
    }


    Bitmap bitmap;

    public void update() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = Bitmap.createBitmap(mEntireBm.getWidth(), mEntireBm.getHeight(), Config.ARGB_8888);
        }

//        bitmap = Bitmap.createBitmap(mEntireBm.getWidth(), mEntireBm.getHeight(), Config.ARGB_8888);
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getUpdatePageTask(bitmap, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void cancelAndWait() {
                super.cancelAndWait();
                flagHQ = false;
            }

            public void onPostExecute(Void result) {
                mEntireBm = bitmap.copy(bitmap.getConfig(), true);
//                bitmap.recycle();
//                bitmap = null;
                if (mEntireBm != null && !mEntireBm.isRecycled()) {
//                    Canvas entireCanvas = new Canvas(mEntireBm);
//                    drawBitmaps(entireCanvas, null, null);

                    mEntire.setImageBitmap(mEntireBm);
                    mEntire.invalidate();
                    flagHQ = false;
                    updateHq(true);
                }
            }
        };
        mDrawEntire.execute();

    }

    public void removeHq() {
        // Stop the drawing of the patch if still going
        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        // And get rid of it
        mPatchViewSize = null;
        mPatchArea = null;
        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }
        flagHQ = false;
    }

    public int getPage() {
        return mPageNumber;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    /**
     * Check if a Bitmap exists in the point coordinates, and remove it.
     *
     * @param screenPoint Point for the pdf to check
     * @return
     */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


    }

    /**
     * Por defecto la medida de pagina que devuelve MuPdf parece ser dos veces superior al correcto
     *
     * @param size
     * @return
     */
    private PointF correctBugMuPdf(PointF size) {
        return new PointF(size.x / 2, size.y / 2);
    }

    public DigitalizedEventCallback getEventCallback() {
        return eventCallback;
    }

    public void setEventCallback(DigitalizedEventCallback eventCallback) {
        this.eventCallback = eventCallback;
    }


    public void setParentSize(Point parentSize) {
        this.mParentSize = parentSize;
    }



    public void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            Log.d(TAG, "Recycling bitmap " + bitmap.toString());
            bitmap.recycle();
            if (!bitmap.isRecycled()) {
                Log.e(TAG, "NOT Recycled bitmap " + bitmap.toString());
            }
        }
    }

}
