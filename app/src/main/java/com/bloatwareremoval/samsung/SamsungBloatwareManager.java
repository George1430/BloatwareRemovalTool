package com.bloatwareremoval.samsung;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Samsung-specific bloatware manager with pre-loaded Samsung apps list
 */
public class SamsungBloatwareManager {
    private static final String TAG = "SamsungBloatwareManager";

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final Set<String> mProtectedApps;
    private final Map<String, AppInfo> mAppCache;

    // Samsung Galaxy S10e essential apps (protected)
    private static final String[] SAMSUNG_PROTECTED = {
            "android", "com.android.systemui", "com.android.launcher3",
            "com.android.settings", "com.android.phone", "com.android.dialer",
            "com.android.messaging", "com.android.contacts", "com.android.mms",
            "com.samsung.android.incallui", "com.samsung.android.messaging",
            "com.samsung.android.app.contacts", "com.samsung.android.app.launcher",
            "com.google.android.gms", "com.google.android.gsf",
            "com.samsung.android.apps.camera", "com.samsung.android.apps.phone"
    };

    // Samsung Galaxy S10e bloatware (recommended for removal)
    private static final String[] SAMSUNG_BLOATWARE = {
            // Samsung apps
            "com.samsung.android.bixby.agent",
            "com.samsung.android.bixbyvision.framework",
            "com.samsung.bixby.voiceinput",
            "com.samsung.android.app.spage",
            "com.samsung.android.app.theme",
            "com.samsung.android.themestore",
            "com.samsung.android.app.notes",
            "com.samsung.android.app.galaxybuds",
            "com.samsung.android.app.watchmanager",
            "com.samsung.android.storyservice",
            "com.samsung.android.app.settings.samsung",
            "com.sec.android.app.shealth",
            "com.samsung.android.game.gametools",
            "com.samsung.android.game.gamebooster",
            "com.samsung.android.app.dex.dexonpc",
            "com.samsung.desktopmode.service",
            "com.sec.android.daemonapp",
            "com.samsung.android.smartcallscreen",
            "com.samsung.android.smartthings",
            "com.samsung.android.allshare.service.mediasharingd",

            // Facebook & Social
            "com.facebook.katana", "com.facebook.system", "com.facebook.services",
            "com.instagram.android", "com.twitter.android", "com.linkedin.android",
            "com.reddit.frontpage", "com.snapchat.android", "com.pinterest",

            // Google duplicates
            "com.android.chrome", "com.google.android.apps.maps",
            "com.google.android.apps.docs", "com.google.android.apps.sheets",
            "com.google.android.apps.slides", "com.google.android.gm",
            "com.google.android.apps.mediashell", "com.google.android.apps.turbo",
            "com.google.android.apps.youtube.music", "com.google.android.apps.nbu.files",

            // Music & Media
            "com.spotify.music", "com.pandora.android", "com.amazon.mp3",
            "com.musixmatch.android.lyrify",

            // Microsoft apps
            "com.microsoft.skydrive", "com.microsoft.office.outlook",
            "com.microsoft.office.onenote", "com.microsoft.skype",
            "com.microsoft.teams",

            // Other pre-installed
            "com.ebay.mobile", "com.flipkart.android", "com.opera.browser",
            "com.dropbox.android", "com.evernote", "com.booking",
            "com.duolingo", "com.discord"
    };

    public SamsungBloatwareManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
        this.mPackageManager = this.mContext.getPackageManager();
        this.mProtectedApps = new HashSet<>();
        this.mAppCache = new HashMap<>();

        for (String pkg : SAMSUNG_PROTECTED) {
            mProtectedApps.add(pkg);
        }
    }

    /**
     * Get all installed apps sorted by name
     */
    public List<AppInfo> getAllInstalledApps() {
        List<AppInfo> result = new ArrayList<>();

        try {
            List<ApplicationInfo> installedApps = mPackageManager.getInstalledApplications(
                    PackageManager.GET_META_DATA
            );

            for (ApplicationInfo appInfo : installedApps) {
                String label = String.valueOf(mPackageManager.getApplicationLabel(appInfo));
                boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isProtected = mProtectedApps.contains(appInfo.packageName);
                boolean isSamsungBloatware = isSamsungBloatware(appInfo.packageName);

                AppInfo app = new AppInfo(
                        appInfo.packageName,
                        label,
                        isSystem,
                        isProtected,
                        isSamsungBloatware,
                        appInfo.sourceDir
                );
                result.add(app);
                mAppCache.put(appInfo.packageName, app);
            }

            Collections.sort(result);
            Log.i(TAG, "Loaded " + result.size() + " apps");
        } catch (Exception e) {
            Log.e(TAG, "Error loading apps: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get recommended bloatware for removal (Samsung + known apps)
     */
    public List<AppInfo> getSuggestedBloatware() {
        List<AppInfo> result = new ArrayList<>();

        if (mAppCache.isEmpty()) {
            getAllInstalledApps();
        }

        for (AppInfo app : mAppCache.values()) {
            if (!app.isProtected && (app.isSamsungBloatware || isSamsungBloatware(app.packageName))) {
                result.add(app);
            }
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Get all system apps
     */
    public List<AppInfo> getSystemApps() {
        List<AppInfo> result = new ArrayList<>();

        if (mAppCache.isEmpty()) {
            getAllInstalledApps();
        }

        for (AppInfo app : mAppCache.values()) {
            if (app.isSystemApp) {
                result.add(app);
            }
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Check if package is Samsung bloatware
     */
    private boolean isSamsungBloatware(String packageName) {
        for (String bloat : SAMSUNG_BLOATWARE) {
            if (bloat.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate total size of Samsung bloatware
     */
    public long getTotalBloatwareSize() {
        List<AppInfo> bloatware = getSuggestedBloatware();
        long totalSize = 0;

        for (AppInfo app : bloatware) {
            totalSize += getAppSize(app);
        }

        return totalSize;
    }

    /**
     * Get app APK file size
     */
    private long getAppSize(AppInfo app) {
        try {
            java.io.File file = new java.io.File(app.sourceDir);
            if (file.exists()) {
                return file.length();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting size for " + app.packageName);
        }
        return 0;
    }

    /**
     * Uninstall app safely
     */
    public void uninstallApp(String packageName) {
        if (mProtectedApps.contains(packageName)) {
            Log.w(TAG, "Blocked: Protected app " + packageName);
            return;
        }

        try {
            Uri packageUri = Uri.parse("package:" + packageName);
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
            uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(uninstallIntent);
            Log.i(TAG, "Uninstall dialog opened for: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error uninstalling: " + e.getMessage());
        }
    }

    /**
     * App info data class
     */
    public static class AppInfo implements Comparable<AppInfo> {
        public final String packageName;
        public final String appLabel;
        public final boolean isSystemApp;
        public final boolean isProtected;
        public final boolean isSamsungBloatware;
        public final String sourceDir;
        public boolean isSelected = false;

        public AppInfo(String packageName, String appLabel, boolean isSystemApp,
                       boolean isProtected, boolean isSamsungBloatware, String sourceDir) {
            this.packageName = packageName;
            this.appLabel = appLabel != null ? appLabel : "Unknown";
            this.isSystemApp = isSystemApp;
            this.isProtected = isProtected;
            this.isSamsungBloatware = isSamsungBloatware;
            this.sourceDir = sourceDir;
        }

        @Override
        public int compareTo(AppInfo other) {
            return this.appLabel.compareTo(other.appLabel);
        }
    }
}
