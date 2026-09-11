package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.view.TerminalView;

public class TerminalsFragment extends Fragment {

    private TermuxActivity mActivity;
    private TerminalView mTerminalView;
    private TermuxTerminalViewClient mTermuxTerminalViewClient;
    private TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;
    private TermuxSessionsListViewController mTermuxSessionListViewController;
    private DrawerLayout mDrawerLayout;
    private View mRootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_terminals, container, false);
        mActivity = (TermuxActivity) requireActivity();

        setupViews();
        return mRootView;
    }

    private void setupViews() {
        mTerminalView = mRootView.findViewById(R.id.terminal_view);
        mDrawerLayout = mRootView.findViewById(R.id.drawer_layout);

        // Set up terminal view clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(mActivity);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(mActivity, mTermuxTerminalSessionActivityClient);

        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.onCreate();
        }

        if (mTermuxTerminalSessionActivityClient != null) {
            mTermuxTerminalSessionActivityClient.onCreate();
        }

        // Set up sessions list
        ListView termuxSessionsListView = mRootView.findViewById(R.id.terminal_sessions_list);
        mTermuxSessionListViewController = new TermuxSessionsListViewController(mActivity, mActivity.getTermuxService().getTermuxSessions());
        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);

        // Set up drawer buttons
        ImageButton settingsButton = mRootView.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            mActivity.startActivity(new android.content.Intent(mActivity, com.termux.app.activities.SettingsActivity.class));
        });

        mRootView.findViewById(R.id.new_session_button).setOnClickListener(v ->
            mTermuxTerminalSessionActivityClient.addNewSession(false, null));

        mRootView.findViewById(R.id.new_session_button).setOnLongClickListener(v -> {
            com.termux.shared.termux.interact.TextInputDialogUtils.textInput(mActivity,
                R.string.title_create_named_session, null,
                R.string.action_create_named_session_confirm,
                text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                R.string.action_new_session_failsafe,
                text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                -1, null, null);
            return true;
        });

        mRootView.findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {
            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
            mDrawerLayout.closeDrawers();
        });

        mRootView.findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {
            mActivity.toggleTerminalToolbar();
            return true;
        });

        // Register for context menu
        mActivity.registerForContextMenu(mTerminalView);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mTermuxTerminalSessionActivityClient != null) {
            mTermuxTerminalSessionActivityClient.onStart();
        }
        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTermuxTerminalSessionActivityClient != null) {
            mTermuxTerminalSessionActivityClient.onResume();
        }
        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.onResume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mTermuxTerminalSessionActivityClient != null) {
            mTermuxTerminalSessionActivityClient.onStop();
        }
        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.onDestroy();
        }
        if (mTermuxTerminalSessionActivityClient != null) {
            mTermuxTerminalSessionActivityClient.onDestroy();
        }
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    public TermuxSessionsListViewController getTermuxSessionListViewController() {
        return mTermuxSessionListViewController;
    }

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }
}