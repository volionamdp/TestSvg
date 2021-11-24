package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

import androidx.core.view.MotionEventCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;

import com.artifex.menu.CustomMenuAdapter;
import com.artifex.menu.DefaultEditMenu;
import com.artifex.menu.MyPopupMenu;
import com.artifex.utils.DigitalizedEventCallback;
import com.artifex.utils.PdfBitmap;
import com.skydoves.powermenu.CustomPowerMenu;
import com.skydoves.powermenu.OnMenuItemClickListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
//import me.kareluo.ui.OptionMenu;
//import me.kareluo.ui.OptionMenuView;
//import me.kareluo.ui.PopupMenuView;

public class ReaderView
        extends AdapterView<Adapter>
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener, Runnable {
    public enum ModeAuto {NORMAL, AUTO_HIGHLIGHT, AUTO_UNDERLINE, AUTO_STRIKE_THROUGH, AUTO_DELETE}

    protected boolean canEdit = true;

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    private static final int MOVING_DIAGONALLY = 0;
    private static final int MOVING_LEFT = 1;
    private static final int MOVING_RIGHT = 2;
    private static final int MOVING_UP = 3;
    private static final int MOVING_DOWN = 4;

    private static final int FLING_MARGIN = 100;
    private static final int GAP = 20;

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 4.0f;
    private static final float REFLOW_SCALE_FACTOR = 0.5f;

    private static final boolean HORIZONTAL_SCROLLING = false;

    private Adapter mAdapter;
    private int mCurrent;    // Adapter's index for the current view
    private boolean mResetLayout;
    private boolean mResetLayoutZooming;
    protected final SparseArray<View>
            mChildViews = new SparseArray<View>(5);
    // Shadows the children of the adapter view
    // but with more sensible indexing
    private final LinkedList<View> mViewCache = new LinkedList<View>();
    private boolean mUserInteracting;  // Whether the user is interacting
    private boolean mScaling;    // Whether the user is currently pinch zooming
    private float mScale = 1.0f;
    private int mXScroll;    // Scroll amounts recorded from events.
    private int mYScroll;    // and then accounted for in onLayout
    private boolean mReflow = false;
    private boolean mReflowChanged = false;
    private final GestureDetector
            mGestureDetector;
    private final ScaleGestureDetector
            mScaleGestureDetector;
    private final Scroller mScroller;
    private final Stepper mStepper;
    public static boolean allowsPopupMenu = true;

    PageView currentPage;
    private DigitalizedEventCallback eventCallback;

    private float mLastTouchX;
    private float mLastTouchY;
    public MuPDFReaderView.Mode mMode = MuPDFReaderView.Mode.Selecting;
    public boolean mEditing = true;
    protected ViewModeCallback mViewModeCallback;
    //    public MuPDFReaderView.Mode mMode = MuPDFReaderView.Mode.Viewing;
    protected ModeAuto mModeAuto = ModeAuto.NORMAL;


    public void setViewModeCallback(ViewModeCallback viewModeCallback) {
        this.mViewModeCallback = viewModeCallback;
        mViewModeCallback.showEditView(mEditing);
    }

    public static abstract class ViewMapper {
        public abstract void applyToView(View view);
    }

    public ModeAuto getModeAuto() {
        return mModeAuto;
    }

    public void setModeAuto(ModeAuto modeAuto) {
        this.mModeAuto = modeAuto;
        if (modeAuto == ModeAuto.AUTO_DELETE) {
            currentPage.mItemSelectBox = null;
        }
    }

    public ReaderView(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);
        mStepper = new Stepper(this, this);
        init(context);
    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // "Edit mode" means when the View is being displayed in the Android GUI editor. (this class
        // is instantiated in the IDE, so we need to be a bit careful what we do).
        if (isInEditMode()) {
            mGestureDetector = null;
            mScaleGestureDetector = null;
            mScroller = null;
            mStepper = null;
        } else {
            mGestureDetector = new GestureDetector(this);
            mScaleGestureDetector = new ScaleGestureDetector(context, this);
            mScroller = new Scroller(context);
            mStepper = new Stepper(this, this);
        }
        init(context);
    }

    public ReaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mGestureDetector = new GestureDetector(this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);
        mStepper = new Stepper(this, this);
        init(context);
    }

    private void init(Context context) {
        MyPopupMenu.getInstance(context);
        mPopupMenuView = new CustomPowerMenu.Builder<>(getContext(), new CustomMenuAdapter())
                .addItem(new DefaultEditMenu(getContext().getString(R.string.copy), "menu_copy", Annotation.Type.DEFAULT))
                .setBackgroundColor(Color.TRANSPARENT)
                .setOnMenuItemClickListener(onIconMenuItemClickListener)
                .setAutoDismiss(true)
                .build();
        if (canEdit) {
            mPopupMenuView.addItem(new DefaultEditMenu(getContext().getString(R.string.highlight), "menu_hl", Annotation.Type.HIGHLIGHT));
            mPopupMenuView.addItem(new DefaultEditMenu(getContext().getString(R.string.underline), "menu_under", Annotation.Type.UNDERLINE));
            mPopupMenuView.addItem(new DefaultEditMenu(getContext().getString(R.string.strikethrough), "menu_strike", Annotation.Type.STRIKEOUT));

        }

    }

    public void releaseView() {
        if (mPopupMenuView != null) {
            mPopupMenuView.dismiss();
        }
    }

    protected Handler handlerClickBackgroundPopup = null;


    public int getDisplayedViewIndex() {
        return mCurrent;
    }

    public void setDisplayedViewIndex(int i) {
        if (0 <= i && i < mAdapter.getCount()) {
            onMoveOffChild(mCurrent);
            mCurrent = i;
            onMoveToChild(i);
            mResetLayout = true;
            requestLayout();
        }
    }


    // When advancing down the page, we want to advance by about
    // 90% of a screenful. But we'd be happy to advance by between
    // 80% and 95% if it means we hit the bottom in a whole number
    // of steps.
    private int smartAdvanceAmount(int screenHeight, int max) {
        int advance = (int) (screenHeight * 0.9 + 0.5);
        int leftOver = max % advance;
        int steps = max / advance;
        if (leftOver == 0) {
            // We'll make it exactly. No adjustment
        } else if ((float) leftOver / steps <= screenHeight * 0.05) {
            // We can adjust up by less than 5% to make it exact.
            advance += (int) ((float) leftOver / steps + 0.5);
        } else {
            int overshoot = advance - leftOver;
            if ((float) overshoot / steps <= screenHeight * 0.1) {
                // We can adjust down by less than 10% to make it exact.
                advance -= (int) ((float) overshoot / steps + 0.5);
            }
        }
        if (advance > max)
            advance = max;
        return advance;
    }

    public void smartMoveForwards() {
        View v = mChildViews.get(mCurrent);
        if (v == null)
            return;

        // The following code works in terms of where the screen is on the views;
        // so for example, if the currentView is at (-100,-100), the visible
        // region would be at (100,100). If the previous page was (2000, 3000) in
        // size, the visible region of the previous page might be (2100 + GAP, 100)
        // (i.e. off the previous page). This is different to the way the rest of
        // the code in this file is written, but it's easier for me to think about.
        // At some point we may refactor this to fit better with the rest of the
        // code.

        // screenWidth/Height are the actual width/height of the screen. e.g. 480/800
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        // We might be mid scroll; we want to calculate where we scroll to based on
        // where this scroll would end, not where we are now (to allow for people
        // bashing 'forwards' very fast.
        int remainingX = mScroller.getFinalX() - mScroller.getCurrX();
        int remainingY = mScroller.getFinalY() - mScroller.getCurrY();
        // right/bottom is in terms of pixels within the scaled document; e.g. 1000
        int top = -(v.getTop() + mYScroll + remainingY);
        int right = screenWidth - (v.getLeft() + mXScroll + remainingX);
        int bottom = screenHeight + top;
        // docWidth/Height are the width/height of the scaled document e.g. 2000x3000
        int docWidth = v.getMeasuredWidth();
        int docHeight = v.getMeasuredHeight();

        int xOffset, yOffset;
        if (bottom >= docHeight) {
            // We are flush with the bottom. Advance to next column.
            if (right + screenWidth > docWidth) {
                // No room for another column - go to next page
                View nv = mChildViews.get(mCurrent + 1);
                if (nv == null) // No page to advance to
                    return;
                int nextTop = -(nv.getTop() + mYScroll + remainingY);
                int nextLeft = -(nv.getLeft() + mXScroll + remainingX);
                int nextDocWidth = nv.getMeasuredWidth();
                int nextDocHeight = nv.getMeasuredHeight();

                // Allow for the next page maybe being shorter than the screen is high
                yOffset = (nextDocHeight < screenHeight ? ((nextDocHeight - screenHeight) >> 1) : 0);

                if (nextDocWidth < screenWidth) {
                    // Next page is too narrow to fill the screen. Scroll to the top, centred.
                    xOffset = (nextDocWidth - screenWidth) >> 1;
                } else {
                    // Reset X back to the left hand column
                    xOffset = right % screenWidth;
                    // Adjust in case the previous page is less wide
                    if (xOffset + screenWidth > nextDocWidth)
                        xOffset = nextDocWidth - screenWidth;
                }
                xOffset -= nextLeft;
                yOffset -= nextTop;
            } else {
                // Move to top of next column
                xOffset = screenWidth;
                yOffset = screenHeight - bottom;
            }
        } else {
            // Advance by 90% of the screen height downwards (in case lines are partially cut off)
            xOffset = 0;
            yOffset = smartAdvanceAmount(screenHeight, docHeight - bottom);
        }
        mScroller.startScroll(0, 0, remainingX - xOffset, remainingY - yOffset, 400);
        mStepper.prod();
    }

    public void smartMoveBackwards() {
        View v = mChildViews.get(mCurrent);
        if (v == null)
            return;

        // The following code works in terms of where the screen is on the views;
        // so for example, if the currentView is at (-100,-100), the visible
        // region would be at (100,100). If the previous page was (2000, 3000) in
        // size, the visible region of the previous page might be (2100 + GAP, 100)
        // (i.e. off the previous page). This is different to the way the rest of
        // the code in this file is written, but it's easier for me to think about.
        // At some point we may refactor this to fit better with the rest of the
        // code.

        // screenWidth/Height are the actual width/height of the screen. e.g. 480/800
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        // We might be mid scroll; we want to calculate where we scroll to based on
        // where this scroll would end, not where we are now (to allow for people
        // bashing 'forwards' very fast.
        int remainingX = mScroller.getFinalX() - mScroller.getCurrX();
        int remainingY = mScroller.getFinalY() - mScroller.getCurrY();
        // left/top is in terms of pixels within the scaled document; e.g. 1000
        int left = -(v.getLeft() + mXScroll + remainingX);
        int top = -(v.getTop() + mYScroll + remainingY);
        // docWidth/Height are the width/height of the scaled document e.g. 2000x3000
        int docHeight = v.getMeasuredHeight();

        int xOffset, yOffset;
        if (top <= 0) {
            // We are flush with the top. Step back to previous column.
            if (left < screenWidth) {
                /* No room for previous column - go to previous page */
                View pv = mChildViews.get(mCurrent - 1);
                if (pv == null) /* No page to advance to */
                    return;
                int prevDocWidth = pv.getMeasuredWidth();
                int prevDocHeight = pv.getMeasuredHeight();

                // Allow for the next page maybe being shorter than the screen is high
                yOffset = (prevDocHeight < screenHeight ? ((prevDocHeight - screenHeight) >> 1) : 0);

                int prevLeft = -(pv.getLeft() + mXScroll);
                int prevTop = -(pv.getTop() + mYScroll);
                if (prevDocWidth < screenWidth) {
                    // Previous page is too narrow to fill the screen. Scroll to the bottom, centred.
                    xOffset = (prevDocWidth - screenWidth) >> 1;
                } else {
                    // Reset X back to the right hand column
                    xOffset = (left > 0 ? left % screenWidth : 0);
                    if (xOffset + screenWidth > prevDocWidth)
                        xOffset = prevDocWidth - screenWidth;
                    while (xOffset + screenWidth * 2 < prevDocWidth)
                        xOffset += screenWidth;
                }
                xOffset -= prevLeft;
                yOffset -= prevTop - prevDocHeight + screenHeight;
            } else {
                // Move to bottom of previous column
                xOffset = -screenWidth;
                yOffset = docHeight - screenHeight + top;
            }
        } else {
            // Retreat by 90% of the screen height downwards (in case lines are partially cut off)
            xOffset = 0;
            yOffset = -smartAdvanceAmount(screenHeight, top);
        }
        mScroller.startScroll(0, 0, remainingX - xOffset, remainingY - yOffset, 400);
        mStepper.prod();
    }

    public void resetupChildren() {
        for (int i = 0; i < mChildViews.size(); i++)
            onChildSetup(mChildViews.keyAt(i), mChildViews.valueAt(i));
    }

    public void applyToChildren(ViewMapper mapper) {
        for (int i = 0; i < mChildViews.size(); i++)
            mapper.applyToView(mChildViews.valueAt(i));
    }

    public void refresh(boolean reflow) {
        mReflow = reflow;
        mReflowChanged = true;
        mResetLayout = true;

        mScale = 1.0f;
        mXScroll = mYScroll = 0;

        requestLayout();
    }

    protected void onChildSetup(int i, View v) {
    }

    protected void onMoveToChild(int i) {
    }

    protected void onMoveOffChild(int i) {
    }

    protected void onSettle(View v) {
    }


    protected void onUnsettle(View v) {
    }


    protected void onNotInUse(View v) {
        ((PageView) v).releaseResources();
    }

    ;

    protected void onScaleChild(View v, Float scale) {

    }

    public View getView(int i) {
        return mChildViews.get(i);
    }


    int mCurrentViewTouch = 0;

    public View getDisplayedView() {
        return mChildViews.get(mCurrentViewTouch);
    }

    public View getDisplayedViewByTouch(MotionEvent event) {
        for (int i = 0; i < mChildViews.size(); i++) {
            int key = mChildViews.keyAt(i);
            View v = mChildViews.get(key);
            if (event.getY() > v.getTop() && event.getY() < v.getBottom()) {
                mCurrentViewTouch = key;
                return v;
            }
        }
        return null;
    }

    public View getDisplayedViewByTouch(int y) {
        for (int i = 0; i < mChildViews.size(); i++) {
            int key = mChildViews.keyAt(i);
            View v = mChildViews.get(key);
            if (y > v.getTop() && y < v.getBottom()) {
                mCurrentViewTouch = i;
                return v;
            }
        }
        return null;
    }


    public void run() {
//        if (!mScroller.isFinished()) {
//            mScroller.computeScrollOffset();
//            int x = mScroller.getCurrX();
//            int y = mScroller.getCurrY();
//            mXScroll += x - mScrollerLastX;
//            mYScroll += y - mScrollerLastY;
//            mScrollerLastX = x;
//            mScrollerLastY = y;
//            requestLayout();
//            mStepper.prod();
//        } else if (!mUserInteracting) {
//            // End of an inertial scroll and the user is not interacting.
//            // The layout is stable
//            View v = mChildViews.get(mCurrent);
//            if (v != null)
//                postSettle(v);
//        }
    }

    public boolean onDown(MotionEvent arg0) {
        mScroller.forceFinished(true);
        return true;
    }


    private int oldValue = 0;

    private FlingAnimation flingAnimationX;

    private void startFlingAnimationX(float velocityX) {
        oldValue = 0;
        if (flingAnimationX != null) {
            flingAnimationX.cancel();
        }

        flingAnimationX = new FlingAnimation(new FloatValueHolder());
        flingAnimationX.setStartVelocity(velocityX);
        flingAnimationX.setMaxValue((Math.abs(velocityX) / 2));
//        flingAnimationX.setMinValue(-10000);
        flingAnimationX.setMinValue(-(Math.abs(velocityX) / 2));
        flingAnimationX.setFriction(0.5f);
//        flingAnimationX.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS);
        flingAnimationX.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                final int x = Math.round(value);
                mYScroll = x - oldValue;
                requestLayout();
                oldValue = x;
            }
        });
        flingAnimationX.start();
    }

    private float startPixel = 0;

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
//        startPixel = e1.getY();
        startFlingAnimationX(velocityY);
