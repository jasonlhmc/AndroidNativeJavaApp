package com.example.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.fragments.FragmentMenuNews;
import com.example.fragments.MenuWeatherPageFragment;

public class MenuViewPager2Adapter  extends FragmentStateAdapter {

    public MenuViewPager2Adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        switch (position) {
            case 1:
                return new MenuWeatherPageFragment();
            default:
                return new FragmentMenuNews();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
