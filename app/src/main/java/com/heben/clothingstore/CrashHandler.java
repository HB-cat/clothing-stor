package com.heben.clothingstore;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局异常捕获处理器
 * 捕获未处理的异常，写入崩溃日志文件，然后重启App
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance;
    private Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler() {}

    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    /**
     * 初始化：保存系统默认的异常处理器，然后设置本类为全局处理器
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当发生未捕获异常时，系统会调用这个方法
     */
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 1. 写入崩溃日志文件
        writeCrashLog(throwable);

        // 2. 尝试重启App
        restartApp();

        // 3. 如果系统有默认处理器，交给它处理（通常会杀死进程）
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            // 没有默认处理器，自己结束进程
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 将崩溃信息写入文件
     * 文件位置：Download/记账备份/crash_log.txt
     */
    private void writeCrashLog(Throwable throwable) {
        try {
            // 保存到 Download/记账备份/ 目录（和自动备份同目录）
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "记账备份"
            );
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File logFile = new File(dir, "crash_log.txt");
            FileWriter writer = new FileWriter(logFile, true);  // 追加模式

            // 写入崩溃时间
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            writer.write("========================================\n");
            writer.write("崩溃时间：" + time + "\n");
            writer.write("App版本：v1.0.0\n");
            writer.write("\n");

            // 写入异常信息
            throwable.printStackTrace(new PrintWriter(writer));
            writer.write("\n\n");

            writer.flush();
            writer.close();

        } catch (IOException e) {
            // 写日志本身失败，无法处理，忽略
        }
    }

    /**
     * 重启App：通过启动一个透明的 RestartActivity 来重新进入首页
     */
    private void restartApp() {
        try {
            Intent intent = new Intent(context, RestartActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {}
    }
}