package com.wangwei.music_wangwei.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.adapter.HomeAdapter;
import com.wangwei.music_wangwei.api.ApiClient;
import com.wangwei.music_wangwei.api.ApiService;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.entity.Record;
import com.wangwei.music_wangwei.event.MusicItemClickListener;
import com.wangwei.music_wangwei.response.HomePageResponse;
import com.wangwei.music_wangwei.service.FloatingViewService;
import com.wangwei.music_wangwei.service.MusicService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//glide
import com.bumptech.glide.Glide;
//looper
import android.os.Looper;


public class HomeActivity extends AppCompatActivity implements MusicItemClickListener {
    private static final String TAG = "HomeActivity";
    private static final int PAGE_SIZE = 4;
    private int currentPage = 1;
    private boolean isLoading = false;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private Handler handler;

    private ProgressDialog progressDialog;
    private Runnable bannerRunnable;

    // 搜索框-搜索内容
    private EditText searchEditText;
    // 搜索框-取消
    private TextView cancelText;
    // 后台音乐服务
    private MusicService musicService;
    private boolean isBound = false;

    private List<MusicInfo> allMusicList = new ArrayList<>();

    private View floatingView;
    private ImageView ivFloatingPlayPause, ivFloatingPlaylist;
    private TextView tvFloatingSongName, tvFloatingArtist;
    private ImageView ivFloatingAlbum;
    private SeekBar sbFloatingProgress;
    private ArrayList<MusicInfo> musicList = new ArrayList<>();

    private ArrayList<MusicInfo> allmusicList = new ArrayList<>();

    private static final int REQUEST_CODE_SYSTEM_ALERT_WINDOW = 1;
    private int currentPosition = 0;

    private FloatingViewService floatingViewService;
    private boolean isServiceBound = false;

    // Service连接对象
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    private final ServiceConnection mmm = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingViewService.LocalBinder binder = (FloatingViewService.LocalBinder) service;
            floatingViewService = binder.getService();
            isServiceBound = true;

            // 传递真实的音乐数据
            if (allMusicList != null && !allMusicList.isEmpty()) {
                floatingViewService.setMusicList(new ArrayList<>(allMusicList), currentPosition);

                // 更新UI显示当前播放歌曲
                updateFloatingViewWithCurrentSong();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private void updateFloatingViewWithCurrentSong() {
        if (currentPosition >= 0 && currentPosition < allMusicList.size()) {
            MusicInfo currentSong = allMusicList.get(currentPosition);
            tvFloatingSongName.setText(currentSong.getMusicName());
            tvFloatingArtist.setText(currentSong.getAuthor());
            Glide.with(this)
                    .load(currentSong.getCoverUrl())
                    .into(ivFloatingAlbum);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1);
        } else {
            // 启动服务
            Intent serviceIntent = new Intent(this, FloatingViewService.class);
            startService(serviceIntent);
            bindService(serviceIntent, mmm, BIND_AUTO_CREATE);
        }


        if (checkSystemAlertWindowPermission()) {
            initFloatingView();
        } else {
            requestSystemAlertWindowPermission();
        }

        initViews();
        initRecyclerView();
        loadData();
        startBannerAutoScroll();

        // 绑定Service
        bindMusicService();

        // 初始化悬浮View
        initFloatingView();

        // 首次打开App，随机选择某个模块音乐播放
        if (allMusicList != null && !allMusicList.isEmpty()) {
            java.util.Random random = new java.util.Random();
            currentPosition = random.nextInt(allMusicList.size());
            musicList = new ArrayList<>(allMusicList);
            playMusic(currentPosition);
            showFloatingView();
        }
    }


