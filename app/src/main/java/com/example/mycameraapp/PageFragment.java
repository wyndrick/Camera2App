package com.example.mycameraapp;


import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class PageFragment extends Fragment {

    private String imageResource;
    private Bitmap bitmap;
    private Cursor cursor;
    private String path;

    private View v;
    public boolean isVideo = false;
    public static PageFragment getInstance(String resourcePath) {
        PageFragment f = new PageFragment();
        Bundle args = new Bundle();
        args.putString("image_source", resourcePath);
        f.setArguments(args);
        return f;
    }


    public PageFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageResource = getArguments().getString("image_source");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        v = view;
        VideoView videoView = (VideoView) view.findViewById(R.id.video_thumb);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        if (isVideo) {
            // установите свой путь к файлу на SD-карточке

            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse("file://"+ imageResource));

//            videoView.setMediaController(new MediaController(activity));
            videoView.requestFocus(0);
            videoView.seekTo(30);
            videoView.start(); // начинаем воспроизведение автоматически
        } else {
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            imageView.setImageURI(Uri.parse("file://"+ imageResource));
        }

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {

        if (isVideo && v != null) {
            VideoView videoView = (VideoView) v.findViewById(R.id.video_thumb);
            videoView.seekTo(30);
            videoView.start(); // начинаем воспроизведение автоматически
        }
    }
}
