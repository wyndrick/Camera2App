package com.example.mycameraapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

import androidx.viewpager.widget.ViewPager;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GalleryActivity2 extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private String removableStoragePath = "";
    private ArrayList<String> images;
    private ArrayList<String> imagesPath;
    private BitmapFactory.Options options;
    private ViewPager viewPager;
    private ImageView btnNext, btnPrev, btnDel, btnCam, btnCard, btnMakeShot, btnCardProblem;
    private ViewPagerAdapter adapter;

    SaveToSdCardDialogFragment saveToSdCardDialog;
    DelFileDialogFragment delFileDialog;

    public static final String LOG_TAG = "myLogs";
    int mCurrentItem = 0;
    private String uriString;
    private boolean isAccessWriteSD;
    private SDCardInsertionReceiver receiver;
    private Context act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery2);

        // Hide status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        images = new ArrayList<>();
        imagesPath = new ArrayList<>();

        //find view by id
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        btnNext = (ImageView)findViewById(R.id.btn_right);
        btnPrev = (ImageView)findViewById(R.id.btn_left);

        btnDel = (ImageView)findViewById(R.id.del_photo);
        btnCam = (ImageView)findViewById(R.id.open_camera);
        //btnMakeShot = (ImageView)findViewById(R.id.make_shot);
        btnCard = (ImageView)findViewById(R.id.save_sd);
        btnCardProblem = (ImageView)findViewById(R.id.save_sd_problem);

        btnPrev.setOnClickListener(onClickListener(0));
        btnNext.setOnClickListener(onClickListener(1));

        btnPrev.setVisibility(View.INVISIBLE);




//        File fileList[] = new File("/storage/").listFiles();
//        for (File file : fileList)
//        {     if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
//            removableStoragePath = file.getAbsolutePath();
//        }
//        //If there is an SD Card, removableStoragePath will have it's path. If there isn't it will be an empty string.
//        Log.d(LOG_TAG, "StoragePath = " + removableStoragePath);
//
//
//        if(removableStoragePath == null) {
//            //Log.d("test", "sdcard not available");
//            showToast("SD-card not available");
//            btnCard.setVisibility(View.GONE);
//            btnCardProblem.setVisibility(View.VISIBLE);
//        }
//        else {
//            showToast(removableStoragePath);
//            btnCard.setVisibility(View.VISIBLE);
//            btnCardProblem.setVisibility(View.GONE);
//        }


        /* Получаем базовый путь */
        // пытаемся найти карты памяти
