package com.example.mycameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;
import android.widget.SimpleCursorAdapter;

import java.net.URI;

public class GalleryActivity extends AppCompatActivity implements ViewSwitcher.ViewFactory,
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private ImageSwitcher mImageSwitcher;
    int position = 0;
    private int[] mImageIds;

    private static final int GALLERY_LOADER_ID = 1;
    private static final int THUMB_LOADER_ID = 2;

    //define source of MediaStore.Images.Media, internal or external storage
    final Uri sourceUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    final Uri thumbUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;

    SimpleCursorAdapter mySimpleCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Intent intent = getIntent();
        String path = intent.getStringExtra("pathToFile");

        mImageSwitcher = (ImageSwitcher)findViewById(R.id.imageSwitcher);
        mImageSwitcher.setFactory(this);

        Animation inAnimation = new AlphaAnimation(0, 1);
        inAnimation.setDuration(2000);
        Animation outAnimation = new AlphaAnimation(1, 0);
        outAnimation.setDuration(2000);

        mImageSwitcher.setInAnimation(inAnimation);
        mImageSwitcher.setOutAnimation(outAnimation);


        //getLoaderManager().initLoader(THUMB_LOADER_ID, null, this);
        getSupportLoaderManager().initLoader(0, null, this);

        int[] mImageIds = { R.drawable.ic_launcher_background };

        //mImageSwitcher.setImageResource(mImageIds[0]);
        mImageSwitcher.setImageURI(Uri.parse("file:///storage/4B40-1704/DCIM/Camera/IMG_20190228_181603.jpg"));
        //mImageSwitcher.setImageURI(mImageIds[0]);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonForward:
                setPositionNext();
                mImageSwitcher.setImageResource(mImageIds[position]);
                break;
            case R.id.buttonPrev:
                setPositionPrev();
                mImageSwitcher.setImageResource(mImageIds[position]);
                break;

            default:
                break;
        }
    }

    public void setPositionNext() {
        position++;
        if (position > mImageIds.length - 1) {
            position = 0;
        }
    }

    public void setPositionPrev() {
        position--;
        if (position < 0) {
            position = mImageIds.length - 1;
        }
    }

    @Override
    public View makeView() {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(new
                ImageSwitcher.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        imageView.setBackgroundColor(0xFF000000);
        return imageView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        String[] projection = {
                MediaStore.Images.Media.DATA
        };
        CursorLoader cursorLoader = new CursorLoader(this, MediaStore.Images.Media.getContentUri("external"), projection, null, null, null);

        return cursorLoader;

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        String imagePath;
        if (data != null) {
            int columnIndex = data.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            data.moveToFirst();
            imagePath = data.getString(columnIndex);
        } else {
            Uri imageUri = sourceUri;
            imagePath = imageUri.getPath();
        }

        //setupImageView();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
}
