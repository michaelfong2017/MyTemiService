/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.robocore.secretcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraService implements ImageReader.OnImageAvailableListener {

    private static final String TAG = CameraService.class.getSimpleName();

    private static CameraService cameraService;

    // Prevent clients from using the constructor
    private CameraService() {
    }

    //Control the accessible (allowed) instances
    public static CameraService getInstance() {
        Log.d(TAG, "getInstance()");
        if (cameraService == null) {
            cameraService = new CameraService();
        }
        return cameraService;
    }

    private Context context;
    private CameraManager cameraManager;

    public void initialize(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public static void clear() {
        cameraService = null;
    }


    private boolean isProcessingFrame = false;

    /**
     * data
     */
    private static byte[] imageBytes;


    /**
     * public functions
     */
    public void setCamera(String cameraID) { // For Temi, "0" for normal camera, "1' for fish eye camera.
        this.cameraID = cameraID;
    }

    private OnImageProcessedListener onImageProcessedListener;

    public void setOnImageProcessedListener(OnImageProcessedListener onImageProcessedListener) {
        this.onImageProcessedListener = onImageProcessedListener;
    }

    public void startSecretCamera() {
        setHasPreview(false);
        isProcessingFrame = false;

        startBackgroundThread();
        openCamera(720, 480);
    }

    public void closeSecretCamera() {
        closeCamera();
        stopBackgroundThread();
    }


    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);


    /**
     * ID of the current {@link CameraDevice}.
     */
    private String cameraID;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession captureSession;
    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice cameraDevice;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;
    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler;
    /**
     * An {@link ImageReader} that handles preview frame capture.
     */
    private ImageReader previewReader;
    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder previewRequestBuilder;
    /**
     * {@link CaptureRequest} generated by {@link #previewRequestBuilder}
     */
    private CaptureRequest previewRequest;

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            Log.d(TAG, "onCaptureFailed()");
            super.onCaptureFailed(session, request, failure);
        }
    };

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(final CameraDevice cd) {
                    Log.d(TAG, "onOpened");
                    // This method is called when the camera is opened.  We start camera preview here.
                    cameraOpenCloseLock.release();
                    cameraDevice = cd;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(final CameraDevice cd) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(final CameraDevice cd, final int error) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                }
            };


    /**
     * Camera preview
     */
    private boolean hasPreview;
    private void setHasPreview(boolean hasPreview) {
        this.hasPreview = hasPreview;
    }

    /**
     * The camera preview size will be chosen to be the smallest frame by pixel size capable of
     * containing a DESIRED_SIZE x DESIRED_SIZE square.
     */
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView textureView;
    private void setTextureView(AutoFitTextureView textureView) {
        this.textureView = textureView;
    }

    /** The rotation in degrees of the camera sensor from the display. */
    private Integer sensorOrientation;
    /**
     * The {@link Size} of camera preview.
     */
    private Size previewSize;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
     * TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    Log.d(TAG, "onSurfaceTextureAvailable()");
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                    configureTransform(width, height);
                    Log.d(TAG, "onSurfaceTextureSizeChanged()");
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    Log.d(TAG, "onSurfaceTextureDestroyed()");
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
                    Log.d(TAG, "onSurfaceTextureUpdated()");
                }
            };

    public void startPreviewCamera(AutoFitTextureView textureView) {
        Log.d(TAG, "startPreviewCamera()");
        setHasPreview(true);
        isProcessingFrame = false;

        setTextureView(textureView);

        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the minimum of both, or an exact match if possible.
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width   The minimum desired width
     * @param height  The minimum desired height
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        Log.d(TAG, "Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
        Log.d(TAG, "Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
        Log.d(TAG, "Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");

        if (exactSizeFound) {
            Log.d(TAG, "Exact size match found.");
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            Log.d(TAG, "Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            Log.d(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
    /** Sets up member variables related to camera. */
    private void setUpCameraOutputs() {
        try {
            final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);

            final StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            previewSize =
                    chooseOptimalSize(
                            map.getOutputSizes(SurfaceTexture.class),
                            1920,
                            1080);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            final int orientation = context.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        } catch (final CameraAccessException e) {
            Log.e(TAG, "CameraAccessException!");
        } catch (final NullPointerException e) {
            Log.e(TAG, "NullPointerException!");
        }

//    cameraConnectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation);
    }
    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`. This method should be
     * called after the camera preview size is determined in setUpCameraOutputs and also the size of
     * `mTextureView` is fixed.
     *
     * @param viewWidth The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(final int viewWidth, final int viewHeight) {
        final Activity activity = (Activity)context;
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }
    /** Compares two {@code Size}s based on their areas. */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Camera preview end
     */


    /**
     * Opens the camera specified by {@link CameraService#cameraID}.
     */
    private void openCamera(final int width, final int height) {
        if (hasPreview) {
            setUpCameraOutputs();
            configureTransform(width, height);
        }

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(cameraID, stateCallback, backgroundHandler);
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Exception!");
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }


    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (previewReader != null) {
                previewReader.close();
                previewReader = null;
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (backgroundThread == null) {
            return;
        }
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, "Exception!");
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            Surface surface = null;
            if (hasPreview) {
                /** Preview
                 *
                 */

                final SurfaceTexture texture = textureView.getSurfaceTexture();
                assert texture != null;

                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

                // This is the output Surface we need to start preview.
                surface = new Surface(texture);

                // We set up a CaptureRequest.Builder with the output Surface.
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(surface);

                Log.d(TAG, "Opening camera preview: " + previewSize.getWidth() + "x" + previewSize.getHeight());
            }

            /** Create the reader for the preview frames.
             *
             */
            if (null == cameraDevice) {
                Log.e(TAG, "cameraDevice is null");
                return;
            }
            previewReader = ImageReader.newInstance(720, 480, ImageFormat.YUV_420_888, 2);
            previewReader.setOnImageAvailableListener(this, backgroundHandler);
            /** Preview
             *
             */
            if (!hasPreview) {
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            }

            previewRequestBuilder.addTarget(previewReader.getSurface());

            final List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(previewReader.getSurface());


            /** Preview
             *
             */
            if (surface != null) {
                outputSurfaces.add(surface);
            }

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(
                    outputSurfaces,
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (final CameraAccessException e) {
                                Log.e(TAG, "Exception!");
                            }
                        }

                        @Override
                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                        }
                    },
                    null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Log.d(TAG, "onImageAvailable()");

        /**
         * YUV_420_888 to RGB
         */
        // Get the YUV data

        final Image image = reader.acquireLatestImage();

        if (image == null) {
//            Log.d(TAG, "null image");
            return;
        }
        if (isProcessingFrame == true) {
            image.close();
            return;
        }
        isProcessingFrame = true;

//        Log.d(TAG, "now process frame");

        // Convert YUV to RGB
//        long startTime = System.nanoTime();
        imageBytes = ImageUtil.imageToByteArray(image);
//        long endTime = System.nanoTime();
//        Log.d(TAG, "convert takes "+(endTime-startTime)+" nanoseconds");
//        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
//        Log.d(TAG, "encodedImage: " + encodedImage);
//        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
//        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//        Log.d(TAG, "decodedByte: " + decodedByte);

//        Log.d(TAG, "image now close");
        image.close();
        isProcessingFrame = false;

        onImageProcessedListener.onImageProcessed(imageBytes);
    }
}