//        if (flingAnimation != null) {
//            flingAnimation.cancel();
//        }
//        flingAnimation = createAnimation(
//                startPixel,
//                velocityY,
//               10000,
//                0
//        );
//        flingAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
//            @Override
//            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
//                Log.d("dsk6", "onAnimationUpdate: "+value);
//                startPixel = value;
////                mYScroll = x - oldValue;
//                mYScroll = -(int) value;
//                requestLayout();
//            }
//        });


//        flingAnimation.addUpdateListener() {
//            _, value, _ ->
//                    startPixel = value
//            listener ?.updateTime(getStart(), getEnd())
//            invalidate()
//        }

//        flingAnimation.start();
        return true;
    }

    private FlingAnimation createAnimation(
            float startValue,
            float startVelocity,
            float maxValue,
            float minValue
    ) {
        return new FlingAnimation(new FloatValueHolder(startValue))
                .setStartVelocity(startVelocity)
                .setMaxValue(maxValue)
                .setMinValue(minValue)
                .setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
                .setFriction(0.9f);
    }


    private FlingAnimation flingAnimation;


    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        if (flingAnimationX != null) {
            flingAnimationX.cancel();
        }


        if (!mScaling) {

            mXScroll -= distanceX * 1.5;
            mYScroll -= distanceY * 1.5;
            requestLayout();
        }
        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    public boolean onScale(ScaleGestureDetector detector) {
        float previousScale = mScale;
        float scale_factor = mReflow ? REFLOW_SCALE_FACTOR : 1.0f;
        float min_scale = MIN_SCALE * scale_factor;
        float max_scale = MAX_SCALE * scale_factor;
        mScale = Math.min(Math.max(mScale * detector.getScaleFactor(), min_scale), max_scale);

        if (mReflow) {
            View v = mChildViews.get(mCurrent);
            if (v != null)
                onScaleChild(v, mScale);
        } else {
            float factor = mScale / previousScale;

            View v = mChildViews.get(mCurrent);
            if (v != null) {
                // Work out the focus point relative to the view top left
                int viewFocusX = (int) detector.getFocusX() - (v.getLeft() + mXScroll);
                int viewFocusY = (int) detector.getFocusY() - (v.getTop() + mYScroll);
                // Scroll to maintain the focus point
                mXScroll += viewFocusX - viewFocusX * factor;
                mYScroll += viewFocusY - viewFocusY * factor;
                requestLayout();
            }
        }
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mScaling = true;
        // Ignore any scroll amounts yet to be accounted for: the
        // screen is not showing the effect of them, so they can
        // only confuse the user
        mXScroll = mYScroll = 0;
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        if (mReflow) {
            applyToChildren(new ViewMapper() {
                @Override
                public void applyToView(View view) {
                    onScaleChild(view, mScale);
                }
            });
        }
        mScaling = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (MyPopupMenu.getInstance(getContext()).setTouch(event)) {
            invalidate();
            return false;
        }
        boolean movementEnd = false;

        // We need this check to avoid refreshing the screen after a "tap" or "double tap". We only want to refresh the PDF after a pan, pinch or drag.
        int ident = MotionEventCompat.getActionIndex(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLastTouchX = MotionEventCompat.getX(event, ident);
            mLastTouchY = MotionEventCompat.getY(event, ident);
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float upX = MotionEventCompat.getX(event, ident);
            float upY = MotionEventCompat.getY(event, ident);
            int displacementX = (int) Math.abs(mLastTouchX - upX);
            int displacementY = (int) Math.abs(mLastTouchY - upY);
            movementEnd = (displacementX > 10) || (displacementY > 10);
        }
        if (event.getPointerCount() > 1) isOnePointCount = false;
        processTouchEvent(event, movementEnd);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            isOnePointCount = true;
            // cap nhat hinh anh net
//            if (currentPage != null && mScale != 1f) currentPage.updateHq(true);
            PageView pageView = (PageView) getDisplayedViewByTouch(event);
            if (pageView != null && mScale != 1f) pageView.updateHq(true);
//            requestLayout();
        }
        return true;
    }


    protected boolean isOnePointCount = true;
    private boolean isSearchWord = false;
    public static RectF currentSearchRect;
    public static int currentSearchPage = -1;
    protected boolean isLongClicked = false;
    private int maxPageInScreen = 0;

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        MuPDFView pageView = (MuPDFView) getDisplayedView();
        if (pageView != null) pageView.dispatchDrawViewGroup(canvas, getWidth(), getHeight());
        MyPopupMenu.getInstance(getContext()).drawMenu(canvas);
    }

    public void setCurrentSearchRect(RectF rect, int page) {
        if (currentPage != null) {
            currentPage.setCurrentSearchRect(rect);
        }
        currentSearchRect = rect;
        currentSearchPage = page;
        isSearchWord = rect != null;
    }

    //todo show popup edit
    protected void showPopupMenu(float x, final float y) {
        MyPopupMenu myPopupMenu = MyPopupMenu.getInstance(getContext());
        if (canEdit) {
            myPopupMenu.setItemMenuEditDefault();
        } else {
            myPopupMenu.setItemMenuEditCopy();
        }
        myPopupMenu.setClickListener(new MyPopupMenu.ClickListener() {
            @Override
            public void clickItem(DefaultEditMenu defaultEditMenu) {
                MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch((int) y);
//                MuPDFView pageView = (MuPDFView) getDisplayedView();
                switch (defaultEditMenu.getId()) {
                    case "menu_under":
                        if (pageView != null)
                            pageView.markupSelection(Annotation.Type.UNDERLINE);
                        break;
                    case "menu_copy":
                        actionCopy(pageView);
                        break;
                    case "menu_hl":
//                        if (pageView != null)
//                            success = pageView.markupSelection(Annotation.Type.HIGHLIGHT);
                        actionHighLight(pageView);
                        break;
                    case "menu_strike":
                        if (pageView != null)
                            pageView.markupSelection(Annotation.Type.STRIKEOUT);
                        break;
                }
            }
        });
        myPopupMenu.showMenu(x, y);
        invalidate();
    }

    private void processTouchEvent(MotionEvent event, boolean withRefresh) {
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            mUserInteracting = true;
        }
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mUserInteracting = false;

