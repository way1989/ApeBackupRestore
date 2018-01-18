package com.ape.backuprestore;

import android.graphics.drawable.Drawable;

public class AppSnippet {
    public String mFileName; //only for restore
    private Drawable mIcon;
    private CharSequence mName;
    private String mPackageName;

    public AppSnippet(Drawable icon, CharSequence name, String packageName) {
        mIcon = icon;
        mName = name;
        mPackageName = packageName;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public CharSequence getName() {
        return mName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setFileName(String filename) {
        mFileName = filename;
    }

    public String getFileName() {
        return mFileName;
    }
}
