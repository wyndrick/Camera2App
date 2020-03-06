package com.example.mycameraapp;

import android.animation.TimeInterpolator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class OnClickAnimTouchListener implements View.OnTouchListener {

    private TimeInterpolator interpolator= new DecelerateInterpolator();
    public float scaleX = 0.8f;
    public float scaleY = 0.8f;
    public float scaleXDefault = 1f;
    public float scaleYDefault = 1f;
    public int duration = 150;

    public void setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.animate().scaleX(scaleX).scaleY(scaleY).setDuration(duration).setInterpolator(interpolator);
                v.setPressed(true);
                break;

            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                boolean isInside = (x > 0 && x < v.getWidth() && y > 0 && y < v.getHeight());
                if (v.isPressed() != isInside) {
                    v.setPressed(isInside);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                v.setPressed(false);
                v.animate().scaleX(scaleXDefault).scaleY(scaleYDefault).setInterpolator(interpolator);
                break;
            case MotionEvent.ACTION_UP:
                v.animate().scaleX(scaleXDefault).scaleY(scaleYDefault).setInterpolator(interpolator);
                if (v.isPressed()) {
                    v.performClick();
                    v.setPressed(false);
                }
                break;
        }
        return true;
    }
}