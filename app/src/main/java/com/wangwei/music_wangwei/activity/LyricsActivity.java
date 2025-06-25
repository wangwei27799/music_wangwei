package com.wangwei.music_wangwei.activity;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.util.LyricsUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LyricsActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    // 常量定义
    public static final String ACTION_UPDATE_LYRIC = "com.wangwei.music.action.UPDATE_LYRIC";
    public static final String EXTRA_CURRENT_TIME = "current_time";

    private static final int HIGHLIGHT_COLOR = Color.RED;
    private static final int NORMAL_COLOR = Color.WHITE;
    private static final long UPDATE_INTERVAL = 200; // 提高同步精度

    // UI组件
    private ImageView ivClose, ivFavorite, ivPlayPause, ivPrevious, ivNext, ivPlayMode;
    private TextView tvSongName, tvArtist, tvLyrics;

    private MediaPlayer mediaPlayer;
    private SeekBar sbProgress;

    // 数据
    private MusicInfo currentMusic;
    private List<LyricsUtils.LyricLine> lyricLines;
    private int currentLineIndex = -1;
    private boolean isFavorite = false;
    private int playMode = MusicPlayActivity.PLAY_MODE_ORDER;

    // 歌词滚动
    private Handler lyricHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator scrollAnimator;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);

        initViews();
        initData();
        setupListeners();
        initGestureDetector();
        registerBroadcastReceiver();
    }

    private void initViews() {
        ivClose = findViewById(R.id.iv_close);
        ivFavorite = findViewById(R.id.iv_favorite);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        ivPrevious = findViewById(R.id.iv_previous);
        ivNext = findViewById(R.id.iv_next);
        ivPlayMode = findViewById(R.id.iv_play_mode);

        tvSongName = findViewById(R.id.tv_song_name);
        tvArtist = findViewById(R.id.tv_artist);
        tvLyrics = findViewById(R.id.tv_lyrics);
        tvLyrics.setTextColor(NORMAL_COLOR); // 设置默认文本颜色

        sbProgress = findViewById(R.id.sb_progress);
    }

    private void initData() {
        currentMusic = getIntent().getParcelableExtra("musicInfo");
        if (currentMusic == null) {
            Toast.makeText(this, "音乐信息为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置歌曲信息
        tvSongName.setText(currentMusic.getMusicName());
        tvArtist.setText(currentMusic.getAuthor());

        // 加载歌词
        loadLyrics();

        // 更新播放模式图标
        updatePlayModeIcon();
    }

    private void loadLyrics() {
        new Thread(() -> {
            try {
                String path = currentMusic.getLyricUrl();
                Log.d("LyricsActivity", "尝试加载歌词: " + path);

                String lyricsContent;
                if (path.startsWith("http")) {
                    lyricsContent = loadLyricsFromNetwork(path);
                } else {
                    lyricsContent = loadLyricsFromAssets(path);
                }

                runOnUiThread(() -> {
                    lyricLines = LyricsUtils.parseLyrics(lyricsContent);
                    if (lyricLines.isEmpty()) {
                        tvLyrics.setText("暂无歌词");
                    } else {
                        showLyrics();
                        startLyricsSync();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvLyrics.setText("歌词加载失败"));
            }
        }).start();
    }

    // 从网络加载歌词
    private String loadLyricsFromNetwork(String urlString) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            } else {
                throw new IOException("HTTP请求失败，状态码: " + responseCode);
            }
        } finally {
            if (inputStream != null) inputStream.close();
            if (connection != null) connection.disconnect();
        }
    }

    private String loadLyricsFromAssets(String path) throws IOException {
        InputStream inputStream = null;
        try {
            Log.d("LyricsActivity", "开始加载本地歌词");
            inputStream = getAssets().open(path);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            Log.d("LyricsActivity", "歌词内容长度: " + content.length());
            return content.toString();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("LyricsActivity", "关闭输入流失败", e);
                }
            }
        }
    }

    private void showLyrics() {
        StringBuilder lyricsBuilder = new StringBuilder();
        for (LyricsUtils.LyricLine line : lyricLines) {
            lyricsBuilder.append(line.content).append("\n");
        }
        tvLyrics.setText(lyricsBuilder.toString());
    }

    private void setupListeners() {
        ivClose.setOnClickListener(this);
        ivFavorite.setOnClickListener(this);
        ivPlayPause.setOnClickListener(this);
        ivPrevious.setOnClickListener(this);
        ivNext.setOnClickListener(this);
        ivPlayMode.setOnClickListener(this);
        sbProgress.setOnSeekBarChangeListener(this);
    }

    private void initGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e2.getX() - e1.getX() > 200 && Math.abs(velocityX) > 100) {
                    finish();
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_UPDATE_LYRIC);
        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, filter);
    }

    private void startLyricsSync() {
        lyricHandler.removeCallbacks(lyricsSyncRunnable);
        lyricHandler.post(lyricsSyncRunnable);
    }

    private final Runnable lyricsSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (lyricLines != null && !lyricLines.isEmpty()) {
                long currentTime = getCurrentMusicTime();
                int lineIndex = LyricsUtils.getCurrentLineIndex(lyricLines, currentTime);
                if (lineIndex != currentLineIndex) {
                    currentLineIndex = lineIndex;
                    highlightCurrentLine();
                }
            }
            lyricHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    private void highlightCurrentLine() {
        if (lyricLines == null || lyricLines.isEmpty() || currentLineIndex < 0) {
            tvLyrics.setTextColor(NORMAL_COLOR);
            return;
        }

        String fullLyrics = tvLyrics.getText().toString();
        if (fullLyrics.isEmpty()) return;

        SpannableString spannable = new SpannableString(fullLyrics);

        // 计算当前行位置
        int startIndex = 0;
        for (int i = 0; i < currentLineIndex; i++) {
            startIndex = fullLyrics.indexOf("\n", startIndex) + 1;
            if (startIndex <= 0) break;
        }

        int endIndex = fullLyrics.indexOf("\n", startIndex);
        if (endIndex == -1) endIndex = fullLyrics.length();

        // 清除旧高亮
        ForegroundColorSpan[] oldSpans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : oldSpans) {
            spannable.removeSpan(span);
        }

        // 设置新高亮
        spannable.setSpan(
                new ForegroundColorSpan(HIGHLIGHT_COLOR),
                startIndex, endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvLyrics.setText(spannable);
        scrollToCurrentLine();
    }

    private void scrollToCurrentLine() {
        if (currentLineIndex < 0 || tvLyrics.getLineCount() <= currentLineIndex) return;

        int lineHeight = tvLyrics.getLineHeight();
        int targetY = currentLineIndex * lineHeight - tvLyrics.getHeight() / 2 + lineHeight / 2;

        // 确保不超出滚动范围
        int maxScrollY = tvLyrics.getLineCount() * lineHeight - tvLyrics.getHeight();
        targetY = Math.max(0, Math.min(targetY, maxScrollY));

        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }

        scrollAnimator = ValueAnimator.ofInt(tvLyrics.getScrollY(), targetY);
        scrollAnimator.setDuration(300);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            tvLyrics.scrollTo(0, value);
        });
        scrollAnimator.start();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.iv_close) {
            finish();
        } else if (id == R.id.iv_favorite) {
            toggleFavorite();
        } else if (id == R.id.iv_play_pause) {
            togglePlayPause();
        } else if (id == R.id.iv_previous) {
            playPrevious();
        } else if (id == R.id.iv_next) {
            playNext();
        } else if (id == R.id.iv_play_mode) {
            changePlayMode();
        }
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        ivFavorite.setImageResource(isFavorite ?
                R.drawable.ic_favorite_fill : R.drawable.heart);
        Toast.makeText(this, isFavorite ? "已添加到喜欢" : "已取消喜欢", Toast.LENGTH_SHORT).show();
    }

    private void togglePlayPause() {
        Intent intent = new Intent(MusicPlayActivity.ACTION_PLAY_PAUSE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void playPrevious() {
        Intent intent = new Intent(MusicPlayActivity.ACTION_PREVIOUS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void playNext() {
        Intent intent = new Intent(MusicPlayActivity.ACTION_NEXT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void changePlayMode() {
        playMode = (playMode + 1) % 3;
        updatePlayModeIcon();

        Intent intent = new Intent(MusicPlayActivity.ACTION_CHANGE_MODE);
        intent.putExtra("play_mode", playMode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updatePlayModeIcon() {
        switch (playMode) {
            case MusicPlayActivity.PLAY_MODE_ORDER:
                ivPlayMode.setImageResource(R.drawable.ic_play_mode_order);
                break;
            case MusicPlayActivity.PLAY_MODE_REPEAT:
                ivPlayMode.setImageResource(R.drawable.ic_play_mode_repeat);
                break;
            case MusicPlayActivity.PLAY_MODE_SHUFFLE:
                ivPlayMode.setImageResource(R.drawable.ic_play_mode_shuffle);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && mediaPlayer != null) {
            int seekPosition = (int) (mediaPlayer.getDuration() * progress / 1000f);
            seekTo(seekPosition);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        lyricHandler.removeCallbacks(lyricsSyncRunnable);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        lyricHandler.post(lyricsSyncRunnable);
    }

    private void seekTo(int position) {
        Intent intent = new Intent(MusicPlayActivity.ACTION_SEEK);
        intent.putExtra("position", position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private long getCurrentMusicTime() {
        // 从MusicService获取当前播放时间
        return 0;
    }

    private final BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_UPDATE_LYRIC.equals(intent.getAction())) {
                long currentTime = intent.getLongExtra(EXTRA_CURRENT_TIME, 0);
                updateCurrentLyric(currentTime);

                if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                    int progress = (int) (currentTime * 1000 / mediaPlayer.getDuration());
                    sbProgress.setProgress(progress);
                }
            }
        }
    };

    private void updateCurrentLyric(long currentTime) {
        if (lyricLines != null && !lyricLines.isEmpty()) {
            int lineIndex = LyricsUtils.getCurrentLineIndex(lyricLines, currentTime);
            if (lineIndex != currentLineIndex) {
                currentLineIndex = lineIndex;
                highlightCurrentLine();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lyricHandler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicReceiver);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}