package com.bloatwareremoval.samsung;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Main app manager activity - easy to use interface
 */
public class MainActivity extends AppCompatActivity {

    private SamsungBloatwareManager mBloatwareManager;
    private AppListAdapter mAdapter;
    private ListView mAppListView;
    private ProgressBar mLoadingProgress;
    private RadioButton mRadioSuggested;
    private RadioButton mRadioAll;
    private RadioButton mRadioSystem;
    private Button mAutoSelectButton;
    private Button mUninstallButton;
    private TextView mStorageInfo;
    private int mCurrentMode = 0; // 0: suggested, 1: all, 2: system

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBloatwareManager = new SamsungBloatwareManager(this);
        initializeViews();
        loadSuggestedBloatware();
    }

    private void initializeViews() {
        mAppListView = findViewById(R.id.app_list_view);
        mLoadingProgress = findViewById(R.id.loading_progress);
        mRadioSuggested = findViewById(R.id.radio_suggested);
        mRadioAll = findViewById(R.id.radio_all_apps);
        mRadioSystem = findViewById(R.id.radio_system);
        mAutoSelectButton = findViewById(R.id.auto_select_button);
        mUninstallButton = findViewById(R.id.uninstall_button);
        mStorageInfo = findViewById(R.id.storage_info);

        mRadioSuggested.setChecked(true);

        // Radio buttons
        mRadioSuggested.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mCurrentMode = 0;
                loadSuggestedBloatware();
            }
        });

        mRadioAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mCurrentMode = 1;
                loadAllApps();
            }
        });

        mRadioSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mCurrentMode = 2;
                loadSystemApps();
            }
        });

        // Buttons
        mAutoSelectButton.setOnClickListener(v -> selectAllBloatware());
        mUninstallButton.setOnClickListener(v -> uninstallSelected());
    }

    private void showLoading(boolean show) {
        mLoadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void loadSuggestedBloatware() {
        showLoading(true);
        new Thread(() -> {
            List<SamsungBloatwareManager.AppInfo> apps = mBloatwareManager.getSuggestedBloatware();
            runOnUiThread(() -> updateAppList(apps));
        }).start();
    }

    private void loadAllApps() {
        showLoading(true);
        new Thread(() -> {
            List<SamsungBloatwareManager.AppInfo> apps = mBloatwareManager.getAllInstalledApps();
            runOnUiThread(() -> updateAppList(apps));
        }).start();
    }

    private void loadSystemApps() {
        showLoading(true);
        new Thread(() -> {
            List<SamsungBloatwareManager.AppInfo> apps = mBloatwareManager.getSystemApps();
            runOnUiThread(() -> updateAppList(apps));
        }).start();
    }

    private void updateAppList(List<SamsungBloatwareManager.AppInfo> apps) {
        mAdapter = new AppListAdapter(this, apps);
        mAppListView.setAdapter(mAdapter);
        showLoading(false);
    }

    private void selectAllBloatware() {
        if (mAdapter == null) return;

        int count = 0;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            SamsungBloatwareManager.AppInfo app = mAdapter.getItem(i);
            if (app != null && !app.isProtected) {
                app.isSelected = true;
                count++;
            }
        }
        mAdapter.notifyDataSetChanged();
        updateStorageInfo();
        Toast.makeText(this, "Selected " + count + " apps", Toast.LENGTH_SHORT).show();
    }

    private void uninstallSelected() {
        List<SamsungBloatwareManager.AppInfo> selected = getSelectedApps();

        if (selected.isEmpty()) {
            Toast.makeText(this, "No apps selected", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder appList = new StringBuilder();
        for (int i = 0; i < Math.min(selected.size(), 10); i++) {
            appList.append("• ").append(selected.get(i).appLabel).append("\n");
        }

        long freedSpace = calculateFreedSpace(selected);
        String freedMB = freedSpace / (1024 * 1024) + " MB";

        new AlertDialog.Builder(this)
                .setTitle("Remove " + selected.size() + " App(s)?")
                .setMessage("Apps to remove:\n\n" + appList +
                        "\nWill free: " + freedMB +
                        "\n\nYou can reinstall anytime from Play Store.")
                .setPositiveButton("Remove", (d, w) -> {
                    SamsungBloatwareManager.AppInfo app = selected.get(0);
                    mBloatwareManager.uninstallApp(app.packageName);
                    Toast.makeText(this,
                            "Uninstalling: " + app.appLabel,
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStorageInfo() {
        List<SamsungBloatwareManager.AppInfo> selected = getSelectedApps();
        long freedSpace = calculateFreedSpace(selected);
        String freedMB = freedSpace / (1024 * 1024) + " MB";

        if (selected.isEmpty()) {
            mStorageInfo.setText("Storage That Will Be Freed: 0 MB");
        } else {
            mStorageInfo.setText("Storage That Will Be Freed: " + freedMB +
                    " (" + selected.size() + " " +
                    getString(R.string.apps_selected) + ")");
        }
    }

    private long calculateFreedSpace(List<SamsungBloatwareManager.AppInfo> apps) {
        long total = 0;
        for (SamsungBloatwareManager.AppInfo app : apps) {
            try {
                java.io.File file = new java.io.File(app.sourceDir);
                if (file.exists()) {
                    total += file.length();
                }
            } catch (Exception ignored) {
            }
        }
        return total;
    }

    private List<SamsungBloatwareManager.AppInfo> getSelectedApps() {
        List<SamsungBloatwareManager.AppInfo> result = new ArrayList<>();
        if (mAdapter == null) return result;

        for (int i = 0; i < mAdapter.getCount(); i++) {
            SamsungBloatwareManager.AppInfo app = mAdapter.getItem(i);
            if (app != null && app.isSelected && !app.isProtected) {
                result.add(app);
            }
        }
        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            updateStorageInfo();
        }
    }
}
