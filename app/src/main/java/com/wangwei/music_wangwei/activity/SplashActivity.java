package com.wangwei.music_wangwei.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.fragment.TermsDialogFragment;

public class SplashActivity extends AppCompatActivity {

    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_IS_AGREED = "isAgreed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏标题栏和状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        // 检查是否已同意过协议
        boolean isAgreed = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getBoolean(KEY_IS_AGREED, false);
        if (isAgreed) {
            // 已同意，直接进入首页
            goToMainActivity();
            return;
        }

        // 未同意，显示协议弹窗
        FragmentManager fragmentManager = getSupportFragmentManager();
        TermsDialogFragment dialogFragment = new TermsDialogFragment();
        dialogFragment.show(fragmentManager, "TermsDialog");
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}