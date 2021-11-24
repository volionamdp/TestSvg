package com.artifex.mupdfdemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;

import com.artifex.utils.PdfBitmap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MuPDFPageAdapter extends BaseAdapter {
    private final Context mContext;
    private final FilePicker.FilePickerSupport mFilePickerSupport;
    private final MuPDFCore mCore;
    private final SparseArray<PointF> mPageSizes = new SparseArray<>(5);
    private SparseArray<MuPDFPageView> pages = new SparseArray<>(5);
    //    private Collection<PdfBitmap> pdfBitmapList; // Each signature for each page.
    private boolean isEdited = false;

    public MuPDFPageAdapter(Context c, FilePicker.FilePickerSupport filePickerSupport, MuPDFCore core) {
        mContext = c;
        mFilePickerSupport = filePickerSupport;
        mCore = core;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public int getCount() {
        return mCore.countPages();
    }

    public Object getItem(int position) {
        return pages.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

//    private int position = 0;

    public View getView(final int position, View convertView, ViewGroup parent) {
        final MuPDFPageView pageView;

        if (pages.get(position) == null) {
            pageView = new MuPDFPageView(mContext, mFilePickerSupport, mCore, new Point(parent.getWidth(), parent.getHeight()), this);
            pages.put(position, pageView);
            pageView.setCallbackEdit(new CallbackEdit() {
                @Override
                public void edited() {
                    isEdited = true;
                }
            });
        } else {
            pageView = pages.get(position);
        }

//        pageView = new MuPDFPageView(mContext, mFilePickerSupport, mCore, new Point(parent.getWidth(), parent.getHeight()), this);
//        pages.put(position, pageView);
        pageView.setCallbackEdit(new CallbackEdit() {
            @Override
            public void edited() {
                isEdited = true;
            }
        });


        Log.d("dsk7", "position: " + position);
        //Limit the pages cache to improve memory usage
        if (pages.size() > 3) {
            if (position > 1) {
                MuPDFPageView previous1 = pages.get(position - 3);
                if (previous1 != null) {
                    pages.removeAt(pages.indexOfValue(previous1));
                }


                MuPDFPageView previous = pages.get(position - 2);
                if (previous != null) {
                    pages.removeAt(pages.indexOfValue(previous));
                }
                MuPDFPageView post = pages.get(position + 2);
                if (post != null) {
                    pages.removeAt(pages.indexOfValue(post));
                }

                MuPDFPageView previous2 = pages.get(position + 3);
                if (previous2 != null) {
                    pages.removeAt(pages.indexOfValue(previous2));
                }
            }
        }

        PointF pageSize = mPageSizes.get(position);
//        pageView.setPage(position, new PointF(1,1));
        if (pageSize != null) {
            // We already know the page size. Set it up
            // immediately
            pageView.setPage(position, pageSize);
        } else {
            // Page size as yet unknown. Blank it for now, and
            // start a background task to find the size
            pageView.blank(position);
            final long time = System.currentTimeMillis();
            AsyncTask<Void, Void, PointF> sizingTask = new AsyncTask<Void, Void, PointF>() {
                @Override
                protected PointF doInBackground(Void... arg0) {
                    return mCore.getPageSize(position);
                }

                @Override
                protected void onPostExecute(PointF result) {
                    super.onPostExecute(result);
                    // We now know the page size
                    Log.d("setPageSize", "onPostExecute: " + (System.currentTimeMillis() - time));
                    mPageSizes.put(position, result);
                    // Check that this view hasn't been reused for
                    // another page since we started
                    if (pageView.getPage() == position)
                        pageView.setPage(position, result);
                }
            };

            sizingTask.execute((Void) null);
        }
        return pageView;
    }
}