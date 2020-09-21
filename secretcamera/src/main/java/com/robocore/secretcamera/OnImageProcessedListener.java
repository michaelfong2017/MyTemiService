package com.robocore.secretcamera;

import android.graphics.Bitmap;

public interface OnImageProcessedListener {
    void onImageProcessed(byte[] imageBytes);
}
