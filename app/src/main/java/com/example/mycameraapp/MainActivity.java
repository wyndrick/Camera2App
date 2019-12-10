package com.example.mycameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "myLogs";

    private static final int REQUEST_PERMISSIONS = 10001;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

    };

    // объявляем разрешение, которое нам нужно получить

    CameraService[] myCameras = null;

    private CameraManager mCameraManager    = null;
    private final int CAMERA1   = 0;
    private final int CAMERA2   = 1;

    private ImageButton mButtonOpenCamera1 = null;
    private ImageButton mButtonOpenCamera2 = null;
    private ImageButton mButtonSwitchCameraSession = null;
    private Boolean isPressed = false;
    private ImageButton mButtonToMakeShot = null;
    private AutoFillTextureView mTextureView = null;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;
    private ImageButton mButtonOpenGallery;

    private String PathPhoto;

    private int mSensorOrientation;

    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    //private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    //private File mFile;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
    }



    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    boolean mCurrentSessionIsVideo = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Log.d(LOG_TAG, "Запрашиваем разрешение");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        )
        {
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_PERMISSIONS);
        }



        mButtonOpenCamera1 =  findViewById(R.id.change_camera);
        //mButtonOpenCamera2 =  findViewById(R.id.change_camera2);
        mButtonSwitchCameraSession =  findViewById(R.id.open_video);
        mButtonToMakeShot = findViewById(R.id.make_shot);
        mButtonOpenGallery = findViewById(R.id.open_gallery);
        mTextureView = findViewById(R.id.textureView);

        mButtonOpenGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GalleryActivity2.class);
                intent.putExtra("pathToFile", PathPhoto);
                startActivity(intent);
                finish();
            }
        });

        mButtonSwitchCameraSession.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                for (CameraService camera : myCameras) {
                    if (camera != null) {
                        if (camera.isOpen()) {
                            camera.closeCamera();
                        }
                    }
                }
                if (mCurrentSessionIsVideo) {
                    if (isPressed) {
                        //start camera 1 (back)
                        if (myCameras[CAMERA1] != null) {
                            if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        }
                    } else {
                        //start camera 2 (front)
                        if (myCameras[CAMERA2] != null) {
                            if (!myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        }
                    }
                    mButtonSwitchCameraSession.setImageResource(R.drawable.asset9);
                    mCurrentSessionIsVideo = false;
                }
                else {
                    if (isPressed) {
                        if (!myCameras[CAMERA1].isOpen()) {
                            myCameras[CAMERA1].openVideoCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        }
                    } else {
                        if (!myCameras[CAMERA2].isOpen()) {
                            myCameras[CAMERA2].openVideoCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        }
                    }
                    mButtonSwitchCameraSession.setImageResource(R.drawable.icon_photo);
                    mCurrentSessionIsVideo = true;
                }

            }
        });

        mButtonOpenCamera1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPressed) {
                    //start camera 2 (front)
                    if (myCameras[CAMERA1].isOpen()) {myCameras[CAMERA1].closeCamera();}
                    if (myCameras[CAMERA2] != null) {
                        if (mCurrentSessionIsVideo) {
                            if (!myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].openVideoCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        } else {
                            if (!myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        }
                    }
                    isPressed = false;
                }
                else {
                    //start camera 1 (back)
                    if (myCameras[CAMERA2].isOpen()) {myCameras[CAMERA2].closeCamera();}
                    if (myCameras[CAMERA1] != null) {
                        if (mCurrentSessionIsVideo) {
                            if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openVideoCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        } else {
                            if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                        }
                    }
                    isPressed = true;
                }

            }
        });


        mButtonToMakeShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                for (CameraService camera : myCameras) {
                    if (mCurrentSessionIsVideo) {
                        if (camera.isOpen()) camera.makeVideo();
                    } else {
                        if (camera.isOpen()) camera.makePhoto();
                    }
                }
            }
        });


        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            // Получение списка камер с устройства
            myCameras = new CameraService[mCameraManager.getCameraIdList().length];

            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: "+cameraID);
                int id = Integer.parseInt(cameraID);

                // создаем обработчик для камеры
                myCameras[id] = new CameraService(mCameraManager,cameraID);
            }
        }
        catch(CameraAccessException e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
    }


    public void openCamera() {
        //start camera 1 (back)
        if (myCameras[CAMERA2].isOpen()) {myCameras[CAMERA2].closeCamera();}
        if (myCameras[CAMERA1] != null) {
            if (mCurrentSessionIsVideo) {
                if (!myCameras[CAMERA1].isOpen())
                    myCameras[CAMERA1].openVideoCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                if (!myCameras[CAMERA1].isOpen())
                    myCameras[CAMERA1].openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }
        isPressed = true;
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    private boolean isPermissionGranted(String permission) {
        // проверяем разрешение - есть ли оно у нашего приложения
        int permissionCheck = ActivityCompat.checkSelfPermission(this, permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission, int requestCode) {
        // запрашиваем разрешение
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    public Point getDisplaySize() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay(). getSize(size);
        return size;
    }

    private Size chooseVideoSize(Size[] choices) {
        Point displaySize = getDisplaySize();

        for (Size size : choices) {
            if ((1920 == size.getWidth() && 1080 == size.getHeight() ||
                    (1280 == size.getWidth() && 720 == size.getHeight()))) {
                return size;
            }
        }

        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 16 / 9 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(LOG_TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Camera resolution obtained", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Camera permission not received", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public class CameraService {
        private File directory;
        private File mFile = null;
        private void createDirectory() {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"100ANDRO");
            if (directory.exists()) {
                PathPhoto = directory.toString();
            } else {
                directory = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"Camera");
                if (!directory.exists())
                        directory.mkdirs();
                PathPhoto = directory.toString();
            }
        }

        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;
        private ImageReader mImageReader;


        public CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;

        }

        /**
         * The {@link android.util.Size} of video recording.
         */
        private Size mVideoSize;

        /**
         * MediaRecorder
         */
        private MediaRecorder mMediaRecorder;

        /**
         * Whether the app is recording video now
         */
        private boolean mIsRecordingVideo;


        /**
         * A {@link Semaphore} to prevent the app from exiting before closing the camera.
         */
        private Semaphore mCameraOpenCloseLock = new Semaphore(1);

        private Integer mSensorOrientation;
        private String mNextVideoAbsolutePath;
        private CaptureRequest.Builder mPreviewBuilder;

        //
        public void makeVideo() {
            if (mIsRecordingVideo) {
                stopRecordingVideo();
            } else {
                startRecordingVideo();
            }
        }

        private void setUpMediaRecorder() throws IOException {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            CamcorderProfile cp = CamcorderProfile
//                    .get(CamcorderProfile.QUALITY_HIGH);
//            mMediaRecorder.setProfile(cp);
            if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
                mNextVideoAbsolutePath = getVideoFilePath(MainActivity.this);
            }
            mMediaRecorder.setMaxDuration(1000000);
            mMediaRecorder.setMaxFileSize(500000000);
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);

            switch (mSensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
            mMediaRecorder.prepare();
        }

        private String getVideoFilePath(Context context) {
            createDirectory();
            return PathPhoto + "/" + "video_" +  System.currentTimeMillis() + ".mp4";
        }


        private void startRecordingVideo() {
            if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
                return;
            }
            try {


                closePreviewSession();
                setUpMediaRecorder();
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                assert texture != null;
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                List<Surface> surfaces = new ArrayList<>();

                // Set up Surface for the camera preview
                Surface previewSurface = new Surface(texture);
                surfaces.add(previewSurface);
                mPreviewBuilder.addTarget(previewSurface);

                // Set up Surface for the MediaRecorder
                Surface recorderSurface = mMediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewBuilder.addTarget(recorderSurface);

                mButtonOpenCamera1.setVisibility(View.INVISIBLE);
                mButtonOpenGallery.setVisibility(View.INVISIBLE);
                mButtonSwitchCameraSession.setVisibility(View.INVISIBLE);
                // Start a capture session
                // Once the session starts, we can update the UI and start recording
                mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        mCaptureSession = cameraCaptureSession;
                        updatePreview();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                // UI
//                                mButtonVideo.setText(R.string.stop);
                                mIsRecordingVideo = true;

                                // Start recording
                                mMediaRecorder.start();
                            }
                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException | IOException e) {
                e.printStackTrace();
            }

        }

        private void closePreviewSession() {
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
        }

        private void stopRecordingVideo() {
            // UI

            mButtonOpenCamera1.setVisibility(View.VISIBLE);
            mButtonOpenGallery.setVisibility(View.VISIBLE);
            mButtonSwitchCameraSession.setVisibility(View.VISIBLE);


            mIsRecordingVideo = false;
//            mButtonVideo.setText(R.string.record);
            // Stop recording
            try {

                mMediaRecorder.stop();
                mMediaRecorder.reset();


                Toast.makeText(MainActivity.this, "Video saved: " + mNextVideoAbsolutePath,
                        Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "Video saved: " + mNextVideoAbsolutePath);
                addVideo(new File(mNextVideoAbsolutePath));


                MyFileObserver fb = new MyFileObserver(mNextVideoAbsolutePath, FileObserver.CLOSE_WRITE);
                fb.startWatching();


                mNextVideoAbsolutePath = null;



            } catch(Exception ex) {
                Toast.makeText(MainActivity.this, "An error occured while MediaRecorder",
                        Toast.LENGTH_SHORT).show();
                ex.printStackTrace();
            }

            startPreview();
        }


        class MyFileObserver extends FileObserver {
            public MyFileObserver (String path, int mask) {
                super(path, mask);
            }

            public void onEvent(int event, String path) {
                if (event == FileObserver.CLOSE_WRITE) {
                    addVideo(new File(path));
                }
            }
        }

        /**
         * Start the camera preview.
         */
        private void startPreview() {
            if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
                return;
            }
            try {
                closePreviewSession();
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                assert texture != null;
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Surface previewSurface = new Surface(texture);
                mPreviewBuilder.addTarget(previewSurface);

                mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCaptureSession = session;
                                updatePreview();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        /**
         * Update the camera preview. {@link #startPreview()} needs to be called in advance.
         */
        private void updatePreview() {
            if (null == mCameraDevice) {
                return;
            }
            try {
                setUpCaptureRequestBuilder(mPreviewBuilder);
                HandlerThread thread = new HandlerThread("CameraPreview");
                thread.start();
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        }

        public void makePhoto (){
            try {
                // This is the CaptureRequest.Builder that we use to take a picture.
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(mImageReader.getSurface());

                //int rotation = getWindowManager().getDefaultDisplay().getRotation();
                //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {

                        super.onCaptureCompleted(session, request, result);

//                        updatePreview();
                        createCameraPreviewSession();
                        //swapImageAdapter();
                    }
                };
                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);


            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /** 更新预览 */
        protected void updatePhotosPreview()
        {
            if (mCaptureSession == null) return;
            try
            {
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e(LOG_TAG, "updatePreview error");
            }
        }


        /**
         * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
         */
        @SuppressWarnings("MissingPermission")
        private void openVideoCamera(int width, int height) {
            try {
                Log.d(LOG_TAG, "tryAcquire");
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }

                // Choose the sizes for camera preview and video recording
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
                StreamConfigurationMap map = characteristics
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (map == null) {
                    throw new RuntimeException("Cannot get available preview/video sizes");
                }
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mPreviewSize = chooseOptimalVideoSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, mVideoSize);


                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(getUIAspectRatio());//mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(getUIAspectRatio());//mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                configureTransform(width, height);
                mMediaRecorder = new MediaRecorder();
                mCameraManager.openCamera(mCameraID, mStateCallback, null);
            } catch (CameraAccessException e) {
                Toast.makeText(MainActivity.this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
                finish();
            } catch (NullPointerException e) {
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.");
            }
        }
        private Size chooseOptimalVideoSize(Size[] choices, int width, int height, Size aspectRatio) {
            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<>();
            int w = aspectRatio.getWidth();
            int h = aspectRatio.getHeight();
            for (Size option : choices) {
                if (option.getHeight() == option.getWidth() * h / w &&
                        option.getWidth() >= width && option.getHeight() >= height) {
                    bigEnough.add(option);
                }
            }

            // Pick the smallest of those, assuming we found any
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else {
                Log.e(LOG_TAG, "Couldn't find any suitable preview size");
                return choices[0];
            }
        }




        private void setUpCameraOutputs(int width, int height) {

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {

                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(mCameraID);


                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();

                if (map == null) {
                    throw new RuntimeException("Cannot get available preview/video sizes");
                }
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mPreviewSize = chooseOptimalVideoSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, mVideoSize);


                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(getUIAspectRatio());//mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(getUIAspectRatio());//mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                return;
//                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        private float getUIAspectRatio() {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            return (float)displaySize.x / displaySize.y;
        }


        private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                       int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {


            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<>();
            // Collect the supported resolutions that are smaller than the preview Surface
            List<Size> notBigEnough = new ArrayList<>();
            int w = aspectRatio.getWidth();
            int h = aspectRatio.getHeight();
            for (Size option : choices) {
                if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                        option.getHeight() == option.getWidth() * h / w) {
                    if (option.getWidth() >= textureViewWidth &&
                            option.getHeight() >= textureViewHeight) {
                        bigEnough.add(option);
                    } else {
                        notBigEnough.add(option);
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else if (notBigEnough.size() > 0) {
                return Collections.max(notBigEnough, new CompareSizesByArea());
            } else {
                Log.e(LOG_TAG, "Couldn't find any suitable preview size");
                return choices[0];
            }
        }


        private void transformImage (int width, int height) {
            if (mTextureView == null) {

                return;
            } else try {
                {
                    Matrix matrix = new Matrix();
                    int rotation = getWindowManager().getDefaultDisplay().getRotation();
                    RectF textureRectF = new RectF(0, 0, width, height);
                    RectF previewRectF = new RectF(0, 0, mTextureView.getHeight(), mTextureView.getWidth());
                    float centerX = textureRectF.centerX();
                    float centerY = textureRectF.centerY();
                    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                        previewRectF.offset(centerX - previewRectF.centerX(), centerY - previewRectF.centerY());
                        matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
                        float scale = Math.max((float) width / width, (float) height / width);
                        matrix.postScale(scale, scale, centerX, centerY);
                        matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                    }
                    mTextureView.setTransform(matrix);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
                = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                createDirectory();
                mFile = new File(PathPhoto + "/" + "photo_" + System.currentTimeMillis() + ".jpg");
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
                showToast("Saved: " + mFile);
            }


        };

        private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                mCameraDevice = cameraDevice;
                mCameraOpenCloseLock.release();
                startPreview();
                if (null != mTextureView) {
                    configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
                finish();
            }

        };

        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());

                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                camera.close();

                Log.i(LOG_TAG, "disconnect camera  with id:"+mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                camera.close();
                Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
            }
        };

        public Uri addVideo(File videoFile) {
            ContentValues values = new ContentValues(3);
            values.put(MediaStore.Video.Media.TITLE, "My video title");
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
            return getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }

        private void createCameraPreviewSession() {

            mImageReader = ImageReader.newInstance(1920,1080, ImageFormat.JPEG,1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            texture.setDefaultBufferSize(1920,1080);
            Surface surface = new Surface(texture);

            try {
                final CaptureRequest.Builder builder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                builder.addTarget(surface);

                mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                try {
                                    mCaptureSession.setRepeatingRequest(builder.build(),null,mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) { }}, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }




        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera(int width, int height) {
            try {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    setUpCameraOutputs(width, height);
                    configureTransform(width, height);
                    mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);
                }

            } catch (CameraAccessException e) {
                Log.i(LOG_TAG,e.getMessage());

            }
        }

        public void closeCamera() {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    }


    @Override
    public void onPause() {
        if(myCameras[CAMERA1].isOpen()){myCameras[CAMERA1].closeCamera();}
        if(myCameras[CAMERA2].isOpen()){myCameras[CAMERA2].closeCamera();}
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        startBackgroundThread();
    }





    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = MainActivity.this;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                    Snackbar
                            .make(mTextureView, text, Snackbar.LENGTH_LONG)
                            .show();
                }
            });
        }
    }


    private class ImageSaver implements Runnable {
        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();

                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(mFile);
                        mediaScanIntent.setData(contentUri);
                        sendBroadcast(mediaScanIntent);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
