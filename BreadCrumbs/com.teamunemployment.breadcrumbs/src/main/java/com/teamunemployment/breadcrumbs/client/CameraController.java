package com.teamunemployment.breadcrumbs.client;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.Releasable;
import com.teamunemployment.breadcrumbs.PreferencesAPI;
import com.teamunemployment.breadcrumbs.R;
import com.teamunemployment.breadcrumbs.caching.GlobalContainer;
import com.teamunemployment.breadcrumbs.client.Animations.SimpleAnimations;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Policy;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Boolean isPreviewRunning = false;
    private Activity context;
    private boolean backCameraOpen = true;
    private boolean video = false; // by default
    private MediaRecorder recorder;
    private boolean currentlyFilming = false;
    private String fileName;
    private int cameraWidth;
    private int cameraHeight;

    private int videoTimer = 0;
    private final static int FRONT_FACING_CAM = 0;
    private final static int SELFIE_CAM = 1;
    private final static int VIDEO = 1;
    private final static int PHOTO = 0;
    private int CAMERA_MODE = VIDEO;

    private Timer t;
    /*
        Default constructors for a custom surfaceView.
     */
    public CameraController(Context context) {
        super(context);
        getHolder().addCallback(this);
        this.context = (Activity) context;
        videoTimer= 0;
    }

    public CameraController(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        getHolder().addCallback(this);
        this.context = (Activity) context;
        videoTimer= 0;

    }

    public CameraController(Context context, AttributeSet attrs, int defStyle)  {
        super(context, attrs, defStyle);
        getHolder().addCallback(this);
        this.context = (Activity) context;
        videoTimer= 0;
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Set the height of the camera to be the same as the width so we get a square.
        mHolder = holder;
        videoTimer = 0;
        if (backCameraOpen) {
            mCamera = Camera.open(0); // Open rear facing by default
        }
        else {
            mCamera = Camera.open(1);
        }
        Display display = ((WindowManager)context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
        int raot = display.getRotation();
        mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size size = getOptimalPreviewSize(parameters.getSupportedPictureSizes());
        if (parameters.getFocusMode() == null || parameters.getFocusMode().equals("auto")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        //parameters.setFocusMode(Camera.Parameters.);

        // Store this for later reference
        cameraHeight = size.height;
        cameraWidth = size.width;
        parameters.setPictureSize(size.width, size.height);

        mCamera.setParameters(parameters);

        CheckOrientationIsNotAllFuckingRetarded(parameters, display);
        try {

            mCamera.setPreviewDisplay(holder);
            // mCamera.setDisplayOrientation(90);
            SetupCameraButtonListener();
            setUpSwitchCameraListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    For some reason every phone i have tested this on, android sets the default preview to be sideways.
    Even when activity is limited to portrait.
    Why the fuck this is ever a thing i will never know.
     */
    private void CheckOrientationIsNotAllFuckingRetarded(Camera.Parameters parameters, Display display) {
        if (display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
        }
    }

    private void setUpSwitchCameraListener() {
        //If we are back camera, change to front camera.
        ImageButton switchButton = (ImageButton) context.findViewById(R.id.font_or_back_camera);
        switchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backCameraOpen) {
                    SetFrontCamera();
                } else {
                    OpenBackCamera();
                }
            }
        });

        //otherwise, if we are front camera change to back
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        if (isPreviewRunning) {
            mCamera.stopPreview();
            isPreviewRunning = false;
        }
        mCamera.release();
        mCamera = null; // This could cause issues.
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (!isPreviewRunning) {
            mCamera.startPreview();
            try {
                Display display = ((WindowManager)context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
                Camera.Parameters parameters = mCamera.getParameters();
                Camera.Size size = getOptimalPreviewSize(parameters.getSupportedPictureSizes());
                cameraHeight = size.height;
                cameraWidth = size.width;
                if (parameters.getFocusMode() == null) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }

                parameters.setPictureSize(size.width, size.height);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(holder);
                CheckOrientationIsNotAllFuckingRetarded(parameters, display);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes) {
        // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
        Camera.Size optimalSize = null;

        // Try to find a size match which suits the whole screen minus the menu on the left.
        for (Camera.Size size : sizes) {
            Log.d("CAM", "Checking size: width = " + size.width + ", Height = "+size.height);
            if (size.height <1000 && size.height >= 720) {
                return size;
            }
        }

        // If we cannot find the one that matches the aspect ratio, ignore the requirement.
        if (optimalSize == null) {
            // TODO : Backup in case we don't get a size.
            return sizes.get(0);
        }

        return optimalSize;
    }

    private Camera.Size getOptimalVideoSize(List<Camera.Size> sizes) {
        // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
        Camera.Size optimalSize = null;

        // Try to find a size match which suits the whole screen minus the menu on the left.
        for (Camera.Size size : sizes) {
            Log.d("CAM", "Checking size: width = " + size.width + ", Height = "+size.height);
            if (size.height <500) {
                return size;
            }
        }

        // If we cannot find the one that matches the aspect ratio, ignore the requirement.
        if (optimalSize == null) {
            // TODO : Backup in case we don't get a size.
            return sizes.get(sizes.size()-2); // random number really
        }

        return optimalSize;
    }

    public void OpenBackCamera() {
        if (isPreviewRunning) {
            mCamera.stopPreview();
            isPreviewRunning = false;
        }
        mCamera.release();
        mCamera = null;
        mCamera = Camera.open(0);
        mCamera.startPreview();
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size size = getOptimalPreviewSize(parameters.getSupportedPictureSizes());
        cameraHeight = size.height;
        cameraWidth = size.width;
        if (parameters.getFocusMode() == null) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        parameters.setPictureSize(size.width, size.height);
        mCamera.setParameters(parameters);
        mCamera.setParameters(parameters);
        isPreviewRunning = true;
        mCamera.autoFocus(this);


        try {
            Display display = ((WindowManager)context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
            CheckOrientationIsNotAllFuckingRetarded(parameters, display);
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        backCameraOpen = true;
    }

    public void SetFrontCamera() {
        if (isPreviewRunning) {
            mCamera.stopPreview();
            isPreviewRunning = false;
        }

        mCamera.release();
        mCamera = null;
        mCamera = Camera.open(1);
        mCamera.startPreview();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        Camera.Size size = getOptimalPreviewSize(sizes);
        //parameters.setJpegQuality(50);
        parameters.setPreviewSize(size.width,size.height);
        parameters.setPictureSize(size.width, size.height);
        mCamera.setParameters(parameters);
        isPreviewRunning = true;

        try {
            Display display = ((WindowManager)context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
            Camera.Parameters parameters2 = mCamera.getParameters();
            CheckOrientationIsNotAllFuckingRetarded(parameters2, display);
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        backCameraOpen = false;
    }

    // release the camera and its preview
    private void releaseCameraAndPreview() {
        mCamera.stopPreview();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // Set up the camera to record video
    private boolean setUpVideoCamera() {

        // Some phones do dumb shit if the camera isnt locked. No wonder this class is depreciated.
        mCamera.lock();
        List<Camera.Size> supportedSizes = mCamera.getParameters().getSupportedVideoSizes();

        // If the supported video sizes are the same as the preview sizes, the above call returns null. No idea why anyone ever thought that was a good idea.
        if (supportedSizes == null) {
            supportedSizes = mCamera.getParameters().getSupportedPreviewSizes();
        }

        recorder = new MediaRecorder();
        mCamera.unlock();
        recorder.setCamera(mCamera);

        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);

        if (backCameraOpen) {
            recorder.setOrientationHint(90);
        } else {
            recorder.setOrientationHint(270);
        }
        recorder.setVideoEncodingBitRate(3000000);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        Camera.Size size = getOptimalVideoSize(supportedSizes);
        recorder.setVideoSize(size.width, size.height);

        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        fileName =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        int eventId = new PreferencesAPI(context).GetEventId();
        //eventId += 1;
        // I Add the +1 because later in the piece(before saving, I increment the event Id. This entire class needs a rework.
        fileName += "/"+ eventId+ ".mp4"; //
        recorder.setOutputFile(fileName);

        recorder.setPreviewDisplay(mHolder.getSurface());
        try{
            recorder.prepare();
        } catch (Exception e) {
            String message = e.getMessage();
            recorder.release();
            recorder = null;
            return false;
        }
        video = true;
        return video;
    }

    private boolean disableVideoCamera() {
        //
        recorder.reset();
        recorder.release();
        mCamera.lock();
        mCamera.stopPreview();
        video = false;
        return true;
    }

    /*
        Listener for when the user takes a photo.
     */
    public void SetupCameraButtonListener() {
        final FloatingActionButton cameraButton = (FloatingActionButton) context.findViewById(R.id.captureButton);
        final FloatingActionButton videoButton = (FloatingActionButton) context.findViewById(R.id.videoButton);

        videoButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CAMERA_MODE == VIDEO) {
                    if (currentlyFilming) {
                        Toast.makeText(context, "Cannot toggle while filming", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CAMERA_MODE = PHOTO;
                    // Change to red
                    cameraButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
                    SimpleAnimations.ShrinkUnshrinkStandardFab(cameraButton);
                    cameraButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_camera));
                } else {
                    CAMERA_MODE = VIDEO;
                    // Set back to blue.
                    cameraButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
                    SimpleAnimations.ShrinkUnshrinkStandardFab(cameraButton);
                    cameraButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_videocam_white_24dp));

                }
            }
        });

       cameraButton.setOnClickListener(new OnClickListener() {
           @Override
           public void onClick(View v) {
               // Take a photo
               if (CAMERA_MODE == PHOTO) {
                   mCamera.takePicture(null, rawCallback, jpegCallback);

               } else {
                   toggleVideo();
                   // set the color to a pressed blue color.
                   cameraButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BBDEFB")));
               }
           }

       });

        ImageButton imageButton = (ImageButton) context.findViewById(R.id.backButtonCapture);
        imageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.finish();
            }
        });
    }

    private void toggleVideo() {
        if (currentlyFilming) {
            recorder.stop();
            disableVideoCamera();
            currentlyFilming = false;
            t.cancel();
            // Launch video intent to save the video.
            Intent save = new Intent();
            save.setClassName("com.teamunemployment.breadcrumbs", "com.teamunemployment.breadcrumbs.client.tabs.SaveVideoActivity");
            Bundle extras = new Bundle();
            extras.putBoolean("IsBackCameraOpen", backCameraOpen);
            save.putExtras(extras);
            save.putExtra("videoUrl", fileName); //The uri to our address
            context.startActivity(save);
        } else {
            currentlyFilming = true;
            //doProgressShit(50);
            startTimer();
            // Set up our video camera. This is different from taking a photo
            setUpVideoCamera();
           // videoButton.setBackgroundColor(getResources().getColor(R.color.accent));
            recorder.start();
        }
    }


    private void startTimer() {
        t=new Timer();
        final ProgressBar progressBar = (ProgressBar) context.findViewById(R.id.video_progress);
        //progressBar.getProgressDrawable().set(Color.parseColor("#C0D000"), android.graphics.PorterDuff.Mode.SRC_ATOP);
        //progressBar.setScaleY(4f);z
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                videoTimer += 50;
                if (videoTimer < 15000) {
                    progressBar.setProgress(videoTimer);
                } else {
                    // Stop video, and go to the next page
                    stopRecorderAndOpenViewScreen();
                    t.cancel();
                }
            }
        }, 50, 50);
    }

    private void stopRecorderAndOpenViewScreen() {
        if (recorder!= null) {
            recorder.stop();
            disableVideoCamera();
            currentlyFilming = false;
            // Launch video intent to save the video.
            Intent save = new Intent();
            save.setClassName("com.teamunemployment.breadcrumbs", "com.teamunemployment.breadcrumbs.client.tabs.SaveVideoActivity");
            Bundle extras = new Bundle();
            extras.putBoolean("IsBackCameraOpen", backCameraOpen);
            save.putExtras(extras);
            save.putExtra("videoUrl", fileName);
            context.startActivity(save);
        }
    }

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            System.out.println( "onPictureTaken - raw");
        }
    };

    /** Handles data for jpeg picture */
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = 0;
            Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length,options);

            int difference = cameraWidth -cameraHeight;
            Log.d("CAM", "Creating bitmap. Width: " + cameraWidth + " Height: " + cameraHeight + " Difference: " + difference);
            // Cache our photo.
            GlobalContainer.GetContainerInstance().SetBitMap(bm);
            Intent save = new Intent();
            Bundle extras = new Bundle();
            extras.putBoolean("IsBackCameraOpen", backCameraOpen);
            save.putExtras(extras);
            save.setClassName("com.teamunemployment.breadcrumbs", "com.teamunemployment.breadcrumbs.client.tabs.SaveEventFragment");
            context.startActivity(save);
        }
    };

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private String saveToInternalStorage(Bitmap bitmap) throws IOException {
        ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
        // Create imageDir
        File mypath=new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES.toString()+ "/profile.jpg")));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fos.close();
        }
        return mypath.getPath();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

/*
    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }*/
}