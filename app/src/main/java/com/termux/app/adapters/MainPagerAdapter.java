package com.termux.app.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.termux.app.fragments.DesktopFragment;
import com.termux.app.fragments.TerminalsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    private static final int NUM_TABS = 2;
    private static final int TAB_TERMINALS = 0;
    private static final int TAB_DESKTOP = 1;

    private TerminalsFragment mTerminalsFragment;
    private DesktopFragment mDesktopFragment;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_TERMINALS:
                if (mTerminalsFragment == null) {
                    mTerminalsFragment = new TerminalsFragment();
                }
                return mTerminalsFragment;
            case TAB_DESKTOP:
                if (mDesktopFragment == null) {
                    mDesktopFragment = new DesktopFragment();
                }
                return mDesktopFragment;
            default:
                return new TerminalsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }

    public TerminalsFragment getTerminalsFragment() {
        return mTerminalsFragment;
    }

    public DesktopFragment getDesktopFragment() {
        return mDesktopFragment;
    }
}