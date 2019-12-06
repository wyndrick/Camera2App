package com.example.mycameraapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;

import androidx.viewpager.widget.ViewPager;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity2 extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private ArrayList<String> images;
    private ArrayList<String> imagesPath;
    private BitmapFactory.Options options;
    private ViewPager viewPager;
    private ImageView btnNext, btnPrev, btnDel, btnCam, btnCard, btnMakeShot;
    private FragmentStatePagerAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery2);

        images = new ArrayList<>();
        imagesPath = new ArrayList<>();

        //find view by id
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        btnNext = (ImageView)findViewById(R.id.btn_right);
        btnPrev = (ImageView)findViewById(R.id.btn_left);

        btnDel = (ImageView)findViewById(R.id.del_photo);
        btnCam = (ImageView)findViewById(R.id.open_camera);
        btnMakeShot = (ImageView)findViewById(R.id.make_shot);
        btnCard = (ImageView)findViewById(R.id.save_sd);

        btnPrev.setOnClickListener(onClickListener(0));
        btnNext.setOnClickListener(onClickListener(1));
        btnMakeShot.setEnabled(false);

        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File fdelete = new File((images.get(viewPager.getCurrentItem()+ images.size() - 1)));
                if (fdelete.exists()) {
                    if (fdelete.delete()) {
                        //System.out.println("file Deleted :" + uri.getPath());
                        showToast("file Deleted: " + images.get(viewPager.getCurrentItem()+ images.size() - 1));
                    } else {
                        //System.out.println("file not Deleted :" + uri.getPath());
                        showToast("file not Deleted: " + images.get(viewPager.getCurrentItem()+ images.size() - 1));
                    }

                    images.remove(viewPager.getCurrentItem()+ images.size() - 1);


                    adapter.notifyDataSetChanged();
                    viewPager.setAdapter(adapter);

                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(fdelete);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);
                }
                else {
                    showToast("File no found!");
                }



                //showToast("Current photo: " + images.get(viewPager.getCurrentItem()+ images.size() - 1));
                //showToast("Current photo: " + viewPager.getCurrentItem());

            }
        });

        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(GalleryActivity2.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // init viewpager adapter and attach
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), images);
        viewPager.setAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);

    }


    private View.OnClickListener onClickListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i > 0) {
                    //next page
                    if (viewPager.getCurrentItem() < viewPager.getAdapter().getCount() - 1) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                    }
                } else {
                    //previous page
                    if (viewPager.getCurrentItem() > 0) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                    }
                }
            }
        };
    }

    private void setImagesData() {
        for (int i = 0; i < imagesPath.size(); i++) {
            images.add(imagesPath.get(i));
        }
    }

    private View.OnClickListener onChagePageClickListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(i);
            }
        };
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                MediaStore.Images.Media.DATA,
        };
        CursorLoader cursorLoader = new CursorLoader(this, MediaStore.Images.Media.getContentUri("external"), projection, null, null, null);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            //int columnIndex = data.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            data.moveToFirst();
            do {
                imagesPath.add(data.getString(data
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));

            } while (data.moveToNext());

            setImagesData();

            //inflateThumbnails();
        }
        //this.data = data;
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = GalleryActivity2.this;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                    Snackbar
                            .make(viewPager, text, Snackbar.LENGTH_LONG)
                            .show();
                }
            });
        }
    }
}
