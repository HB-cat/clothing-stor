package com.heben.clothingstore;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 所有 Activity 的基类：全局点击音效。
 * 重写 dispatchTouchEvent，任何按钮的点击都会播放 click.mp3。
 */
public abstract class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 开启窗口过渡动画
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 手指抬起时检测点击位置是否落在某个按钮上
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            View touchedView = findViewAt(
                    (ViewGroup) getWindow().getDecorView().getRootView(),
                    (int) ev.getRawX(), (int) ev.getRawY()
            );
            if (touchedView instanceof Button || touchedView instanceof ImageButton) {
                MediaSoundHelper.getInstance().playClick(this);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 递归查找坐标 (x, y) 所在的叶子 View
     */
    private View findViewAt(ViewGroup parent, int x, int y) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (isViewContains(child, x, y)) {
                if (child instanceof ViewGroup) {
                    return findViewAt((ViewGroup) child, x, y);
                } else {
                    return child;
                }
            }
        }
        return parent;
    }

    /**
     * 判断坐标是否落在 view 的区域内
     */
    private boolean isViewContains(View view, int rx, int ry) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        return (rx >= x && rx <= x + view.getWidth() && ry >= y && ry <= y + view.getHeight());
    }
}