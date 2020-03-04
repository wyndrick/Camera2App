package com.example.mycameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioAttributes;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
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
    private ImageView mPlayVideo;
    private TextView mTextViewTimer;
    private int mCurrentPeriod = 0;
    private Timer myTimer;

    private SoundPool mSoundPool;
    private AssetManager mAssetManager;
    private int mShotSound;
    private int mStreamID;

    private String PathPhoto;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Состояние камеры: ожидание блокировки фокуса.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Состояние камеры: ожидание, когда экспозиция будет в состоянии предварительной съемки.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Состояние камеры: ожидание состояния экспозиции, отличного от предварительной съемки.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Состояние камеры: Снимок сделан.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    private int mSensorOrientation;

    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    //private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    //private File mFile;

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * The current state of camera state for taking pictures.
     *
     */
    private int mState = STATE_PREVIEW;

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

    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;

    private int exposureCompensation;
    private Float minFocusDist;
    private int[] aeModes;
    private int[] afModes;
    private int[] awbModes;
    private ConfirmationDialog confirmationDialog;

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Hide status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mButtonOpenCamera1 =  findViewById(R.id.change_camera);
        //mButtonOpenCamera2 =  findViewById(R.id.change_camera2);
        mButtonSwitchCameraSession =  findViewById(R.id.open_video);
        mButtonToMakeShot = findViewById(R.id.make_shot);
        mButtonOpenGallery = findViewById(R.id.open_gallery);
        mTextureView = findViewById(R.id.textureView);
        mPlayVideo = findViewById(R.id.mPlayVideo);
        mTextViewTimer = findViewById(R.id.txt_timer);

        Log.d(LOG_TAG, "Запрашиваем разрешение");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        )
        {
            requestPermission(VIDEO_PERMISSIONS, REQUEST_PERMISSIONS);
        }


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
                    mButtonSwitchCameraSession.setImageResource(R.drawable.asset9_press);
                    mButtonToMakeShot.setImageResource(R.drawable.asset12_press);
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
                    mButtonSwitchCameraSession.setImageResource(R.drawable.asset14_press);
                    mButtonToMakeShot.setImageResource(R.drawable.asset1212);
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
                        if (camera.isOpen()){
                            camera.makeVideo();
                        }

                    } else {
                        if (camera.isOpen()) {
                            camera.makePhoto();
                            mStreamID = playSound(mShotSound);
                        }
                    }
                }
            }
        });

        //звук для камеры
        createNewSoundPool();
        mAssetManager = getAssets();
        //получим идентификаторы
        mShotSound = loadSound("shot.ogg");

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

    //change orientation
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();

        }
    }

    private void createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }

    private int playSound(int sound) {
        if (sound > 0) {
            mStreamID = mSoundPool.play(sound, 1, 1, 1, 0, 1);
        }
        return mStreamID;
    }

    private int loadSound(String fileName) {
        AssetFileDescriptor afd;
        try {
            afd = mAssetManager.openFd(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.cant_upload_file) + fileName,
                    Toast.LENGTH_SHORT).show();
            return -1;
        }
        return mSoundPool.load(afd, 1);
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


    private void requestPermission(String[] permission, int requestCode) {
        // запрашиваем разрешение
        ActivityCompat.requestPermissions(this, permission, requestCode);
    }

    public Point getDisplaySize() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay(). getSize(size);
        return size;
    }

    private Size chooseVideoSize(Size[] choices) {
        Point displaySize = getDisplaySize();

        for (Size size : choices) {
            if ((1920 == size.getWidth() && 1080 == size.getHeight())) {
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


            //PebbleGear
            //sd-карта
//            String strSDCardPath = "";
//
//            File fileList[] = new File("/storage/").listFiles();
//            for (File file : fileList)
//            {     if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
//                strSDCardPath = file.getAbsolutePath();
//            }
//
//
//            if(strSDCardPath == null) {
//                Log.d("test", "sdcard not available");
//                showToast("SD-card not available");
//            }
//            else {
//                showToast(strSDCardPath);
//            }
        }

        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;
        private ImageReader mImageReader;


        public CameraService(CameraManager cameraManager, String cameraID) {
            mCameraManager = cameraManager;
            mCameraID = cameraID;
        }

        private Size mImageSize;

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

        /**
         * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
         */
        private CameraCaptureSession.CaptureCallback mCaptureCallback
                = new CameraCaptureSession.CaptureCallback() {

            private void process(CaptureResult result) {
                switch (mState) {
                    case STATE_PREVIEW: {
                        // We have nothing to do when the camera preview is working normally.
                        break;
                    }
                    case STATE_WAITING_LOCK: {
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afState == null) {
                            captureStillPicture();
                        } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();
                            } else {
                                runPrecaptureSequence();
                            }
                        }
                        break;
                    }
                    case STATE_WAITING_PRECAPTURE: {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                        }
                        break;
                    }
                    case STATE_WAITING_NON_PRECAPTURE: {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureResult partialResult) {
                process(partialResult);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                process(result);
            }

        };

        private void runPrecaptureSequence() {
            try {
                // This is how to tell the camera to trigger.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                // Tell #mCaptureCallback to wait for the precapture sequence to be set.
                mState = STATE_WAITING_PRECAPTURE;
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        //
        public void makeVideo() {
            if (mIsRecordingVideo) {
                stopRecordingVideo();
                mButtonToMakeShot.setImageResource(R.drawable.asset1212);
                //stop timer
                mCurrentPeriod = 0;
                if (myTimer != null) {
                    myTimer.cancel();
                    myTimer = null;
                    mTextViewTimer.setText("00:00");
                    mTextViewTimer.setVisibility(View.GONE);
                }
            } else {
                startRecordingVideo();
                mButtonToMakeShot.setImageResource(R.drawable.asset1213);

                //start timer
                mTextViewTimer.setVisibility(View.VISIBLE);
                if (myTimer != null) {
                    myTimer.cancel();
                }

                myTimer = new Timer();
                myTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        TimerMethod();
                    }
                }, 0, 1000);
            }
        }

        private void TimerMethod() {
            runOnUiThread(Timer_Tick);
        }

        private Runnable Timer_Tick = new Runnable() {
            public void run() {
                mCurrentPeriod++;
                String temp = (new SimpleDateFormat("mm:ss")).format(new Date(
                        mCurrentPeriod * 1000));
                mTextViewTimer.setText(temp);
            }
        };

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

                mButtonOpenCamera1.setEnabled(false);
                mButtonOpenGallery.setEnabled(false);
                mButtonSwitchCameraSession.setEnabled(false);
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
            mButtonOpenCamera1.setEnabled(true);
            mButtonOpenGallery.setEnabled(true);
            mButtonSwitchCameraSession.setEnabled(true);


            mIsRecordingVideo = false;
            // Stop recording
            try {

                mMediaRecorder.stop();
                mMediaRecorder.reset();


                Toast.makeText(MainActivity.this, getString(R.string.video_saved) + mNextVideoAbsolutePath,
                        Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, getString(R.string.video_saved) + mNextVideoAbsolutePath);
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

        private void captureStillPicture() {
            try {
                // Это CaptureRequest.Builder, который мы используем для съемки.
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(mImageReader.getSurface());



                // Required for RAW capture
//                captureBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON);
//                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
//                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) ((214735991 - 13231) / 2));
//                captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
//                captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, (10000 - 100) / 2);
                //setAutoFlash(captureBuilder);

                // Используйте те же режимы AE и AF, что и при предварительном просмотре.
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 12);
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, new RggbChannelVector(86, 86, 86, 86));

                CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        //createCameraPreviewSession();
                        unlockFocus();

                    }
                };
                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        public void makePhoto (){
            lockFocus();
            //captureStillPicture();
        }

        private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
            if (mFlashSupported) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
        }

        /**
         * Зафиксируйте фокусировку как первый шаг для захвата неподвижного изображения.
         */
        private void lockFocus() {
            try {
                // This is how to tell the camera to lock focus.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_START);
                // Tell #mCaptureCallback to wait for the lock.
                mState = STATE_WAITING_LOCK;
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Разблокировать фокус. Этот метод следует вызывать, когда последовательность захвата неподвижного изображения
         *закончено.
         */
        private void unlockFocus() {
            try {
                // Reset the auto-focus trigger
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                setAutoFlash(mPreviewRequestBuilder);
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
                // After this, the camera will go back to the normal state of preview.
                mState = STATE_PREVIEW;
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
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
                Log.d(LOG_TAG, String.valueOf(mSensorOrientation));

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
                // Получениe характеристик камеры
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
                initFPS(characteristics);
                //осветление/затемнение изображения камеры
                Range<Integer> range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                //double exposureCompensationSteps = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).doubleValue();
                //exposureCompensation = (int)( 2.0 / exposureCompensationSteps );
                exposureCompensation = (int)setExposure(0, range);

                // Enable auto-magical 3A run by camera device
                minFocusDist = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

                // Если доступен режим автоматического управления вспышкой, используйте его, иначе по умолчанию
                // режим «on», который гарантированно всегда будет доступен.
                aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);

                // Если доступен режим «Непрерывное изображение», используйте его, в противном случае
                // по умолчанию установлено значение «АВТО».
                afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

                // Если доступен режим автоматического управления балансом белого, используйте его.
                awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

                // Получения списка выходного формата, который поддерживает камера
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

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

                // Проверьте, поддерживается ли вспышка.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                //mFlashSupported = available == null ? false : available;
                mFlashSupported = false;

                return;
//                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            //}
            } catch (NullPointerException e) {
                // Currently an NPE is thrown when the Camera2API is used but not supported on the
                // device this code runs.
                ErrorDialog.newInstance(getString(R.string.camera_error))
                        .show(getFragmentManager(), "dialog");
            }
        }

        private float getUIAspectRatio() {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            return (float)displaySize.x / displaySize.y;
        }

        public float setExposure(double exposureAdjustment, Range<Integer> range1) {
            float newCalculatedValue = 0;
            int minExposure = range1.getLower();
            int maxExposure = range1.getUpper();

            if (minExposure != 0 || maxExposure != 0) {
                if (exposureAdjustment >= 0) {
                    newCalculatedValue = (float) (minExposure * exposureAdjustment);
                } else {
                    newCalculatedValue = (float) (maxExposure * -1 * exposureAdjustment);
                }
            }
            return newCalculatedValue;
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
                //createCameraPreviewSession();
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
                mCameraOpenCloseLock.release();
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());

                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraOpenCloseLock.release();
                camera.close();

                Log.i(LOG_TAG, "disconnect camera  with id:"+mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                mCameraOpenCloseLock.release();
                camera.close();
                Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
                mCameraDevice = null;
            }
        };

        public Uri addVideo(File videoFile) {
            ContentValues values = new ContentValues(3);
            values.put(MediaStore.Video.Media.TITLE, "My video title");
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
            return getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }

        public void createCameraPreviewSession() {
            try {
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                assert texture != null;

                // Мы настраиваем размер буфера по умолчанию, чтобы он соответствовал размеру предварительного просмотра камеры, который мы хотим.
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                //texture.setDefaultBufferSize(1280,720);

                Surface surface = new Surface(texture);

                mPreviewRequestBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surface);

                if(fpsRange != null) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                }


                // Enable auto-magical 3Auto run by camera device
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//
//                // Auto focus should be continuous for camera preview.
//                // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
//                boolean noAFRun = (minFocusDist == null || minFocusDist == 0);
//                if (!noAFRun) {
//                    // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
//                    //int[] afModes = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
//                    if (contains(afModes, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
//                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                        // Flash автоматически включается при необходимости.
//                        setAutoFlash(mPreviewRequestBuilder);
//
//                        // Finally, we start displaying the camera preview.
//                        mPreviewRequest = mPreviewRequestBuilder.build();
//                    }
//                    else {
//                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
//                    }
//                }
//
//                // If there is an auto-magical flash control mode available, use it, otherwise default to
//                // the "on" mode, which is guaranteed to always be available.
//                //int[] aeModes = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
//                if (contains(aeModes, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
//                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//                }
//                else {
//                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
//                }
//
//                // If there is an auto-magical white balance control mode available, use it.
//                //int[] awbModes = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
//                if (contains(awbModes, CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
//                    // Allow AWB to run auto-magically if this device supports this
//                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
//                }


                mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                mCaptureSession = cameraCaptureSession;
                                try {

                                    // Auto focus should be continuous for camera preview.
//                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureCompensation);

                                    // Используйте те же режимы AE и AF, что и при предварительном просмотре.
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 12);
                                    mPreviewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, new RggbChannelVector(86, 86, 86, 86));
                                    // Flash is automatically enabled when necessary.
                                    //setAutoFlash(mPreviewRequestBuilder);

                                    if(fpsRange != null) {
                                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                                    }
                                    // Finally, we start displaying the camera preview.
                                    mPreviewRequest = mPreviewRequestBuilder.build();
                                    mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                            mCaptureCallback, mBackgroundHandler);

                                    //mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),null,mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }


                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                showToast("Failed");
                            }
                        }, null
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
        public Range<Integer> fpsRange;
        private void initFPS(CameraCharacteristics cameraCharacteristics){
            try {
                Range<Integer>[] ranges = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if(ranges != null) {
                    for (Range<Integer> range : ranges) {
                        int upper = range.getUpper();
                        Log.i("Camera", "[FPS Range Available] is:" + range);
                        if (upper >= 10) {
                            if (fpsRange == null || upper < fpsRange.getUpper()) {
                                fpsRange = range;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i("Camera", "[FPS Range] is:" + fpsRange);
        }


        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera(int width, int height) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }

            setUpCameraOutputs(width, height);
            configureTransform(width, height);

            try {
//                if (!mCameraOpenCloseLock.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
//                    throw new RuntimeException("Time out waiting to lock camera opening.");
//                }
                mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);

            } catch (CameraAccessException e) {
                Log.i(LOG_TAG, e.getMessage());
            }

//            }catch (InterruptedException e) {
//                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
//            }
        }

        public void closeCamera() {
            try {
                mCameraOpenCloseLock.acquire();
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                if (null != mImageReader) {
                    mImageReader.close();
                    mImageReader = null;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
            } finally {
                mCameraOpenCloseLock.release();
            }
        }
    }


    private boolean contains(int[] afModes, int key) {
        Arrays.sort(afModes);
        return Arrays.binarySearch(afModes, key) >= 0;
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


    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            //ConfirmationDialog = ConfirmationDialog.getInstance(inputPath, filename, outputPath);
            confirmationDialog.show(getSupportFragmentManager(), "dialog_confirm");
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        }
    }

    // Обработчик обратного вызова для окончательного ответа
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getFragmentManager(), "dialog_confirm");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }


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
        private String mOutputPath;

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
