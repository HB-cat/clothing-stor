package com.heben.clothingstore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * 自动备份工具：每天首次启动时备份数据库到Download/记账备份/，
 * 只保留最近7天的备份文件。
 */
public class AutoBackupHelper {

    private static final String BACKUP_DIR_NAME = "记账备份";
    private static final String PREFS_NAME = "backup_prefs";
    private static final String KEY_LAST_BACKUP_DATE = "last_backup_date";
    private static final int KEEP_DAYS = 7;

    /**
     * 检查今天是否已经备份过，如果没有就执行备份
     * 在 MainActivity.onCreate 中调用
     */
    public static void backupIfNeeded(Context context) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String lastBackup = prefs.getString(KEY_LAST_BACKUP_DATE, "");

                // 今天已经备份过，跳过
                if (today.equals(lastBackup)) {
                    return;
                }

                // 获取数据库文件
                File dbFile = context.getDatabasePath("clothing_store.db");
                if (!dbFile.exists()) {
                    return;
                }

                // 创建备份目录
                File backupDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        BACKUP_DIR_NAME
                );
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                // 备份文件命名：服装店数据_2026-05-23.db
                String backupFileName = "服装店数据_" + today + ".db";
                File backupFile = new File(backupDir, backupFileName);

                // 复制文件
                copyFile(dbFile, backupFile);

                // 记录备份日期
                prefs.edit().putString(KEY_LAST_BACKUP_DATE, today).apply();

                // 清理旧备份（只保留最近7天）
                cleanOldBackups(backupDir);

                Log.d("AutoBackup", "备份成功: " + backupFileName);

            } catch (IOException e) {
                Log.e("AutoBackup", "备份失败", e);
            }
        }).start();
    }

    /**
     * 文件复制
     */
    private static void copyFile(File source, File dest) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.flush();
        fos.close();
        fis.close();
    }

    /**
     * 删除超过 KEEP_DAYS 天的旧备份文件
     */
    private static void cleanOldBackups(File backupDir) {
        File[] files = backupDir.listFiles();
        if (files == null || files.length <= KEEP_DAYS) {
            return;
        }

        // 按修改时间排序（从旧到新）
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        // 删除最旧的，直到只剩 KEEP_DAYS 个
        int deleteCount = files.length - KEEP_DAYS;
        for (int i = 0; i < deleteCount; i++) {
            files[i].delete();
        }
    }
}