//            View v = mChildViews.get(mCurrent);
            View v = getDisplayedViewByTouch(event);
            if (v != null && withRefresh && isOnePointCount) {
                if (mMode == MuPDFReaderView.Mode.Selecting) {
                    MuPDFView pageView = (MuPDFView) getDisplayedView();
                    if (mModeAuto == ModeAuto.NORMAL && !isLongClicked) {
                        ArrayList<RectF> list = currentPage.getListOldRect();
                        if (list != null && list.size() > 0 && allowsPopupMenu) {
//                            allowsPopupMenu = false;
                            showPopupMenu(event.getX() + 50, (int) event.getY());
                        }
                    } else if (mModeAuto == ModeAuto.AUTO_HIGHLIGHT) {
                        if (pageView != null)
                            pageView.markupSelection(Annotation.Type.HIGHLIGHT);
                    } else if (mModeAuto == ModeAuto.AUTO_UNDERLINE) {
                        if (pageView != null)
                            pageView.markupSelection(Annotation.Type.UNDERLINE);
                    } else if (mModeAuto == ModeAuto.AUTO_STRIKE_THROUGH) {
                        if (pageView != null)
                            pageView.markupSelection(Annotation.Type.STRIKEOUT);

                    }

                }
//                Toast.makeText(getContext(), "x - y :" + event.getX() + " - " + event.getX(), Toast.LENGTH_SHORT).show();
                if (mScroller.isFinished()) {
                    // If, at the end of user interaction, there is no
                    // current inertial scroll in operation then animate
                    // the view onto screen if necessary
                    slideViewOntoScreen(v);
                }

                if (mScroller.isFinished()) {
                    // If still there is no inertial scroll in operation
                    // then the layout is stable
                    postSettle(v);
                }
            }
        }

        requestLayout();
    }

    private OnMenuItemClickListener<DefaultEditMenu> onIconMenuItemClickListener = new OnMenuItemClickListener<DefaultEditMenu>() {
        @Override
        public void onItemClick(int position, DefaultEditMenu menu) {
            mPopupMenuView.dismiss();
            MuPDFView pageView = (MuPDFView) getDisplayedView();
            boolean success = false;
            switch (menu.getId()) {
                case "menu_under":
                    if (pageView != null)
                        success = pageView.markupSelection(Annotation.Type.UNDERLINE);
                    break;
                case "menu_copy":
                    actionCopy(pageView);
                    break;
                case "menu_hl":
                    actionHighLight(pageView);
                    break;
                case "menu_strike":
                    if (pageView != null)
                        success = pageView.markupSelection(Annotation.Type.STRIKEOUT);
                    break;
            }

        }
    };

    private void actionHighLight(MuPDFView pageView) {
        if (pageView != null)
            pageView.markupSelection(Annotation.Type.HIGHLIGHT);
    }

    private void actionCopy(MuPDFView pageView) {
        if (pageView != null)
            pageView.copySelection();
    }


    CustomPowerMenu mPopupMenuView;
    private Rect pageOneRect = new Rect();
    private Rect pageZeroRect = new Rect();

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        MyPopupMenu.getInstance(getContext()).setWidthHeight(getWidth(), getHeight());
        int n = getChildCount();
        for (int i = 0; i < n; i++)
            measureView(getChildAt(i));
    }

    private boolean isCheckChangePage = true;
    private int heightPage = 0;
    private int marginPage = 50;
    private View lastView;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // "Edit mode" means when the View is being displayed in the Android GUI editor. (this class
        // is instantiated in the IDE, so we need to be a bit careful what we do).
        if (isInEditMode())
            return;