//        ArrayList<StorageHelper.MountDevice> storages = StorageHelper.getInstance()
//                    .getRemovableMountedDevices();
//        // проверяем съемные карты памяти
//        if (storages.size() != 0) {
//            //setBasePath(storages.get(0).getPath() + mAppPath);
//            showToast(storages.get(0).getPath());
//        } else if ((storages = StorageHelper.getInstance() // Проверяем
//                    // внутреннюю
//                    // память
//                    .getExternalMountedDevices()).size() != 0) {
//            //setBasePath(storages.get(0).getPath() + mAppPath);
//            showToast(storages.get(0).getPath());
//        }

        //btnMakeShot.setVisibility(View.GONE);


        updateButtons(0);


        if (isAccessWriteSD) {
            removableStoragePath = getSDcardPath() + "/PebbleGear" ;
        }


        btnCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (images.size()!= 0) {
                    int itemIndex = images.size() - viewPager.getCurrentItem() - 1;

//                    Map<String, File> externalLocations = ExternalStorage.getAllStorageLocations();
//                    File sdCard = externalLocations.get(ExternalStorage.SD_CARD);
//                    File externalSdCard = externalLocations.get(ExternalStorage.EXTERNAL_SD_CARD);
//
//                    for (Map.Entry<String, File> entry : externalLocations.entrySet()) {
//                        Log.i(LOG_TAG, "entry = " + entry.getKey() + " val = " + entry.getValue().getAbsolutePath());
//                    }
//                    Log.i(LOG_TAG, "sdCard = " + sdCard);
//                    Log.i(LOG_TAG, "externalSdCard = " + externalSdCard);
//
                    //String outputPath = sdCard + "/DCIM";

                    //String outputPath = Environment.getExternalStorageState()+ "/DCIM";
                    //String outputPath = Environment.getExternalStorageDirectory() + "/SDCamera";

                    String outputPath = removableStoragePath; // "/PebbleGear";//uriString; //removableStoragePath + "/DCIM/Camera";

                    Log.i(LOG_TAG, "Environment.getExternalStorageState() = " + outputPath);
                    String filepath = images.get(itemIndex);
                    File f = new File(filepath);
                    String filename = f.getName();
                    String inputPath = f.getParentFile().getAbsolutePath();
                    Log.i(LOG_TAG, "filepath = " + filepath);
                    Log.i(LOG_TAG, "filename = " + filename);
                    Log.i(LOG_TAG, "inputFolder = " + inputPath);
                    Log.i(LOG_TAG, "outputPath = " + outputPath);

                    saveToSdCardDialog = SaveToSdCardDialogFragment.getInstance(inputPath, filename, outputPath);
                    saveToSdCardDialog.show(getSupportFragmentManager(), "dialog_save_sd_card");
                }
            }
        });


        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            final int currentItemIndex = viewPager.getCurrentItem();
            int itemIndex = images.size() - currentItemIndex - 1;
            Log.d(LOG_TAG, "viewPager.getCurrentItem() = " + currentItemIndex);
            Log.d(LOG_TAG, "images.size() = " + images.size());
            Log.d(LOG_TAG, "viewPager.getCurrentItem() + images.size() - 1 = " + (itemIndex));
            File fdelete = new File((images.get(itemIndex)));

            delFileDialog = DelFileDialogFragment.getInstance(images.get(itemIndex));
            delFileDialog.show(getSupportFragmentManager(), "dialog_save_sd_card");

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

        // init viewpager adapter and attach
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), images);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateButtons(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(5);
        getLoaderManager().initLoader(0, null, this);

    }

    //Функция определяет путь до внешней извлекаемой карты
    // наподобие /storage/extSdCard
    private String getSDcardPath()
    {
        String exts =  Environment.getExternalStorageDirectory().getPath();
        String sdCardPath = null;
        try
        {
            FileReader fr = new FileReader(new File("/proc/mounts"));
            BufferedReader br = new BufferedReader(fr);
            String line;
            while((line = br.readLine())!=null)
            {
                if(line.contains("secure") || line.contains("asec"))
                    continue;
                if(line.contains("fat"))
                {
                    String[] pars = line.split("\\s");
                    if(pars.length<2)
                        continue;
                    if(pars[1].equals(exts))
                        continue;
                    sdCardPath =pars[1];
                    break;
                }
            }
            fr.close();
            br.close();
            return sdCardPath;
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            //textInfo.setText(e.toString());
        }
        return sdCardPath;
    }

    static public ArrayList<String> getMountedPaths() {
        ArrayList<String> list = new ArrayList<>();

        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[1];

                        if (!element.equals("/mnt/sdcard")) {
                            list.add(element);
                        }
                    }
                }
            }
        } catch(Exception e) {
            Log.d("MyLogs", "exception", e);
        }

        return list;
    }

    static String getSDPath() {
        ArrayList<String> mountList = getMountedPaths();

        for(String mpath : mountList) {
            final String prefix = "/mnt/media_rw";
            if(mpath.startsWith(prefix))
                return "/storage"+mpath.substring(prefix.length());
        }

        return null;
    }

    private boolean moveFile(String inputPath, String inputFile, String outputPath) {
        InputStream in = null;
        OutputStream out = null;

        try {
            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            in = new FileInputStream(inputPath + "/" + inputFile);
            out = new FileOutputStream(outputPath + "/" + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            return true;
        }

        catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
        return false;
    }

    public void onFileDeleted(String inputFile) {

        int itemIndex = images.size() - viewPager.getCurrentItem() - 1;

        File fdelete = new File(inputFile);

        if (fdelete.exists()) {
            if (fdelete.delete()) {
                //System.out.println("file Deleted :" + uri.getPath());
                showToast(getString(R.string.file_delete) + images.get(itemIndex));
            } else {
                //System.out.println("file not Deleted :" + uri.getPath());
                showToast(getString(R.string.file_not_delete) + images.get(itemIndex));
            }
            images.remove(itemIndex);

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(fdelete);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);

            adapter.notifyDataSetChanged();
            viewPager.setAdapter(adapter);

        }
        else {
            showToast(getString(R.string.file_no_found));
        }
        //showToast("Current photo: " + images.get(viewPager.getCurrentItem()+ images.size() - 1));
        //showToast("Current photo: " + viewPager.getCurrentItem());
    }

    public void onFileMoved(String inputPath, String inputFile, String outputPath) {
        int itemIndex = images.size() - viewPager.getCurrentItem() - 1;
        if (isNeedToShowMedia(outputPath + "/" + inputFile)) {
            images.set(itemIndex, outputPath + "/" + inputFile);
        } else {
            images.remove(itemIndex);
        }
        adapter.notifyDataSetChanged();
        viewPager.setAdapter(adapter);

        updateButtons(viewPager.getCurrentItem());
    }

    public void updateButtons(int position){
        btnPrev.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setVisibility(position == images.size() - 1 || images.size() == 0  ? View.INVISIBLE : View.VISIBLE);
        btnDel.setVisibility(images.size() == 0  ? View.INVISIBLE : View.VISIBLE);

        int itemIndex = images.size() - viewPager.getCurrentItem() - 1;
        if (images.size() > itemIndex && images.size() > 0 ){
            String filePath = images.get(itemIndex);
            //btnCard.setEnabled(filePath.contains("PebbleGear") ? false : true);
            //btnCard.setImageAlpha(false ? 0xFF : 0x3F);
            btnCard.setEnabled(true);
            btnCard.setImageAlpha(true ? 0xFF : 0x3F);

            //sd-карта
            File fileList[] = new File("/storage/").listFiles();
            for (File file : fileList)
            {     if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
                removableStoragePath = file.getAbsolutePath();
            }
            //removableStoragePath = getSDcardPath();
            //removableStoragePath = getSDPath();
            //removableStoragePath = getMountedPaths();

            //ArrayList<String> testList = getMountedPaths();

            //showToast(removableStoragePath);

            //String filepath = images.get(itemIndex);
            File f = new File(filePath);
            String filename = f.getName();
            String inputPath = f.getParentFile().getAbsolutePath();
            String outputPath = removableStoragePath;

            if (moveFile(inputPath, filename, outputPath)) {
                isAccessWriteSD = true;
                showToast("sd-card write available");
            } else {
                isAccessWriteSD = false;
                showToast("sd-card not write available");
            }
        } else {
            //btnCard.setVisibility(images.size() == 0  ? View.INVISIBLE : View.VISIBLE);
            btnCard.setEnabled(false);
            btnCard.setImageAlpha(false ? 0xFF : 0x3F);
        }

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
        images.clear();
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
        // Get relevant columns for use later.
        String[] projection = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE
        };
        // Return only video and image metadata.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
            + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            + " OR "
            + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
            + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;


        Uri queryUri = MediaStore.Files.getContentUri("external");

        CursorLoader cursorLoader = new CursorLoader(
                this,
                queryUri,
                projection,
                selection,
                null, // Selection args (none).
                MediaStore.Files.FileColumns.DATE_MODIFIED
        );

        return cursorLoader;
    }

    public boolean isNeedToShowMedia(String path) {
        File file = new File(path);
        return (path.toLowerCase().contains("/dcim/camera")
                || path.toLowerCase().contains("/dcim/100andro"))
                && file.exists() && path.contains("photo") || path.contains("video");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        imagesPath.clear();
        if (data != null) {
            if (data.moveToFirst()) {
                do {
                    String path = data.getString(data.getColumnIndex(MediaStore.Video.Media.DATA));
                    if (isNeedToShowMedia(path)) {
                        imagesPath.add(path);

//                        if (path.contains("PebbleGear")) {
//                            btnCard.setEnabled(false);
//                            btnCard.setImageAlpha(false ? 0xFF : 0x3F);
//                        } else {
//                            btnCard.setEnabled(true);
//                            btnCard.setImageAlpha(true ? 0xFF : 0x3F);
//                        }
                    }

                } while (data.moveToNext());

                setImagesData();
            }
            //inflateThumbnails();
        }
        //this.data = data;
        adapter.notifyDataSetChanged();
        int index = viewPager.getCurrentItem();
        updateButtons(index);
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

    @Override
    public void onPause() {
        if(receiver != null)
            act.unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(this);
    }

    public void registerReceiver(Context context) {
        try {
            if(receiver == null)
                receiver = new SDCardInsertionReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            Intent sticky = context.registerReceiver(receiver, filter);
            if (sticky != null) {
                receiver.onReceive(context, sticky);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class SDCardInsertionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {

                //   ..  card was inserted ..
                showToast("card was inserted");
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {

                //   ..  card was removed ..
                showToast("card was removed");
            }
        }
    }
}
