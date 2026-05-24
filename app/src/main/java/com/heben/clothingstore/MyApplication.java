package com.heben.clothingstore;

import android.app.Application;

/**
 * 自定义 Application：初始化全局异常捕获和音效
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 设置全局异常捕获
        CrashHandler.getInstance().init(this);
        // 初始化音效
        MediaSoundHelper.getInstance().init(this);
    }
}