//        Log.d("dsk8", "size: "+mChildViews.size());

        View cv = mChildViews.get(mCurrent);
        Point cvOffset;

        if (!mResetLayout) {
            // Move to next or previous if current is sufficiently off center
            isCheckChangePage = true;
            if (cv != null) {

                cvOffset = subScreenSizeOffset(cv);
                if (Math.abs((cv.getTop() - cvOffset.y - GAP / 2 + mYScroll)) < getHeight() / 2)
                    isCheckChangePage = true;

                if (mResetLayoutZooming) {
                    isCheckChangePage = false;
                }

                if (mCurrent == 0 && mYScroll > 0 && cv.getTop() > 0) {
                    mYScroll = 0;
                }


                // cv.getRight() may be out of date with the current scale
                // so add left to the measured width for the correct position

                if (cv.getBottom() <= 0 && mYScroll < 0) {
                    onMoveOffChild(mCurrent);
                    mCurrent++;
                    onMoveToChild(mCurrent);
                }

                if (cv.getTop() > marginPage && mYScroll > 0) {
                    onMoveOffChild(mCurrent);
                    mCurrent--;
                    onMoveToChild(mCurrent);
                }
            }

            // Remove not needed children and hold them for reuse
            int numChildren = mChildViews.size();
            int childIndices[] = new int[numChildren];
            for (int i = 0; i < numChildren; i++)
                childIndices[i] = mChildViews.keyAt(i);

            for (int i = 0; i < numChildren; i++) {
                int ai = childIndices[i];
                if (ai < mCurrent - 2 || ai > mCurrent + 2) {
                    View v = mChildViews.get(ai);
                    onNotInUse(v);
                    mViewCache.add(v);
                    removeViewInLayout(v);
                    mChildViews.remove(ai);
                }
            }

        } else {
            mResetLayout = false;
            mXScroll = mYScroll = 0;

            // Remove all children and hold them for reuse
            int numChildren = mChildViews.size();
            for (int i = 0; i < numChildren; i++) {
                View v = mChildViews.valueAt(i);
                onNotInUse(v);
                mViewCache.add(v);
                removeViewInLayout(v);
            }
            mChildViews.clear();

            // Don't reuse cached views if the adapter has changed
            if (mReflowChanged) {
                mReflowChanged = false;
                mViewCache.clear();
            }

            // post to ensure generation of hq area
            mStepper.prod();
        }

        // Ensure current view is present
        int cvLeft, cvRight, cvTop, cvBottom;
        boolean notPresent = (mChildViews.get(mCurrent) == null);
        cv = getOrCreateChild(mCurrent);
        currentPage = (PageView) cv;
        lastView = null;
        if (mCurrent == mAdapter.getCount() - 1) {
            lastView = cv;
        }
        currentPage.setEventCallback(eventCallback);
        PointF pdfSize = currentPage.pdfSize;
        if (pdfSize != null) {
            heightPage = (int) (getWidth() * pdfSize.y / pdfSize.x);
            if (maxPageInScreen == 0 && heightPage >= 0) {
                try {
                    maxPageInScreen = getHeight() / heightPage + 1;
                } catch (Exception e) {
                    maxPageInScreen = 1;
                }
            }
            if (heightPage < 0) heightPage = 1;
            heightPage = (int) (heightPage * mScale);
        }


        if (lastView != null) {
            Log.d("nam3", "bottom1: " + lastView.getBottom());
            // gioi han bien duoi cua pdf
            if (mYScroll < 0 && lastView.getBottom() < getHeight()) {
                mYScroll = 0;
                onMoveToChild(mAdapter.getCount() - 1);
            }
        }

        // gioi han bien duoi cua pdf
