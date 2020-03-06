package com.example.mycameraapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SaveToSdCardDialogFragment extends DialogFragment {

    public static String TAG = "SaveToSdCardDialogFragment";

    ImageView buttonYes;
    ImageView buttonNo;

    String inputPath = "";
    String inputFile = "";
    String outputPath = "";
    static DocumentFile pickedDir;
    // Уникальный код запроса на конкретное разрешение private
    private static int REQUEST_EXTERNAL_STORAGE = 1;

    public static SaveToSdCardDialogFragment getInstance() {
        SaveToSdCardDialogFragment f = new SaveToSdCardDialogFragment();
        Bundle args = new Bundle();

        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_save_sd_card, null);

        buttonYes = view.findViewById(R.id.btn_yes);
        buttonNo = view.findViewById(R.id.btn_no);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Dialog 1: ");
                dismiss();
                ((GalleryActivity2)getActivity()).onSaveToSDCardConfirm();
            }
        });

        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(SaveToSdCardDialogFragment.this).commit();
            }
        });
        OnClickAnimTouchListener clickAnim = new OnClickAnimTouchListener();
        clickAnim.scaleX = 0.9f;
        clickAnim.scaleY = 0.9f;

        buttonYes.setOnTouchListener(clickAnim);
        buttonNo.setOnTouchListener(clickAnim);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme);

        return view;
    }


    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(TAG, "Dialog 1: onDismiss");
    }

    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(TAG, "Dialog 1: onCancel");
    }



    public void updateFileGalleryInfo(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
