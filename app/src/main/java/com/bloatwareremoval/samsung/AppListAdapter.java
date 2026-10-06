package com.bloatwareremoval.samsung;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying apps with caching
 */
public class AppListAdapter extends ArrayAdapter<SamsungBloatwareManager.AppInfo> {
    private final Context mContext;
    private final List<SamsungBloatwareManager.AppInfo> mApps;
    private final LayoutInflater mInflater;
    private final Map<String, android.graphics.drawable.Drawable> mIconCache;

    public AppListAdapter(@NonNull Context context, @NonNull List<SamsungBloatwareManager.AppInfo> apps) {
        super(context, 0, apps);
        this.mContext = context;
        this.mApps = apps;
        this.mInflater = LayoutInflater.from(context);
        this.mIconCache = new HashMap<>();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.app_list_item, parent, false);
            holder = new ViewHolder();
            holder.appIcon = convertView.findViewById(R.id.app_icon);
            holder.appName = convertView.findViewById(R.id.app_name);
            holder.appPackage = convertView.findViewById(R.id.app_package);
            holder.appType = convertView.findViewById(R.id.app_type);
            holder.appCheckbox = convertView.findViewById(R.id.app_checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SamsungBloatwareManager.AppInfo app = mApps.get(position);

        // Get icon from cache
        android.graphics.drawable.Drawable icon = mIconCache.get(app.packageName);
        if (icon == null) {
            try {
                icon = mContext.getPackageManager().getApplicationIcon(app.packageName);
                mIconCache.put(app.packageName, icon);
            } catch (Exception e) {
                icon = mContext.getResources().getDrawable(
                        android.R.drawable.sym_def_app_icon, null);
            }
        }
        holder.appIcon.setImageDrawable(icon);

        // Set app info
        holder.appName.setText(app.appLabel);
        holder.appPackage.setText(app.packageName);

        String typeText;
        if (app.isProtected) {
            typeText = "Protected System App";
        } else if (app.isSamsungBloatware) {
            typeText = "Samsung Bloatware";
        } else if (app.isSystemApp) {
            typeText = "System App";
        } else {
            typeText = "User App";
        }
        holder.appType.setText(typeText);

        // Checkbox
        holder.appCheckbox.setOnCheckedChangeListener(null);
        holder.appCheckbox.setChecked(app.isSelected);
        holder.appCheckbox.setEnabled(!app.isProtected);
        holder.appCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.isSelected = isChecked;
        });

        // Visual feedback
        if (app.isSamsungBloatware) {
            convertView.setBackgroundColor(0xFFFFEBEE);
        } else if (app.isProtected) {
            convertView.setAlpha(0.6f);
            convertView.setBackgroundColor(0xFFF5F5F5);
        } else {
            convertView.setAlpha(1.0f);
            convertView.setBackgroundColor(0xFFFFFFFF);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        TextView appType;
        CheckBox appCheckbox;
    }
}
