package com.artifex.mupdf.fitz.utils;

import android.os.AsyncTask;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class PhotorThread {
  private static PhotorThread ThreadUtils;
  private List<AsyncTask> listTasks;

  public PhotorThread() {
    listTasks = new ArrayList<>();
  }

  public static PhotorThread getInstance() {
    if (ThreadUtils == null) {
      ThreadUtils = new PhotorThread();
    }

    return ThreadUtils;
  }

  public DoJobBackground runBackground(IBackground listener) {
    DoJobBackground executor = new DoJobBackground();
    executor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, listener);
    listTasks.add(executor);// Cached
    return executor;
  }

  public void removeAllBackgroundThreads() {
    if (listTasks != null && !listTasks.isEmpty()) {
      for (AsyncTask task : listTasks) {
        if (!task.isCancelled() && task.getStatus().equals(AsyncTask.Status.RUNNING)) {
          task.cancel(true);
        }
      }
      listTasks.clear();
    }
    listTasks = null;
    ThreadUtils = null;
  }

  public Handler runOnUI(final IHandler mIHandler) {
    Handler handler = new Handler();
    handler.post(new Runnable() {
      @Override public void run() {
        if (mIHandler != null) {
          mIHandler.onWork();
        }
      }
    });
    return handler;
  }

  public Handler runUIDelay(final IHandler mIHandler, final int timeDelay) {
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {

      @Override public void run() {
        if (mIHandler != null) {
          mIHandler.onWork();
        }
      }
    }, timeDelay);
    return handler;
  }

  public interface IHandler {
    void onWork();
  }

  public interface IHandlerExtra extends IHandler {
    void onLoadFailed();
  }

  public interface IBackground {
    void doingBackground();

    void onCompleted();

    void onCancel();
  }

  public class DoJobBackground extends AsyncTask<IBackground, Void, Void> {

    private IBackground mListener;

    @Override protected Void doInBackground(IBackground... params) {
      mListener = params[0];
      if (mListener != null) {
        mListener.doingBackground();
      }
      return null;
    }

    @Override protected void onCancelled() {
      super.onCancelled();
      if (mListener != null) {
        mListener.onCancel();
      }
    }

    @Override protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (mListener != null) {
        mListener.onCompleted();
      }
    }
  }
}