//        if (getHeight() * mScale > heightPage && mCurrent >= (mAdapter.getCount() - 2) && mYScroll < 0 && cv.getBottom() < (getHeight() - heightPage)) {
//            mYScroll = 0;
//            onMoveToChild(mCurrent + 1);
//        }


//        if (mCurrent == mAdapter.getCount() - maxPageInScreen) {
//            View rv = getOrCreateChild(mAdapter.getCount() - 1);
//            int bottom1 = rv.getBottom();
//            Log.d("nam2", "bottom1: " + bottom1);
//        }

        currentPage.setParentSize(new Point(right - left, bottom - top));

        // When the view is sub-screen-size in either dimension we
        // offset it to center within the screen area, and to keep
        // the views spaced out
        cvOffset = subScreenSizeOffset(cv);
        if (notPresent) {
            //Main item not already present. Just place it top left
            cvLeft = cvOffset.x;
            cvTop = cvOffset.y;
        } else {
            // Main item already present. Adjust by scroll offsets
            cvLeft = cv.getLeft() + mXScroll;
            // gioi han trai
            if (cvLeft > 0) cvLeft = 0;
            // gioi han phai
            if (cvLeft + cv.getMeasuredWidth() < getWidth()) {
                cvLeft = getWidth() - cv.getMeasuredWidth();
            }
            cvTop = cv.getTop() + mYScroll;
        }
        // Scroll values have been accounted for
        mXScroll = mYScroll = 0;
        cvRight = cvLeft + cv.getMeasuredWidth();
        cvBottom = cvTop + heightPage;

