package com.wangwei.music_wangwei.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.activity.HomeActivity;
import com.wangwei.music_wangwei.activity.MusicPlayActivity;
import com.wangwei.music_wangwei.adapter.PlaylistAdapter;
import com.wangwei.music_wangwei.entity.MusicInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlaylistDialog extends Dialog {

    private ListView lvPlaylist;
    private TextView tvTitle;
    private PlaylistAdapter adapter;
    private OnItemClickListener itemClickListener;
    private List<MusicInfo> musicList;
    private int currentPosition;

    public PlaylistDialog(Context context, List<MusicInfo> musicList, int currentPosition) {
        super(context, R.style.Theme_Transparent);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_playlist);

        this.musicList = new ArrayList<>(musicList);
        this.currentPosition = currentPosition;

        initViews();
        initData();
        setupListeners();

        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
        }
    }

    private void initViews() {
        lvPlaylist = findViewById(R.id.lv_playlist);
        tvTitle = findViewById(R.id.tv_dialog_title);
    }

    private void initData() {
        tvTitle.setText("播放列表 (" + musicList.size() + ")");
        adapter = new PlaylistAdapter(getContext(), musicList, currentPosition);
        lvPlaylist.setAdapter(adapter);
    }

    private void setupListeners() {
        lvPlaylist.setOnItemClickListener((parent, view, position, id) -> {
            if (itemClickListener != null) {
                MusicInfo musicInfo = (MusicInfo) parent.getItemAtPosition(position);
                currentPosition = position;
                itemClickListener.onItemClick(position, musicInfo);
                dismiss();
            }
        });

        lvPlaylist.setOnItemLongClickListener((parent, view, position, id) -> {
            ImageView ivDelete = view.findViewById(R.id.iv_delete);
            ivDelete.setVisibility(View.VISIBLE);
            return true;
        });

        findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, MusicInfo musicInfo);
    }

    public void updateList(List<MusicInfo> newList) {
        musicList = new ArrayList<>(newList);
        initData();
    }

    public void deleteItem(int position) {
        if (position < 0 || position >= musicList.size()) {
            return;
        }
        musicList.remove(position);
        if (position == currentPosition) {
            if (musicList.isEmpty()) {
                dismiss();
                Intent intent = new Intent(getContext(), HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intent);
            } else {
                if (MusicPlayActivity.playMode == MusicPlayActivity.PLAY_MODE_ORDER ||
                        MusicPlayActivity.playMode == MusicPlayActivity.PLAY_MODE_REPEAT) {
                    if (position == musicList.size()) {
                        currentPosition = 0;
                    } else {
                        currentPosition = position;
                    }
                } else if (MusicPlayActivity.playMode == MusicPlayActivity.PLAY_MODE_SHUFFLE) {
                    Random random = new Random();
                    currentPosition = random.nextInt(musicList.size());
                }
                MusicInfo musicInfo = musicList.get(currentPosition);
                Intent intent = new Intent(getContext(), MusicPlayActivity.class);
                intent.putExtra("music_list", new ArrayList<>(musicList));
                intent.putExtra("current_position", currentPosition);
                getContext().startActivity(intent);
            }
        }
        initData();
    }
}