package com.artifex.mupdfdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.artifex.callback.SingleClickCallback;
import com.artifex.menu.MyPopupMenu;

import java.util.ArrayList;

public class MuPDFReaderView extends ReaderView {
    public enum Mode {Viewing, Selecting, Drawing, Editing}

    private final Context mContext;
    private boolean mLinksEnabled = false;

    private boolean tapDisabled = false;
    private int tapPageMargin;
    private final boolean TAP_PAGING_ENABLED = false;

    protected void onTapMainDocArea() {
    }

    protected void onDocMotion() {
    }

    protected void onHit(Hit item) {

    }

    ;

    public void setLinksEnabled(boolean b) {
        mLinksEnabled = b;
        resetupChildren();
    }

    public void setMode(Mode m) {
        mMode = m;
    }


    private void setup() {
        // Get the screen size etc to customise tap margins.
        // We calculate the size of 1 inch of the screen for tapping.
        // On some devices the dpi values returned are wrong, so we
        // sanity check it: we first restrict it so that we are never
        // less than 100 pixels (the smallest Android device screen
        // dimension I've seen is 480 pixels or so). Then we check
        // to ensure we are never more than 1/5 of the screen width.
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        tapPageMargin = (int) dm.xdpi;
        if (tapPageMargin < 100)
            tapPageMargin = 100;
        if (tapPageMargin > dm.widthPixels / 5)
            tapPageMargin = dm.widthPixels / 5;
    }

    public MuPDFReaderView(Context context) {
        super(context);
        mContext = context;
        setup();
    }

    public MuPDFReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setup();
    }

    private CallbackEdit callbackEdit = new CallbackEdit() {
        @Override
        public void edited() {

        }
    };
    private boolean isEdit2 = true;

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        if (isZoom) {
            isZoom = false;
            return super.onSingleTapConfirmed(e);
        }
        if (isLongClicked) return super.onSingleTapConfirmed(e);

