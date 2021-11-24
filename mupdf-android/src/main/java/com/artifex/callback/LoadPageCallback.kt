package com.artifex.callback

import android.graphics.Bitmap

public interface LoadPageCallback {
    fun onLoadComplete(bimap: Bitmap?)
}