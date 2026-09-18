package com.termux.app.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.termux.app.fragments.DesktopFragment;
import com.termux.app.fragments.TerminalsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    private static final int NUM_TABS = 2;
    private static final int TAB_TERMINALS = 0;
    private static final int TAB_DESKTOP = 1;

    private TerminalsFragment mTerminalsFragment;
    private DesktopFragment mDesktopFragment;

    private final FragmentActivity mFragmentActivity;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        mFragmentActivity = fragmentActivity;
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

    @Nullable
    public TerminalsFragment getTerminalsFragment() {
        if (mTerminalsFragment != null && mTerminalsFragment.isAdded()) {
            return mTerminalsFragment;
        }
        // On activity recreation the fragments are restored by the FragmentManager and
        // createFragment() is not called, so look them up by tag (FragmentStateAdapter
        // uses "f" + itemId as the fragment tag).
        FragmentManager fm = mFragmentActivity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag("f" + TAB_TERMINALS);
        if (fragment instanceof TerminalsFragment) {
            mTerminalsFragment = (TerminalsFragment) fragment;
            return mTerminalsFragment;
        }
        return null;
    }

    @Nullable
    public DesktopFragment getDesktopFragment() {
        if (mDesktopFragment != null && mDesktopFragment.isAdded()) {
            return mDesktopFragment;
        }
        FragmentManager fm = mFragmentActivity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag("f" + TAB_DESKTOP);
        if (fragment instanceof DesktopFragment) {
            mDesktopFragment = (DesktopFragment) fragment;
            return mDesktopFragment;
        }
        return null;
    }
}