//        if (!mUserInteracting && mScroller.isFinished()) {
//            Rect rect = getScrollBounds(cvLeft, cvTop, cvRight, cvBottom);
//            Point corr = getCorrection(rect);
//            cvRight += corr.x;
//            cvLeft += corr.x;
//            cvTop += corr.y;
//            cvBottom += corr.y;
////            Log.d("dsk3", cvLeft + " - "+ cvTop + " - "+ cvRight + " - "+  cvBottom);
//        } else if (HORIZONTAL_SCROLLING && cv.getMeasuredHeight() <= getHeight()) {
//            // When the current view is as small as the screen in height, clamp
//            // it vertically
//            Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
//            cvTop += corr.y;
//            cvBottom += corr.y;
//        } else if (!HORIZONTAL_SCROLLING && cv.getMeasuredWidth() <= getWidth()) {
//            // When the current view is as small as the screen in width, clamp
//            // it horizontally
//            Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
//            cvRight += corr.x;
//            cvLeft += corr.x;
//        }

        //todo key
//        Log.d("dsk1", "cvTop: " + cvTop);
        cv.layout(cvLeft, cvTop, cvRight, cvBottom);

        // item trc do
        if (mCurrent > 0) {
            View lv = getOrCreateChild(mCurrent - 1);
            lv.layout((cvLeft + cvRight - lv.getMeasuredWidth()) / 2,
                    cvTop - heightPage,
                    (cvLeft + cvRight + lv.getMeasuredWidth()) / 2,
                    cvTop);
        }

        // ve item 2
        if (mCurrent + 1 < mAdapter.getCount() && !mResetLayoutZooming) {
            View rv = getOrCreateChild(mCurrent + 1);
            pageOneRect.left = (cvLeft + cvRight - rv.getMeasuredWidth()) / 2;
            pageOneRect.top = cvBottom + marginPage;
            pageOneRect.right = (cvLeft + cvRight + rv.getMeasuredWidth()) / 2;
            pageOneRect.bottom = cvBottom + marginPage + heightPage;
            if (mCurrent == mAdapter.getCount() - 2) {
                lastView = rv;
            }
            rv.layout(pageOneRect.left,
                    pageOneRect.top,
                    pageOneRect.right, pageOneRect.bottom);
        }

        // ve item 3
        if (mCurrent + 2 < mAdapter.getCount() && !mResetLayoutZooming) {
            View rv = getOrCreateChild(mCurrent + 2);
            if (mCurrent == mAdapter.getCount() - 3) {
                lastView = rv;
            }
            rv.layout(pageOneRect.left,
                    pageOneRect.bottom + marginPage,
                    pageOneRect.right, pageOneRect.bottom + marginPage + heightPage);
        }


