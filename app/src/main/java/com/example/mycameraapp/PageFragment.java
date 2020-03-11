package com.example.mycameraapp;


import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class PageFragment extends Fragment {

    private String imageResource;
    private Bitmap bitmap;
    private Cursor cursor;
    private String path;

    private View v;
    public boolean isVideo = false;
    private ImageView mPlayVideo, mPauseVideo, mBackVideo, mNextVideo;

    public PageFragment() {
        // Required empty public constructor
    }


    VideoView videoView;
    ImageView imageView;

//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        Activity activity = getActivity();
//        v = view;
//        path = imageResource;
//        if (isVideo) {
//            // установите свой путь к файлу на SD-карточке
//
//            imageView.setVisibility(View.GONE);
//            videoView.setVisibility(View.VISIBLE);
//            videoView.setVideoURI(Uri.parse("file://"+ imageResource));
//            videoView.setMediaController(new MediaController(activity));
//            videoView.requestFocus(0);
//            videoView.seekTo(30);
////            videoView.start(); // начинаем воспроизведение автоматически
//        } else {
//            imageView.setVisibility(View.VISIBLE);
//            videoView.setVisibility(View.GONE);
//            imageView.setImageURI(Uri.parse("file://"+ imageResource));
//        }
//
//    }


    public static boolean isImageFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }
    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    private final String TAG = "VideoFragement";

    public static PageFragment getInstance(String resourcePath) {
        PageFragment f = new PageFragment();
        Bundle args = new Bundle();
        args.putString("image_source", resourcePath);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        v = view;
        videoView = view.findViewById(R.id.video_thumb);
        imageView = view.findViewById(R.id.image);
        mPlayVideo = view.findViewById(R.id.mPlayVideo);
        mPauseVideo = view.findViewById(R.id.mPauseVideo);
        mBackVideo = view.findViewById(R.id.mBackVideo);
        mNextVideo = view.findViewById(R.id.mNextVideo);

        OnClickAnimTouchListener clickAnim = new OnClickAnimTouchListener();

        mPlayVideo.setOnTouchListener(clickAnim);
        mPauseVideo.setOnTouchListener(clickAnim);
        mNextVideo.setOnTouchListener(clickAnim);

        OnClickAnimTouchListener clickAnim2 = new OnClickAnimTouchListener();
        clickAnim2.scaleX = -0.8f;
        clickAnim2.scaleXDefault = -1f;
        mBackVideo.setOnTouchListener(clickAnim2);

        if (getArguments() != null) {
            imageResource = getArguments().getString("image_source");
        }

        mPlayVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.start();
                mPlayVideo.setVisibility(View.GONE);
            }
        });

        mPauseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.pause();
                mPlayVideo.setVisibility(View.VISIBLE);
                mPauseVideo.setVisibility(View.GONE);
                mBackVideo.setVisibility(View.GONE);
                mNextVideo.setVisibility(View.GONE);
            }
        });
        mNextVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int seekTime = Math.min(videoView.getCurrentPosition() + 5000, videoView.getDuration());
                videoView.seekTo(seekTime);
                if (!videoView.isPlaying()) {
                    videoView.resume();
                }
            }
        });
        mBackVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int seekTime = Math.max(videoView.getCurrentPosition() - 1000, 0);
                boolean isVideoPlaying = videoView.isPlaying();
                if (isVideoPlaying) {
                    videoView.seekTo(seekTime);
                } else {
                    videoView.seekTo(seekTime);
                    videoView.start();
                }
            }
        });
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayVideo.getVisibility() != View.VISIBLE) {
                    mPauseVideo.setVisibility(View.VISIBLE);
                    mBackVideo.setVisibility(View.VISIBLE);
                    mNextVideo.setVisibility(View.VISIBLE);
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d("video", "setOnErrorListener ");
                return true;
            }
        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: ");
        try{
            if (imageResource != null && !TextUtils.isEmpty(imageResource)) {
                if (isVideoFile(imageResource)) {
                    isVideo = true;
                    videoView.setVideoPath(imageResource);
                    videoView.seekTo(1);
                    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            //mp.start();
                            //mp.setLooping(true);
                            Log.e(TAG, "видео подготовлено: ");
                        }
                    });
                    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            Log.e(TAG, "EventBus.getDefault().post(new OnNextEvent());");
            //                EventBus.getDefault().post(new OnNextEvent());
                        }
                    });
                    videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            return false;
                        }
                    });
                    imageView.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);

                    mPlayVideo.setVisibility(View.VISIBLE);
                    mPauseVideo.setVisibility(View.GONE);
                    mBackVideo.setVisibility(View.GONE);
                    mNextVideo.setVisibility(View.GONE);

                    setUserVisibleHint(mIsVisibleToUser);
                } else {
                    isVideo = false;
                    imageView.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);

                    mPlayVideo.setVisibility(View.GONE);
                    mPauseVideo.setVisibility(View.GONE);
                    mBackVideo.setVisibility(View.GONE);
                    mNextVideo.setVisibility(View.GONE);

                    imageView.setImageURI(Uri.parse("file://"+ imageResource));

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    boolean mIsVisibleToUser = false;
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        try{

            mIsVisibleToUser = isVisibleToUser;
            Log.e(TAG, "setUserVisibleHint: " + isVisibleToUser);
            if (isVideo) {
                if (isVisibleToUser && videoView != null) {
                    mPlayVideo.setVisibility(View.VISIBLE);
                    mPauseVideo.setVisibility(View.GONE);
                    mBackVideo.setVisibility(View.GONE);
                    mNextVideo.setVisibility(View.GONE);
                    //videoView.start();
                } else {
                    if (videoView != null) {
                        videoView.pause();
                        videoView.resume();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }
}
