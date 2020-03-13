/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mycameraapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {

    private float mAspectRatio;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private int cameraId = 0;
    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(float aspectRatio, int cameraId) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException("aspectRatio >= 0");
        }
        this.cameraId = cameraId;
        mAspectRatio = aspectRatio;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setAspectFitDimensions(width, height);
//        if (0 == cameraId) {
//            setApectFillDimensions(width, height);
//        } else {
//            setAspectFitDimensions(width, height);
//        }
    }

    private void setAspectFitDimensions(int width, int height) {
        if (0 == mAspectRatio) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mAspectRatio) {
                setMeasuredDimension(width, (int) (width / mAspectRatio));
            } else {
                setMeasuredDimension((int) (height * mAspectRatio), height);
            }
        }
    }

    private void setApectFillDimensions(int width, int height) {
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