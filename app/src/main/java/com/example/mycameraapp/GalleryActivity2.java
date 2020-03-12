package com.example.mycameraapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

import androidx.viewpager.widget.ViewPager;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
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
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
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
import java.util.concurrent.TimeUnit;

public class GalleryActivity2 extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public boolean SHOW_SD_CARD_BUTTON = false;
    private static GalleryActivity2 instance;
    private String removableStoragePath = "";
    private ArrayList<String> images;
    private ArrayList<String> imagesPath;
    private BitmapFactory.Options options;
    private ViewPager viewPager;
    private ImageButton btnNext, btnPrev, btnDel, btnCam, btnCard, btnMakeShot, btnCardProblem;
    private ViewPagerAdapter adapter;

    NotEnoughMemoryDialogFragment notEnoughMemoryDialogFragment;
    SaveToSdCardDialogFragment saveToSdCardDialog;
    InsertSDCardDialogFragment insertSDCardDialog;
    DelFileDialogFragment delFileDialog;

    public static final String LOG_TAG = "myLogs";
    int mCurrentItem = 0;
    private String uriString;
    private boolean isAccessWriteSD;
    private SDCardBroadcastReceiver receiver;
    private Context act;
    boolean mNotEnoguhMemory = false;

    private boolean mIsSDCardInserted = false;
    private boolean mIsEnoughSpace = false;
    private long freeSpace = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_gallery2);

        // Hide status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        images = new ArrayList<>();
        imagesPath = new ArrayList<>();

        //find view by id
        viewPager = findViewById(R.id.view_pager);
        btnNext = findViewById(R.id.btn_right);
        btnPrev = findViewById(R.id.btn_left);

        btnDel = findViewById(R.id.del_photo);
        btnCam = findViewById(R.id.open_camera);
        //btnMakeShot = (ImageView)findViewById(R.id.make_shot);
        btnCard = findViewById(R.id.save_sd);
        btnCardProblem = findViewById(R.id.save_sd_problem);

        btnPrev.setOnClickListener(onClickListener(0));
        btnNext.setOnClickListener(onClickListener(1));

        btnPrev.setVisibility(View.INVISIBLE);

        OnClickAnimTouchListener clickAnim = new OnClickAnimTouchListener();

        btnPrev.setOnTouchListener(clickAnim);
        btnCardProblem.setOnTouchListener(clickAnim);
        btnCard.setOnTouchListener(clickAnim);
        btnDel.setOnTouchListener(clickAnim);
        btnCam.setOnTouchListener(clickAnim);

        OnClickAnimTouchListener clickAnim2 = new OnClickAnimTouchListener();
        clickAnim2.scaleX = -0.8f;
        clickAnim2.scaleXDefault = -1f;
        btnNext.setOnTouchListener(clickAnim2);

        mIsEnoughSpace = true;

        updateSDCardButtons();

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

        btnCardProblem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsSDCardInserted) {
                    insertSDCardDialog = InsertSDCardDialogFragment.getInstance();
                    insertSDCardDialog.show(getSupportFragmentManager(), "dialog_insert_sd_card");
                } else {
                    notEnoughMemoryDialogFragment = NotEnoughMemoryDialogFragment.getInstance();
                    notEnoughMemoryDialogFragment.show(getSupportFragmentManager(), "dialog_not_enough_memory");
                }

            }
        });

        btnCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (images.size()!= 0) {

                    saveToSdCardDialog = SaveToSdCardDialogFragment.getInstance();
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

    public void setSDCardMounted(boolean mounted) {
        mIsSDCardInserted = mounted;
        if (mounted) {
            updateSDCardButtons();
        } else {
            mIsEnoughSpace = true;
            setSDCardVisible(mounted);
        }
    }
    public void setSDCardVisible(boolean visible) {

        if (visible) {
            btnCard.setVisibility(SHOW_SD_CARD_BUTTON ? View.VISIBLE : View.GONE);
            btnCardProblem.setVisibility(View.GONE);
        } else {
            btnCardProblem.setVisibility(SHOW_SD_CARD_BUTTON ? View.VISIBLE : View.GONE);
            btnCard.setVisibility(View.GONE);
        }
    }

    public void updateSDCardButtons() {
        mIsEnoughSpace = true;
        mIsSDCardInserted = false;
        File fileList[] = new File("/storage/").listFiles();
        for (File file : fileList) {
            String name = file.getName();
            if (file.isDirectory() && !name.equals("emulated") && !name.equals("self")) {
                mIsSDCardInserted = true;
                long freeSpace = file.getFreeSpace();
                long spaceRequired = getImagesTotalLength(images);
                if (freeSpace < spaceRequired) {
                    mIsEnoughSpace = false;
                }
            }
        }

        setSDCardVisible(mIsSDCardInserted && mIsEnoughSpace);
    }

    Handler moveFilesHandler;
    private int imagesTotalCount = 0;;
    private int imageMovedCount = 0;

    ProgressDialog progressDialog;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK)
            return;
        else {
            Uri treeUri = resultData.getData();
            final DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri docTreeUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
            );
            long freeSpace = 0;
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(docTreeUri, "r");
                assert pfd != null;
                StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
                freeSpace = stats.f_bavail * stats.f_bsize;
                Log.i(LOG_TAG, "block_size=" + stats.f_bsize + ", num_of_blocks=" + stats.f_bavail);
                Log.i(LOG_TAG, "free space in Megabytes:" + stats.f_bavail * stats.f_bsize / 1024 / 1024);
            } catch (FileNotFoundException | ErrnoException e) {
                Log.e(LOG_TAG, Log.getStackTraceString(e));
            }

            long totalSize = getImagesTotalLength(images);

            if (totalSize < freeSpace) {
                imagesTotalCount = images.size();
                progressDialog = new ProgressDialog(GalleryActivity2.this);
                progressDialog.show();
                moveFilesHandler = new Handler();
                Thread t = new Thread(new Runnable() {
                    public void run() {
                    try {
                        for (int i = 0; i < images.size(); i++) {
                            //                int itemIndex = images.size() - viewPager.getCurrentItem() - 1;

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

                            String outputPath = "/PebbleGear";//uriString; //removableStoragePath + "/DCIM/Camera";

                            String filepath = images.get(i);
                            File f = new File(filepath);
                            String filename = f.getName();
                            String inputPath = f.getParentFile().getAbsolutePath();
                            Log.i(LOG_TAG, "filepath = " + filepath);
                            Log.i(LOG_TAG, "filename = " + filename);
                            Log.i(LOG_TAG, "inputFolder = " + inputPath);
                            Log.i(LOG_TAG, "outputPath = " + outputPath);

                            if (copyFile(inputPath, filename, outputPath, pickedDir)) {
//                                    onFileMoved(i);
                            }

                            imageMovedCount = i + 1;
                            TimeUnit.MILLISECONDS.sleep(20);
                            moveFilesHandler.post(updateProgress);
                        }
                        progressDialog.dismiss();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        progressDialog.dismiss();

                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            viewPager.setAdapter(adapter);
                            updateButtons(0);
                            showToast(getString(R.string.files_moved));
                        }
                    });
                    }
                });
                t.start();
            } else {
                mNotEnoguhMemory = true;
//                showToast(String.format(getString(R.string.files_not_enough_space), totalSize / 1024 / 1024 + 1 , freeSpace / 1024 / 1024));
            }
        }
    }

    // обновление ProgressBar
    Runnable updateProgress = new Runnable() {
        public void run() {
            if (progressDialog != null) {
                progressDialog.setMessage(String.format(getString(R.string.file_move_progress), imageMovedCount, imagesTotalCount));
            }
        }
    };

    public int getImagesTotalLength(ArrayList<String> filenames) {
        int totalSize = 0;

        for (int i = 0; i < images.size(); i++) {
            String filepath = images.get(i);
            File f = new File(filepath);
            String filename = f.getName();
            String inputPath = f.getParentFile().getAbsolutePath();
            totalSize += f.length();
        }


        Log.i(LOG_TAG, "totalSize = " + totalSize);
        return totalSize;
    }




    private boolean copyFile(String inputPath, String inputFile, String outputPath, DocumentFile pickedDir) {
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(inputPath + "/" + inputFile);
            DocumentFile documentFile = pickedDir.findFile(inputFile);
            if (documentFile != null) {
                documentFile.delete();
            }

            DocumentFile file = pickedDir.createFile("//MIME type", inputFile);
            out = getContentResolver().openOutputStream(file.getUri());

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

            // delete the original file
            File fileToDelete = new File(inputPath + "/" + inputFile);
            File fileNew = new File(outputPath + "/" + inputFile);

            updateFileGalleryInfo(fileToDelete);
            updateFileGalleryInfo(fileNew);

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

    public void onSaveToSDCardConfirm() {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 2);
    }

    private boolean moveFile(String inputPath, String inputFile, String outputPath, DocumentFile pickedDir) {
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(inputPath + "/" + inputFile);

            DocumentFile file = pickedDir.createFile("//MIME type", inputFile);
            out = getContentResolver().openOutputStream(file.getUri());

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

            // delete the original file
            File fileToDelete = new File(inputPath + "/" + inputFile);
            File fileNew = new File(outputPath + "/" + inputFile);
            fileToDelete.delete();

            updateFileGalleryInfo(fileToDelete);
            updateFileGalleryInfo(fileNew);

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

    public void updateFileGalleryInfo(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
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
            updateFileGalleryInfo(fdelete);

            adapter.notifyDataSetChanged();
            viewPager.setAdapter(adapter);

        }
        else {
            showToast(getString(R.string.file_no_found));
        }
        //showToast("Current photo: " + images.get(viewPager.getCurrentItem()+ images.size() - 1));
        //showToast("Current photo: " + viewPager.getCurrentItem());
    }

    public void onFileMoved(int itemIndex) {

//        int itemIndex = images.size() - viewPager.getCurrentItem() - 1;
//        images.remove(itemIndex);
//        if (isNeedToShowMedia(outputPath + "/" + inputFile)) {
//            images.set(itemIndex, outputPath + "/" + inputFile);
//        } else {
//            images.remove(itemIndex);
//        }

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
//            btnCard.setEnabled(true);
            btnCard.setEnabled(SHOW_SD_CARD_BUTTON); // without SD-Card UI Symbol
            btnCard.setImageAlpha(true ? 0xFF : 0x3F);


//            //sd-карта
//            File fileList[] = new File("/storage/").listFiles();
//            for (File file : fileList)
//            {     if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
//                removableStoragePath = file.getAbsolutePath();
//            }
//            //removableStoragePath = getSDcardPath();
//            //removableStoragePath = getSDPath();
//            //removableStoragePath = getMountedPaths();
//
//            //ArrayList<String> testList = getMountedPaths();
//
//            //showToast(removableStoragePath);
//
//            //String filepath = images.get(itemIndex);
//            File f = new File(filePath);
//            String filename = f.getName();
//            String inputPath = f.getParentFile().getAbsolutePath();
//            String outputPath = removableStoragePath;
//
//            if (moveFile(inputPath, filename, outputPath)) {
//                isAccessWriteSD = true;
//                showToast("sd-card write available");
//            } else {
//                isAccessWriteSD = false;
//                showToast("sd-card not write available");
//            }
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
                && file.exists() && (path.contains("photo") || path.contains("video"));
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
        if(receiver != null) {
            this.unregisterReceiver(receiver);
        }
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
// play with fragments here
        if (mNotEnoguhMemory) {
            mNotEnoguhMemory = false;

            notEnoughMemoryDialogFragment = NotEnoughMemoryDialogFragment.getInstance();
            notEnoughMemoryDialogFragment.show(getSupportFragmentManager(), "dialog_not_enough_memory");
        }
        registerReceiver(this);
    }

    public void registerReceiver(Context context) {
        try {
            if(receiver == null)
                receiver = new SDCardBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            Intent sticky = context.registerReceiver(receiver, filter);
            if (sticky != null) {
                receiver.onReceive(context, sticky);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class SDCardBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
        private static final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
        private static final String MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL";
        private static final String MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";
        private static final String TAG = "SDCardBroadcastReceiver";
        public SDCardBroadcastReceiver() {

        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Intent recieved: " + intent.getAction());

            if (intent.getAction() == ACTION_MEDIA_MOUNTED) {
                GalleryActivity2.instance.setSDCardMounted(true);

            } else if (intent.getAction() == ACTION_MEDIA_REMOVED){
                GalleryActivity2.instance.setSDCardMounted(false);

            } else if(intent.getAction() == MEDIA_BAD_REMOVAL){
                GalleryActivity2.instance.setSDCardMounted(false);

            } else if (intent.getAction() == MEDIA_EJECT){
                GalleryActivity2.instance.setSDCardMounted(false);
            }
        }
    }
}
