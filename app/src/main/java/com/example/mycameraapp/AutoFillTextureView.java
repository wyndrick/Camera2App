package com.example.mycameraapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFillTextureView extends TextureView {
    private float mAspectRatio;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    public AutoFillTextureView(Context context) {
        super(context);
    }

    public AutoFillTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFillTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public void setAspectRatio(float aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException("aspectRatio >= 0");
        }
        mAspectRatio = aspectRatio;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setDimensionWithAspectRatio(width, height);
    }
    private void setDimensionWithAspectRatio(int width, int height) {
        if (mAspectRatio <= 0) {
            setMeasuredDimension(width, height);
        } else {
            // соотношение размеров предварительного просмотра больше, чем 1.0f,
            // имеем в виду текстурную высоту width <height
            if (mAspectRatio < 1.0f) {
                // need texture' s width < height
                if (width > height) {
                    // width can be match, height should be larger than width
                    setMeasuredDimension(width, (int) (width * mAspectRatio));
                } else {
                    // высота может совпадать, ширина должна быть меньше высоты
                    setMeasuredDimension((int) (height / mAspectRatio), height);
                }
            } else {
                // need texture' width > height
                if (width > height) {
                    // width can be match, height should be larger than width
                    setMeasuredDimension(width, (int) (width / mAspectRatio));
                } else {
                    // height can be match, width should be smaller than height
                    setMeasuredDimension((int) (height * mAspectRatio), height);
                }
            }
        }
    }

}