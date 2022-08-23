package com.example.myapplicationmaptest2;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StoreViewPaperAdapter extends FragmentStateAdapter {
    public int mCount;
    public StoreViewPaperAdapter(@NonNull FragmentActivity fragmentActivity, int count) {
        super(fragmentActivity);
        mCount = count;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new StoreFragment();
    }

    @Override
    public int getItemCount() {
        return 20;
    }
}
