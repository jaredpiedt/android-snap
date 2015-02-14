package co.tagalong.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import co.tagalong.ui.util.UIUtils;

/**
 * Created by piedt on 2/7/15.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Context mContext;
    private Camera mCamera;

    public CameraPreview(Context context) {
        super(context);

        mContext = context;
        //mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if(mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = params.getSupportedPreviewSizes();

            UIUtils.hideStatusBar(mContext);
            Camera.Size optimalSize = getBestPreviewSize(this.getWidth(), this.getHeight(), mSupportedPreviewSizes);
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            profile.videoFrameWidth = optimalSize.width;
            profile.videoFrameHeight = optimalSize.height;

            params.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);

            List<String> focusModes = params.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }


            Method downPolymorphic;
            try {
                downPolymorphic = mCamera.getClass().getMethod("setDisplayOrientation", new Class[]
                        {int.class});
            } catch(Exception e) {

            }

//            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
//            {
//                params.set("orientation", "portrait");
//                params.set("rotation", 90);
//                Log.d(TAG, "portrait rotating 90");
//            }
//            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
//            {
//                params.set("orientation", "landscape");
//                params.set("rotation", 90);
//                Log.d(TAG, "landscape rotating 90");
//
//            }
            // set Camera parameters
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90);
        }
    }

    public void setFlash(boolean flash) {
        if(flash) {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(params);
        } else {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(params);
        }
    }

    public void refreshCamera() {
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch(Exception e) {
            Log.d(TAG, "Error setting preview display");
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){
        Log.d(TAG, "enter surfaceChanged()");

        UIUtils.hideStatusBar(mContext);
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager)mContext.getSystemService(mContext.WINDOW_SERVICE)).getDefaultDisplay();
        Point s = new Point();
        display.getSize(s);

        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        if(display.getRotation() == Surface.ROTATION_0); {
            parameters.setPreviewSize(s.x, s.y);
            Log.d(TAG, "parameters set: " + s.x + " + " + s.y);

            mCamera.setDisplayOrientation(90);
        }

        if(display.getRotation() == Surface.ROTATION_90){
            parameters.setPreviewSize(s.x, s.y);
        }

        if(display.getRotation() == Surface.ROTATION_180) {
            parameters.setPreviewSize(s.x, s.y);
        }

        if(display.getRotation() == Surface.ROTATION_270) {
            parameters.setPreviewSize(s.x, s.y);
            mCamera.setDisplayOrientation(180);
        }

        // mCamera.setParameters(parameters);
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Enter surfaceCreated()");
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Enter surfaceDestroyed()");
        // Stop the preview
        if(mCamera != null) {
            mCamera.stopPreview();
            // mCamera.release();
            Log.d(TAG, "Camera preview was stopped");
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height, List<Camera.Size> previewSizes) {
        Camera.Size result = null;

        UIUtils.hideStatusBar(mContext);
        Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
        Point s = new Point();
        display.getSize(s);

        int screenWidth = s.x;
        int screenHeight = s.y;

        Log.d(TAG, "Screen size = " + screenWidth + " x " + screenHeight);
        Log.d(TAG, "View size = " + width + " x " + height);
        for(Camera.Size size : previewSizes){
            if (size.width<=screenHeight && size.height<=screenWidth) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        Log.d(TAG, "Size chosen = " + result.width + " x " + result.height);
        return result;
    }


}