    private boolean checkSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW);
        }
    }


    private void playMusic(int position) {
        if (musicService != null && allMusicList != null && position < allMusicList.size()) {
            currentPosition = position;
            MusicInfo musicInfo = allMusicList.get(position);
            musicService.playMusic(musicInfo.getMusicUrl());

            // 更新悬浮窗显示
            updateFloatingViewWithCurrentSong();
            showFloatingView();

            // 更新服务中的当前播放位置
            if (floatingViewService != null) {
                floatingViewService.setCurrentPosition(position);
            }
        }
    }

    private void initFloatingView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_music_view, null);

        ivFloatingPlayPause = floatingView.findViewById(R.id.iv_floating_play_pause);
        ivFloatingPlaylist = floatingView.findViewById(R.id.iv_floating_playlist);
        tvFloatingSongName = floatingView.findViewById(R.id.tv_floating_song_name);
        tvFloatingArtist = floatingView.findViewById(R.id.tv_floating_artist);
        ivFloatingAlbum = floatingView.findViewById(R.id.iv_floating_album);
        sbFloatingProgress = floatingView.findViewById(R.id.sb_floating_progress);

        ivFloatingPlayPause.setOnClickListener(v -> {
            if (musicService.isPlaying()) {
                musicService.pause();
                ivFloatingPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                musicService.play();
                ivFloatingPlayPause.setImageResource(R.drawable.ic_pause);
            }
        });

        ivFloatingPlaylist.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlaylistDialog.class);
            intent.putExtra("music_list", musicList);
            intent.putExtra("current_position", currentPosition);
            startActivity(intent);
            Log.d("HomeActivity", "点击了播放列表");
        });

        floatingView.setOnClickListener(v -> {
            if (allMusicList != null && !allMusicList.isEmpty()) {
                // 停止悬浮窗服务
                stopService(new Intent(this, FloatingViewService.class));

                // 启动音乐播放Activity
                Intent intent = new Intent(this, MusicPlayActivity.class);
                intent.putExtra("music_list", new ArrayList<>(allMusicList));
                intent.putExtra("current_position", currentPosition);
                startActivity(intent);
            } else {
                Toast.makeText(this, "暂无音乐数据", Toast.LENGTH_SHORT).show();
            }
        });

        sbFloatingProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int seekPosition = musicService.getDuration() * progress / 1000;
                    musicService.seekTo(seekPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 停止更新进度条
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 继续更新进度条
            }
        });

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM;

        windowManager.addView(floatingView, params);
        floatingView.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            musicList = data.getParcelableArrayListExtra("music_list");
            currentPosition = data.getIntExtra("current_position", 0);
            showFloatingView();
        }

        if (requestCode == REQUEST_CODE_SYSTEM_ALERT_WINDOW) {
            if (checkSystemAlertWindowPermission()) {
                initFloatingView();
            } else {
                Toast.makeText(this, "未授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showFloatingView() {
        MusicInfo musicInfo = musicList.get(currentPosition);
        tvFloatingSongName.setText(musicInfo.getMusicName());
        tvFloatingArtist.setText(musicInfo.getAuthor());
        Glide.with(this).load(musicInfo.getCoverUrl()).into(ivFloatingAlbum);
        if (musicService.isPlaying()) {
            ivFloatingPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            ivFloatingPlayPause.setImageResource(R.drawable.ic_play);
        }
        floatingView.setVisibility(View.VISIBLE);
    }

    private void bindMusicService() {
        if (!isBound) {
            Intent intent = new Intent(this, MusicService.class);
            try {
                bindService(intent, serviceConnection, BIND_AUTO_CREATE);
            } catch (Exception e) {
                Log.e(TAG, "绑定服务失败", e);
            }
        }
    }

    private void initViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        cancelText = findViewById(R.id.cancel_text);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        recyclerView = findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

//        recyclerView.addItemDecoration(new HorizontalSpacingItemDecoration(16));
        recyclerView.setPadding(20, 0, 20, 0);
        recyclerView.setClipToPadding(false);

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HomeAdapter(this);
        recyclerView.setAdapter(adapter);
        adapter.setItemClickListener(this);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 1) {
                    loadMoreData();
                }
            }
        });
    }

    @Override
    public void onMusicItemClick(int position, MusicInfo musicInfo) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        // 确保有音乐数据
        if (allMusicList == null || allMusicList.isEmpty()) {
            Toast.makeText(this, "暂无音乐数据", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(this, MusicPlayActivity.class);
            intent.putExtra("current_position", position);
            intent.putExtra("music_list", new ArrayList<>(allMusicList));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "启动播放页面失败", e);
            Toast.makeText(this, "无法打开播放页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {
        currentPage = 1;
        fetchHomeData(currentPage, PAGE_SIZE, true);
    }

    private void refreshData() {
        currentPage = 1;
        fetchHomeData(currentPage, PAGE_SIZE, true);
    }

    private void loadMoreData() {
        if (isLoading || currentPage >= adapter.getTotalPages()) {
            return;
        }
        isLoading = true;
        currentPage++;
        fetchHomeData(currentPage, PAGE_SIZE, true);
    }

    private void showLoading(boolean show) {
        if (show) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("加载中...");
                progressDialog.setCancelable(false);
            }
            progressDialog.show();
        } else {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    private void fetchHomeData(int page, int size, boolean showLoading) {
        if (showLoading && !swipeRefreshLayout.isRefreshing()) {
            showLoading(true);
        }

        ApiService apiService = ApiClient.getRetrofit().create(ApiService.class);
        Call<HomePageResponse> call = apiService.getHomePageData(page, size);
        call.enqueue(new Callback<HomePageResponse>() {
            @Override
            public void onResponse(@NonNull Call<HomePageResponse> call, @NonNull Response<HomePageResponse> response) {
                if (isFinishing()) return;

                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    HomePageResponse responseBody = response.body();
                    Log.d(TAG, "获取数据成功: " + responseBody.getData().getRecords());

                    // 在后台线程中处理数据
                    new Thread(() -> {
                        // 处理数据
                        List<com.wangwei.music_wangwei.entity.Record> records = responseBody.getData().getRecords();
                        List<MusicInfo> musicInfoList = new ArrayList<>();

                        for (com.wangwei.music_wangwei.entity.Record record : records) {
                            musicInfoList.addAll(record.getMusicInfoList());
                        }

                        // 切换回主线程更新 UI
                        runOnUiThread(() -> {
                            handleDataResponse(page, responseBody); // 确保在主线程中调用
                        });
                    }).start();

                } else {
                    handleErrorResponse();
                }
                isLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<HomePageResponse> call, @NonNull Throwable t) {
                if (isFinishing()) return;
                handleNetworkError(t);
            }
        });
    }

    private void handleDataResponse(int page, HomePageResponse responseBody) {
        List<Record> records = responseBody.getData().getRecords();
        if (page == 1) {
            allMusicList.clear();
            for (Record record : records) {
                allMusicList.addAll(record.getMusicInfoList());
            }
            adapter.setData(responseBody.getData());

            // 数据加载完成后再绑定悬浮窗服务
            bindFloatingViewServiceWithRealData();
        } else {
            for (Record record : records) {
                allMusicList.addAll(record.getMusicInfoList());
            }
            adapter.addData(responseBody.getData());
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void bindFloatingViewServiceWithRealData() {
        if (checkSystemAlertWindowPermission() && !allMusicList.isEmpty()) {
            // 解绑旧服务(如果已绑定)
            if (isServiceBound) {
                unbindService(mmm);
                isServiceBound = false;
            }

            // 绑定服务并传递真实数据
            Intent serviceIntent = new Intent(this, FloatingViewService.class);
            bindService(serviceIntent, mmm, BIND_AUTO_CREATE);
        }
    }

    private void handleErrorResponse() {
        Toast.makeText(HomeActivity.this, "获取数据失败", Toast.LENGTH_SHORT).show();
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        isLoading = false;
    }

    private void handleNetworkError(Throwable t) {
        Toast.makeText(HomeActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        isLoading = false;
    }

    private void startBannerAutoScroll() {
        if (handler != null) {
            handler.removeCallbacks(bannerRunnable);
        }

        handler = new Handler(Looper.getMainLooper());
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null && !isFinishing() && !isDestroyed()) {
                    adapter.scrollBannerToNext();
                    handler.postDelayed(this, 3000);
                }
            }
        };
        handler.postDelayed(bannerRunnable, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanUpResources();
        unbindService(serviceConnection);
        unbindService(mmm);
        stopService(new Intent(this, FloatingViewService.class));
        stopService(new Intent(this, MusicService.class));
        isServiceBound = false;
    }

    private void cleanUpResources() {
        if (handler != null) {
            handler.removeCallbacks(bannerRunnable);
            handler = null;
        }

        if (isBound) {
            try {
                unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "解绑Service异常", e);
            }
            isBound = false;
        }

        // 清理GLIDE缓存
        Glide.get(this).clearMemory();

        if (allMusicList != null) {
            allMusicList.clear();
            allMusicList = null;
        }
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}