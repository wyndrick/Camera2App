package com.example.mycameraapp;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<String> images;

    public ViewPagerAdapter(FragmentManager fm, ArrayList<String> imagesPath) {
        super(fm);
        this.images = imagesPath;
    }

    @Override
    public Fragment getItem(int position) {

        return PageFragment.getInstance(images.get(images.size() - (position + 1)));
    }

    @Override
    public int getCount() {
        return images.size();
    }

}