//        if (mCurrent + 2 < mAdapter.getCount() && !mResetLayoutZooming) {
//            View rv = getOrCreateChild(mCurrent + 2);
//            Point rightOffset = subScreenSizeOffset(rv);
//            if (HORIZONTAL_SCROLLING) {
//                int gap = cvOffset.x + GAP + rightOffset.x;
//                rv.layout(cvRight + gap,
//                        (cvBottom + cvTop - heightPage) / 2,
//                        cvRight + rv.getMeasuredWidth() + gap,
//                        (cvBottom + cvTop + rv.getMeasuredHeight()) / 2);
//            } else {
////                int gap = cvOffset.y + GAP + rightOffset.y;
//                rv.layout((cvLeft + cvRight - rv.getMeasuredWidth()) / 2,
//                        pageOneRect.bottom + marginPage,
//                        (cvLeft + cvRight + rv.getMeasuredWidth()) / 2,
//                        pageOneRect.bottom + marginPage + heightPage);
//
//            }
//        }


        invalidate();
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;

        requestLayout();
    }

    @Override
    public void setSelection(int arg0) {
        throw new UnsupportedOperationException(getContext().getString(R.string.not_supported));
    }

    private View getCached() {
        if (mViewCache.size() == 0)
            return null;
        else
            return mViewCache.removeFirst();
    }

    private View getOrCreateChild(int i) {
        View v = mChildViews.get(i);
        if (v == null) {
            v = mAdapter.getView(i, getCached(), this);
            addAndMeasureChild(i, v);
            onChildSetup(i, v);
            onScaleChild(v, mScale);
        }

        return v;
    }

    private void addAndMeasureChild(int i, View v) {
        LayoutParams params = v.getLayoutParams();
        if (params == null) {
//            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params = new LayoutParams(100, 100);
        }
        addViewInLayout(v, 0, params, true);
        mChildViews.append(i, v); // Record the view against it's adapter index
        measureView(v);
    }

    private void measureView(View v) {
        // See what size the view wants to be
        v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        if (!mReflow) {
            // Work out a scale that will fit it to this view
//		float scale = Math.min((float)getWidth()/(float)v.getMeasuredWidth(),
//					(float)getHeight()/(float)v.getMeasuredHeight());
            float scale = (float) getWidth() / (float) v.getMeasuredWidth();
            // Use the fitting values scaled by our current scale factor
            v.measure(MeasureSpec.EXACTLY | (int) (v.getMeasuredWidth() * scale * mScale),
                    MeasureSpec.EXACTLY | (int) (v.getMeasuredHeight() * scale * mScale));
        } else {
            v.measure(MeasureSpec.EXACTLY | (int) (v.getMeasuredWidth()),
                    MeasureSpec.EXACTLY | (int) (v.getMeasuredHeight()));
        }
    }

    private Rect getScrollBounds(int left, int top, int right, int bottom) {
        int xmin = getWidth() - right;
        int xmax = -left;
        int ymin = heightPage - bottom;
        int ymax = -top;

        // In either dimension, if view smaller than screen then
        // constrain it to be central
        if (xmin > xmax) xmin = xmax = (xmin + xmax) / 2;
        if (ymin > ymax) ymin = ymax = (ymin + ymax) / 2;

        return new Rect(xmin, ymin, xmax, ymax);
    }

    private Rect getScrollBounds(View v) {
        // There can be scroll amounts not yet accounted for in
        // onLayout, so add mXScroll and mYScroll to the current
        // positions when calculating the bounds.
        return getScrollBounds(v.getLeft() + mXScroll,
                v.getTop() + mYScroll,
                v.getLeft() + v.getMeasuredWidth() + mXScroll,
                v.getTop() + v.getMeasuredHeight() + mYScroll);
    }

    private Point getCorrection(Rect bounds) {
        return new Point(Math.min(Math.max(0, bounds.left), bounds.right),
                Math.min(Math.max(0, bounds.top), bounds.bottom));
    }

    private void postSettle(final View v) {
        // onSettle and onUnsettle are posted so that the calls
        // wont be executed until after the system has performed
        // layout.
        post(new Runnable() {
            public void run() {
                onSettle(v);
            }
        });
    }

    private void postUnsettle(final View v) {
        post(new Runnable() {
            public void run() {
                onUnsettle(v);
            }
        });
    }

    private void slideViewOntoScreen(View v) {
        Point corr = getCorrection(getScrollBounds(v));
        if (corr.x != 0 || corr.y != 0) {
            mScroller.startScroll(0, 0, corr.x, corr.y, 400);
            mStepper.prod();
        }
    }

    private Point subScreenSizeOffset(View v) {
        return new Point(Math.max((getWidth() - v.getMeasuredWidth()) / 2, 0),
                Math.max((getHeight() - v.getMeasuredHeight()) / 2, 0));


    }

    private static int directionOfTravel(float vx, float vy) {
        if (Math.abs(vx) > 2 * Math.abs(vy))
            return (vx > 0) ? MOVING_RIGHT : MOVING_LEFT;
        else if (Math.abs(vy) > 2 * Math.abs(vx))
            return (vy > 0) ? MOVING_DOWN : MOVING_UP;
        else
            return MOVING_DIAGONALLY;
    }

    private static boolean withinBoundsInDirectionOfTravel(Rect bounds, float vx, float vy) {
        switch (directionOfTravel(vx, vy)) {
            case MOVING_DIAGONALLY:
                return bounds.contains(0, 0);
            case MOVING_LEFT:
                return bounds.left <= 0;
            case MOVING_RIGHT:
                return bounds.right >= 0;
            case MOVING_UP:
                return bounds.top <= 0;
            case MOVING_DOWN:
                return bounds.bottom >= 0;
            default:
                throw new NoSuchElementException();
        }
    }


    public void refreshView() {
        long downTime = SystemClock.uptimeMillis() + 200;
        long eventTime = SystemClock.uptimeMillis() + 210;
        float x = 1.0f;
        float y = 1.0f;
        int metaState = 0;

//        processTouchEvent(motionEvent, true);
    }


    public void redrawAll() {
        redrawPage(currentPage);
        if (mCurrent - 1 >= 0) {
            PageView prevPage = (PageView) mChildViews.get(mCurrent - 1);
            redrawPage(prevPage);
        }
        if (mCurrent + 1 < mChildViews.size()) {
            PageView posPage = (PageView) mChildViews.get(mCurrent + 1);
            redrawPage(posPage);
        }
    }

    private void redrawPage(PageView pageView) {
        if (pageView != null) {
            pageView.updateEntireCanvas(false);
            pageView.updateHq(true);
        }
    }

    private final float spaceScale = 0.2f;
    private final int secondDelay = 10;
    private MotionEvent currentEvent;
    private float currentScale;
    private boolean isZooming = false;
    private boolean isZoomIn = false;

    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isZoomIn) {
                //phong to
                currentScale = currentScale + spaceScale;
                if (currentScale >= MAX_SCALE) {
                    currentScale = MAX_SCALE;
                    zoomNew(currentEvent, currentScale);
                    if (currentPage != null) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
//                                requestLayout();
                                if (currentPage != null) {
                                    currentPage.updateHq(true);
                                }
                            }
                        }, 200);
                    }
                    isZooming = false;
                    handler.removeCallbacksAndMessages(null);
                    return;
                }
            } else {
                //thu nho
                currentScale = currentScale - spaceScale;
                if (currentScale <= MIN_SCALE) {
                    currentScale = MIN_SCALE;
                    zoomNew(currentEvent, currentScale);
                    isZooming = false;
                    handler.removeCallbacksAndMessages(null);
                    return;
                }
            }
            zoomNew(currentEvent, currentScale);
            handler.postDelayed(this, secondDelay);
        }
    };


    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        if (isZooming) return true;
        currentPage = (PageView) getDisplayedViewByTouch(e);
        isZooming = true;
        mViewModeCallback.showEditView(false);
        currentEvent = e;
        if (mScale < MAX_SCALE) {
            isZoomIn = true;
        } else {
            isZoomIn = false;
        }

        if (isZoomIn) {
            currentScale = mScale + spaceScale;
        } else {
            currentScale = mScale - spaceScale;
        }

        handler.post(runnable);
        return true;
    }

    private void zoomNew(MotionEvent e, float scaleNew) {
        Log.d("nam25", "zoomNew: " + scaleNew);

        float previousScale = mScale;
        mScale = scaleNew;
        float factor = mScale / previousScale;
        if (currentPage != null) {
            // Work out the focus point relative to the view top left
            int viewFocusX = (int) e.getX() - (currentPage.getLeft() + mXScroll);
            int viewFocusY = (int) e.getY() - (currentPage.getTop() + mYScroll);
            // Scroll to maintain the focus point
            mXScroll += viewFocusX - viewFocusX * factor;
            mYScroll += viewFocusY - viewFocusY * factor;
            requestLayout();

        }
    }


    @Override
    public void onLongPress(MotionEvent e) {
        if (currentPage != null) {
//            currentPage.onLongPress(e, mScale);
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    public DigitalizedEventCallback getEventCallback() {
        DigitalizedEventCallback result = null;
        if (currentPage != null) {
            result = currentPage.getEventCallback();
        }
        return result;
    }

    public void setEventCallback(DigitalizedEventCallback eventCallback) {
        this.eventCallback = eventCallback;
        if (currentPage != null) {
            currentPage.setEventCallback(eventCallback);
        }
    }
}
