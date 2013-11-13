package org.telegram.android.video;

import android.hardware.Camera;
import android.os.Build;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 22.09.13
 * Time: 3:26
 */
public class CameraHolder {

    private static Camera.Size getBestPreviewSize(int width, int height,
                                                  Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

    private static final int MAX_W = 720;
    private static final int MAX_H = 480;

    private Camera camera;

    private Orientation orientation;

    private Camera.Size size;

    private float surfaceRatio;

    public CameraHolder(Orientation baseOrientation) {
        this.orientation = baseOrientation;
    }

    public void createCamera() {
        if (camera != null)
            return;
        camera = Camera.open();

        int maxW;
        int maxH;

        switch (orientation) {
            default:
            case PORTRAIT:
            case PORTRAIT_REVERSE:
                maxW = MAX_H;
                maxH = MAX_W;
                break;
            case LANDSCAPE:
            case LANDSCAPE_REVERSE:
                maxW = MAX_W;
                maxH = MAX_H;
                break;
        }

        size = getBestPreviewSize(
                maxW,
                maxH,
                camera.getParameters());

        switch (orientation) {
            default:
            case PORTRAIT:
            case PORTRAIT_REVERSE:
                surfaceRatio = size.width / (float) size.height;
                break;
            case LANDSCAPE:
            case LANDSCAPE_REVERSE:
                surfaceRatio = size.height / (float) size.width;
                break;
        }

        Camera.Parameters parameters = camera.getParameters();
        if (Build.VERSION.SDK_INT >= 14) {
            parameters.setRecordingHint(true);
        }
        if (Build.VERSION.SDK_INT >= 15) {
            parameters.setVideoStabilization(true);
        }
        parameters.setPreviewSize(size.width, size.height);

        List<String> focusModes = parameters.getSupportedFocusModes();
        for (int i = 0; i < focusModes.size(); i++) {
            if (focusModes.get(i).equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setWhiteBalance(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                break;
            }
        }

        List<String> whitelist = parameters.getSupportedWhiteBalance();
        for (int i = 0; i < whitelist.size(); i++) {
            if (whitelist.get(i).equals(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                break;
            }
        }

        camera.setParameters(parameters);

        int previewAngle;
        switch (orientation) {
            default:
            case PORTRAIT:
                previewAngle = 90;
                break;
            case PORTRAIT_REVERSE:
                previewAngle = 270;
                break;
            case LANDSCAPE:
                previewAngle = 0;
                break;
            case LANDSCAPE_REVERSE:
                previewAngle = 180;
                break;
        }

        camera.setDisplayOrientation(previewAngle);
    }

    public float getSurfaceRatio() {
        return surfaceRatio;
    }

    public void closeCamera() {
        if (camera == null)
            return;

        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.unlock();
        camera.release();
        camera = null;
    }

    public int getWidth() {
        return size.width;
    }

    public int getHeight() {
        return size.height;
    }

    public Camera getCamera() {
        return camera;
    }
}
