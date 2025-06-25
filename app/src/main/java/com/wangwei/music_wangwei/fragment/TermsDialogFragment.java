package com.wangwei.music_wangwei.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.widget.Toast;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.activity.HomeActivity;

public class TermsDialogFragment extends DialogFragment {

    private static final String USER_PROTOCOL_URL = "https://www.mi.com";
    private static final String PRIVACY_POLICY_URL = "https://www.xiaomiev.com/";
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_IS_AGREED = "isAgreed";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_splash_terms, container, false);

        // 设置按钮点击事件
        view.findViewById(R.id.btn_disagree).setOnClickListener(this::onDisagreeClick);
        view.findViewById(R.id.btn_agree).setOnClickListener(this::onAgreeClick);

        // 设置协议文本的点击事件
        setupTermsClickableText(view);

        return view;
    }

    /**
     * 处理不同意按钮点击
     * @param view
     */
    private void onDisagreeClick(View view) {
        if (requireActivity() != null) {
            requireActivity().finish();
        }
        dismiss();
    }

    /**
     * 处理同意按钮点击
     * @param view
     */
    private void onAgreeClick(View view) {
        // 保存用户状态
        saveAgreeStatus();
        if (requireActivity() != null) {
            Intent intent = new Intent(requireActivity(), HomeActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
        dismiss();
    }

    /**
     * 设置协议文本的点击事件
     * @param view
     */
    private void setupTermsClickableText(View view) {
        TextView contentView = view.findViewById(R.id.tv_content);
        String text = contentView.getText().toString();

        SpannableString spannableString = new SpannableString(text);

        // 设置《用户协议》点击事件
        int startIndex = text.indexOf("《用户协议》");
        int endIndex = startIndex + "《用户协议》".length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Log.d("HomeActivity", "点击了用户协议");
                openUrl(USER_PROTOCOL_URL);
            }
        }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置《隐私政策》点击事件
        int privacyStart = text.indexOf("《隐私政策》");
        int privacyEnd = privacyStart + "《隐私政策》".length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Log.d("HomeActivity", "点击了隐私政策");
                openUrl(PRIVACY_POLICY_URL);
            }
        }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        contentView.setText(spannableString);
        contentView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * 打开网页
     * @param url
     */
    @SuppressLint("QueryPermissionsNeeded")
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        // 添加FLAG_ACTIVITY_NEW_TASK标志
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 开启活动，跳转到对应的网页
        startActivity(intent);
    }

    /**
     * 保持条款状态
     * 同意or不同意
     */
    private void saveAgreeStatus() {
        requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_AGREED, true)
                .apply();
    }
}