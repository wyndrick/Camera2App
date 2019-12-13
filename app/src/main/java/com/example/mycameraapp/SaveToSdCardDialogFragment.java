package com.example.mycameraapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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

    public static SaveToSdCardDialogFragment getInstance(String inputPath, String inputFile, String outputPath) {
        SaveToSdCardDialogFragment f = new SaveToSdCardDialogFragment();
        Bundle args = new Bundle();
        args.putString("input_path", inputPath);
        args.putString("output_path", outputPath);
        args.putString("input_file", inputFile);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_save_sd_card, null);


        if (getArguments() != null) {
            inputPath = getArguments().getString("input_path");
            inputFile = getArguments().getString("input_file");
            outputPath = getArguments().getString("output_path");
        }

        buttonYes = view.findViewById(R.id.btn_yes);
        buttonNo = view.findViewById(R.id.btn_no);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Dialog 1: ");
                if (inputPath != "" && inputFile != "" && outputPath != "") {
                    moveFile(inputPath, inputFile, outputPath);
                    ((GalleryActivity2)getActivity()).onFileMoved(inputPath, inputFile, outputPath);

                }
                dismiss();
            }
        });

        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(SaveToSdCardDialogFragment.this).commit();
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
        getActivity().sendBroadcast(mediaScanIntent);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
