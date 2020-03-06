package com.example.mycameraapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DelFileDialogFragment extends DialogFragment {

    public static String TAG = "DelFileDialogFragment";

    ImageView buttonYes;
    ImageView buttonNo;

    String inputPath = "";
    String inputFile = "";
    String outputPath = "";

    public static DelFileDialogFragment getInstance(String inputFile) {
        DelFileDialogFragment f = new DelFileDialogFragment();
        Bundle args = new Bundle();
        args.putString("input_file", inputFile);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_delete_file, null);


        if (getArguments() != null) {
            //inputPath = getArguments().getString("input_path");
            inputFile = getArguments().getString("input_file");
            //outputPath = getArguments().getString("output_path");
        }

        buttonYes = view.findViewById(R.id.btn_yes);
        buttonNo = view.findViewById(R.id.btn_no);
        OnClickAnimTouchListener clickAnim = new OnClickAnimTouchListener();
        clickAnim.scaleX = 0.9f;
        clickAnim.scaleY = 0.9f;

        buttonYes.setOnTouchListener(clickAnim);
        buttonNo.setOnTouchListener(clickAnim);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Dialog 1: ");
                if (inputFile != "") {
                    //moveFile(inputPath, inputFile, outputPath);
                    ((GalleryActivity2)getActivity()).onFileDeleted(inputFile);

                }
                dismiss();
            }
        });

        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(DelFileDialogFragment.this).commit();
            }
        });

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