//        final MuPDFView pageView = (MuPDFView) getDisplayedView();
        final MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(e);
        MyPopupMenu.getInstance(getContext()).hideMenu();
        if (!tapDisabled && pageView != null) {
            pageView.passClickEvent(e.getX(), e.getY(), new SingleClickCallback() {
                @Override
                public void onClickItem(Hit item) {
                    Log.d("nam4", "onClickItem: " + item.name());
                    if (item == Hit.Nothing) {
                        allowsPopupMenu = false;
                        PageView pageView1 = (PageView) getDisplayedView();
                        if (pageView1 != null) {
                            pageView1.setItemSelectBox(null);
                        }
                        isEdit2 = !isEdit2;
                        pageView.setModeEdit(mEditing);
                        if (mViewModeCallback != null) {
                            mViewModeCallback.showEditView(isEdit2);
                        }
                    } else {
                        // click vao anotation
                        allowsPopupMenu = false;
                        if (mModeAuto == ModeAuto.NORMAL) {
                            if (mPopupMenuView != null) {
                                mPopupMenuView.dismiss();
                            }
                            pageView.showPopupMenuDelete(e);
                            invalidate();
                        }
                    }


//                    if (mMode == Mode.Viewing && !tapDisabled) {
//                        if (item == Hit.Nothing) {
//                            if (TAP_PAGING_ENABLED && e.getX() < tapPageMargin) {
//                                MuPDFReaderView.super.smartMoveBackwards();
//                            } else if (TAP_PAGING_ENABLED && e.getX() > MuPDFReaderView.super.getWidth() - tapPageMargin) {
//                                MuPDFReaderView.super.smartMoveForwards();
//                            } else if (TAP_PAGING_ENABLED && e.getY() < tapPageMargin) {
//                                MuPDFReaderView.super.smartMoveBackwards();
//                            } else if (TAP_PAGING_ENABLED && e.getY() > MuPDFReaderView.super.getHeight() - tapPageMargin) {
//                                MuPDFReaderView.super.smartMoveForwards();
//                            } else {
//                                onTapMainDocArea();
//                            }
//                        }
//                    }


                }
            });

//            mMode = Mode.Viewing;
//            if (pageView != null) {
//                pageView.setModeEdit(mEditing);
//                pageView.deselectText();
//            }

        }
        return super.onSingleTapConfirmed(e);
    }

    private boolean isZoom = false;

    private static final String TAG = "MuPDFReaderView111";

    @Override
    public boolean onDown(MotionEvent e) {
        return super.onDown(e);
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
//        MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(e1);
        MuPDFView pageView = (MuPDFView) getDisplayedView();
        switch (mMode) {
            case Viewing:
                if (!tapDisabled)
                    onDocMotion();

                return super.onScroll(e1, e2, distanceX, distanceY);
            case Selecting:
                if (isOnePointCount && mEditing && pageView != null && e2.getPointerCount() == 1 && !viewTemple && !isLongClicked) {
                    if (mModeAuto == ModeAuto.AUTO_DELETE) {
                        pageView.autoDelete(e2.getX(), e2.getY());
                    } else {
//                        pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
                    }
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        switch (mMode) {
            case Viewing:
                return super.onFling(e1, e2, velocityX, velocityY);
            default:
                return true;
        }
    }

    public boolean onScaleBegin(ScaleGestureDetector d) {
        // Disabled showing the menu_pdf until next touch.
        // Not sure why this is needed, but without it
        // pinch zoom can make the menu_pdf appear
        tapDisabled = true;
        return super.onScaleBegin(d);
    }

    private float downX, downY;
    private boolean check = false;
    private boolean viewTemple = false;


    @Override
    public void onLongPress(MotionEvent e) {
        Log.d("longPress", "onLongPress: ");
        if (mEditing && !isLongClicked) {
            try {
                isLongClicked = true;
                mMode = MuPDFReaderView.Mode.Selecting;
//                MuPDFView pageView = (MuPDFView) getDisplayedView();
                MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(e);
                if (mModeAuto != ModeAuto.AUTO_DELETE) {
                    actionDeselectAllFocusText();
                    currentDownPage = getPageByTouch((int) e.getY());
                    lastMovePage = currentDownPage;
                    currentMovePage = currentDownPage;
                    // long click 1 tu
                    pageView.longSelectText(e.getX(), e.getY(), e.getX(), e.getY());
                }
            } catch (Exception exception) {

            }


        }
        super.onLongPress(e);
    }

    // trang dau tien khi touch
    private int currentDownPage = 0;
    private int currentMovePage = 0;
    private int lastMovePage = 0;

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            check = true;
            isLongClicked = false;
            viewTemple = false;
//            allowsPopupMenu = true;

            MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(event);
            currentDownPage = mCurrentViewTouch;
            lastMovePage = currentDownPage;
            currentMovePage = currentDownPage;
            if (pageView != null && event.getPointerCount() == 1 && isOnePointCount) {

                PageView.HIGHLIGHT_MODE mode = pageView.selectTextOnDown(event.getX(), event.getY());
                if (mode == PageView.HIGHLIGHT_MODE.NORMAL) {
                    mMode = Mode.Viewing;
                    viewTemple = mModeAuto != ModeAuto.AUTO_DELETE;
                } else if (mEditing) {
                    isLongClicked = true;
                    mMode = Mode.Selecting;
                }
                if (mEditing && mModeAuto == ModeAuto.AUTO_DELETE) {
//                    pageView.deselectText();
                    mMode = Mode.Selecting;
                }

            } else {
                mMode = Mode.Viewing;
            }
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(event);
            if (pageView != null)
                pageView.updatePosition(event.getX(), event.getY());
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE && check && event.getPointerCount() == 1) {
            if ((Math.abs(event.getX() - downX) < 30) && Math.abs(event.getY() - downY) < 30) {
                if (event.getEventTime() - event.getDownTime() > 200 && mEditing && !isLongClicked && mModeAuto != ModeAuto.AUTO_DELETE) {
                    isLongClicked = true;
                    mMode = MuPDFReaderView.Mode.Selecting;
//                    MuPDFView pageView = (MuPDFView) getDisplayedView();
                    MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(event);
                    if (pageView != null) {
                        actionDeselectAllFocusText();
                        // cham va giu de boi den
                        pageView.longSelectText(event.getX(), event.getY(), event.getX(), event.getY());
                    }
                    check = false;
                }
            } else if (viewTemple) {
                mMode = Mode.Viewing;
                viewTemple = false;
                check = false;
            }
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE && isLongClicked && event.getEventTime() - event.getDownTime() > 300 && !((Math.abs(event.getX() - downX) < 30) && Math.abs(event.getY() - downY) < 30)) {
            MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(event);
            if (isOnePointCount && mEditing) {
                if (pageView != null) {
                    if (mModeAuto == ModeAuto.AUTO_DELETE) {
//                        pageView.autoDelete(event.getX(), event.getY());
                    } else {
                        currentMovePage = getPageByTouch((int) event.getY());
                        if (currentMovePage != lastMovePage) {
                            // stop
                            long downTime = SystemClock.uptimeMillis();
                            long eventTime = SystemClock.uptimeMillis() + 100;
                            MotionEvent motionEvent = MotionEvent.obtain(
                                    downTime,
                                    eventTime,
                                    MotionEvent.ACTION_UP,
                                    event.getX(),
                                    event.getY() - 500,
                                    0
                            );
                            dispatchTouchEvent(motionEvent);
                        } else {
                            pageView.selectText(downX, downY, event.getX(), event.getY());
                        }
                        lastMovePage = currentMovePage;
                    }
                }
            }
        }
        if (mMode == Mode.Drawing) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    break;
            }
        }

        if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN) {
            tapDisabled = false;
        }
        if ((event.getAction() == MotionEvent.ACTION_UP)) {
            MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch(event);
//            if (isLongClicked && !mPopupMenuView.isShowing() && allowsPopupMenu && mModeAuto == ModeAuto.NORMAL) {
            if (isLongClicked && !mPopupMenuView.isShowing()  && mModeAuto == ModeAuto.NORMAL) {
                if (pageView != null)
                    pageView.deselectAnnotation();
                currentPage = (PageView) getDisplayedViewByTouch(event);
                if (currentPage != null) {
                    ArrayList<RectF> list = currentPage.getListOldRect();
                    if (list != null && list.size() > 0) {
                        showPopupMenu((int) event.getX() + 50, (int) event.getY());
                    }
                }

            } else if (isLongClicked) {
                if (pageView != null) {
                    switch (mModeAuto) {
                        case AUTO_HIGHLIGHT:
                            pageView.markupSelection(Annotation.Type.HIGHLIGHT);
                            break;
                        case AUTO_UNDERLINE:
                            pageView.markupSelection(Annotation.Type.UNDERLINE);
                            break;
                        case AUTO_STRIKE_THROUGH:
                            pageView.markupSelection(Annotation.Type.STRIKEOUT);
                            break;
                    }
                }
            }
            uX = event.getX();
            uY = event.getY();
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 450);

            if (pageView != null) {
                pageView.touchUp();
            }
        }
        return super.onTouchEvent(event);
    }

    private int getPageByTouch(int y) {
        for (int i = 0; i < mChildViews.size(); i++) {
            int key = mChildViews.keyAt(i);
            View v = mChildViews.get(key);
            if (y > v.getTop() && y < v.getBottom()) {
                return key;
            }
        }
        return 0;
    }

    private float mX, mY, uX, uY;

    private static final float TOUCH_TOLERANCE = 2;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            MuPDFView pageView = (MuPDFView) getDisplayedView();
            try {
                if (mModeAuto == ModeAuto.NORMAL && allowsPopupMenu && pageView.checkClickMenu(null)) {
                    MyPopupMenu.getInstance(getContext()).showMenu(uX + 50, uY);
                    invalidate();
                }
            } catch (Exception e) {
//                Toast.makeText(getContext(), "  have error", Toast.LENGTH_SHORT).show();
            }

        }
    };

    // bo boi dat tat ca cac trang trc do
    private void actionDeselectAllFocusText() {
        for (int i = 0; i < mChildViews.size(); i++) {
            int key = mChildViews.keyAt(i);
            PageView v = (PageView) mChildViews.get(key);
            if (v != null) {
                v.deselectText();
            }
        }
    }

    private void touch_start(float x, float y) {

//        MuPDFView pageView = (MuPDFView) getDisplayedView();
        MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch((int) y);
        if (pageView != null) {
            pageView.startDraw(x, y);
        }
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {

        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
//            MuPDFView pageView = (MuPDFView) getDisplayedView();
            MuPDFView pageView = (MuPDFView) getDisplayedViewByTouch((int) y);
            if (pageView != null) {
                pageView.continueDraw(x, y);
            }
            mX = x;
            mY = y;
        }
    }

    private void touch_up() {


        // NOOP
    }

    private RectF currentRect;

    protected void onChildSetup(int i, View v) {
        if (SearchTaskResult.get() != null
                && SearchTaskResult.get().pageNumber == i) {
            ((MuPDFView) v).setSearchRect(currentRect);
            ((MuPDFView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
        } else
            ((MuPDFView) v).setSearchBoxes(null);

        ((MuPDFView) v).setLinkHighlighting(mLinksEnabled);

        ((MuPDFView) v).setChangeReporter(new Runnable() {
            public void run() {
                applyToChildren(new ReaderView.ViewMapper() {
                    @Override
                    public void applyToView(View view) {
                        ((MuPDFView) view).update();
                    }
                });
            }
        });
    }

    protected void onMoveToChild(int i) {
        if (SearchTaskResult.get() != null
                && SearchTaskResult.get().pageNumber != i) {
            SearchTaskResult.set(null);
            resetupChildren();
        }
    }

    @Override
    protected void onMoveOffChild(int i) {
        View v = getView(i);
        if (v != null)
            ((MuPDFView) v).deselectAnnotation();
    }

    protected void onSettle(View v) {
        // When the layout has settled ask the page to render
        // in HQ
//        ((MuPDFView) v).updateHq(false);
    }

    protected void onUnsettle(View v) {
        // When something changes making the previous settled view
        // no longer appropriate, tell the page to remove HQ
        ((MuPDFView) v).removeHq();
    }

    @Override
    protected void onNotInUse(View v) {
        ((MuPDFView) v).releaseResources();
    }

    @Override
    protected void onScaleChild(View v, Float scale) {
        ((MuPDFView) v).setScale(scale);
    }

    public void setModeEdit(boolean isEditMode) {
        mEditing = isEditMode;
//        MuPDFView pageView = (MuPDFView) getDisplayedView();
//        if (pageView != null) {
//            pageView.setModeEdit(mEditing);
//        }
    }
}
