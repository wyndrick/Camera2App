package com.example.mycameraapp;

import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<String> images;
    protected Hashtable<Integer, WeakReference<Fragment>> fragmentReferences;


    public ViewPagerAdapter(FragmentManager fm, ArrayList<String> imagesPath) {
        super(fm);
        this.images = imagesPath;
    }

    @Override
    public Fragment getItem(int position) {
        String path = images.get(images.size() - (position + 1));
        PageFragment fragment = PageFragment.getInstance(images.get(images.size() - (position + 1)));
        fragment.isVideo = path.contains(".mp4");
        return fragment;
    }



    @Override
    public int getCount() {
        return images.size();
    }

    private SparseArray<PageFragment> mFragmentsHolded = new SparseArray<>();


    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object fragment = super.instantiateItem(container, position);
        if(fragment instanceof PageFragment) {
            mFragmentsHolded.append(position, (PageFragment) fragment);
        }
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mFragmentsHolded.delete(position);
        super.destroyItem(container, position, object);
    }

    public PageFragment getCachedItem(int position) {
        return mFragmentsHolded.get(position, null);
    }


}
