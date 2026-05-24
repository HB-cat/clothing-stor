package com.heben.clothingstore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * 崩溃后的重启跳板：无界面，直接跳转到首页
 */
public class RestartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 直接跳转到主页
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // 结束自身
        finish();
    }
}