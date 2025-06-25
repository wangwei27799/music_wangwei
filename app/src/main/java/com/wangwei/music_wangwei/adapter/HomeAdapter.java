package com.wangwei.music_wangwei.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.entity.Data;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.entity.Record;
import com.wangwei.music_wangwei.event.MusicItemClickListener;
import com.wangwei.music_wangwei.response.HomePageResponse;

import java.time.Instant;
import java.util.List;


public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_BANNER = 1;
    private static final int VIEW_TYPE_HORIZONTAL_CARD = 2;
    private static final int VIEW_TYPE_ONE_COLUMN = 3;
    private static final int VIEW_TYPE_TWO_COLUMNS = 4;

    private Context context;
    private Data data;
    private ViewPager2 bannerViewPager;
    private Handler handler;
    private Runnable bannerRunnable;
    private int currentBannerPosition = 0;

    private MusicItemClickListener itemClickListener;

    public HomeAdapter(Context context) {
        this.context = context;
        this.handler = new Handler();
    }

    public void setItemClickListener(MusicItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setData(Data data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addData(Data newData) {
        if (this.data == null) {
            this.data = newData;
        } else {
            this.data.getRecords().addAll(newData.getRecords());
        }
        notifyDataSetChanged();
    }

    public int getTotalPages() {
        return data != null ? data.getPages() : 0;
    }

    public void scrollBannerToNext() {
        if (bannerViewPager != null) {
            int count = bannerViewPager.getAdapter() != null ? bannerViewPager.getAdapter().getItemCount() : 0;
            if (count > 1) {
                currentBannerPosition = (currentBannerPosition + 1) % count;
                bannerViewPager.setCurrentItem(currentBannerPosition, true);
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case VIEW_TYPE_BANNER:
                View bannerView = inflater.inflate(R.layout.item_banner, parent, false);
                return new BannerViewHolder(bannerView);
            case VIEW_TYPE_HORIZONTAL_CARD:
                View horizontalCardView = inflater.inflate(R.layout.item_horizontal_card, parent, false);
                return new HorizontalCardViewHolder(horizontalCardView);
            case VIEW_TYPE_ONE_COLUMN:
                View oneColumnView = inflater.inflate(R.layout.item_one_column, parent, false);
                return new OneColumnViewHolder(oneColumnView);
            case VIEW_TYPE_TWO_COLUMNS:
                View twoColumnsView = inflater.inflate(R.layout.item_two_columns, parent, false);
                return new TwoColumnsViewHolder(twoColumnsView);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (data == null || data.getRecords() == null || data.getRecords().size() <= position) {
            return;
        }

        Record record = data.getRecords().get(position);

        switch (record.getStyle()) {
            case 1: // Banner
                bindBannerViewHolder((BannerViewHolder) holder, record);
                break;
            case 2: // 横滑大卡
                bindHorizontalCardViewHolder((HorizontalCardViewHolder) holder, record);
                break;
            case 3: // 一行一列
                bindOneColumnViewHolder((OneColumnViewHolder) holder, record);
                break;
            case 4: // 一行两列
                bindTwoColumnsViewHolder((TwoColumnsViewHolder) holder, record);
                break;
        }
    }

    private void bindBannerViewHolder(BannerViewHolder holder, Record record) {
        List<MusicInfo> musicInfoList = record.getMusicInfoList();
        BannerAdapter bannerAdapter = new BannerAdapter(context, musicInfoList);
        holder.bannerViewPager.setAdapter(bannerAdapter);

        // 设置子适配器的点击事件
        bannerAdapter.setItemClickListener((position, musicInfo) -> {
            if (itemClickListener != null) {
                itemClickListener.onMusicItemClick(position, musicInfo);
            }
        });

        // 手动绑定TabLayout和ViewPager2
        setupTabLayoutWithViewPager2(holder.bannerIndicator, holder.bannerViewPager, musicInfoList.size());
        // 保存ViewPager2实例用于自动轮播
        this.bannerViewPager = holder.bannerViewPager;

        // 单图时隐藏指示器
        if (musicInfoList.size() <= 1) {
            holder.bannerIndicator.setVisibility(View.GONE);
        } else {
            holder.bannerIndicator.setVisibility(View.VISIBLE);

            // 设置ViewPager2滑动监听
            holder.bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    currentBannerPosition = position;
                }
            });
        }
    }

    /**
     * 手动绑定TabLayout和ViewPager2
     */
    private void setupTabLayoutWithViewPager2(TabLayout tabLayout, ViewPager2 viewPager2, int itemCount) {
        // 清除所有Tab
        tabLayout.removeAllTabs();

        // 添加Tab
        for (int i = 0; i < itemCount; i++) {
            tabLayout.addTab(tabLayout.newTab());
        }

        // 设置Tab选中监听
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void bindHorizontalCardViewHolder(HorizontalCardViewHolder holder, Record record) {
        List<MusicInfo> musicInfoList = record.getMusicInfoList();
        HorizontalCardAdapter adapter = new HorizontalCardAdapter(context, musicInfoList);
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerView.setAdapter(adapter);

        // 设置子适配器的点击事件
        adapter.setItemClickListener((position, musicInfo) -> {
            if (itemClickListener != null) {
                itemClickListener.onMusicItemClick(position, musicInfo);
            }
        });
    }

    private void bindOneColumnViewHolder(OneColumnViewHolder holder, Record record) {
        List<MusicInfo> musicInfoList = record.getMusicInfoList();
        OneColumnAdapter adapter = new OneColumnAdapter(context, musicInfoList);
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.recyclerView.setAdapter(adapter);

        adapter.setItemClickListener((position, musicInfo) -> {
            if (itemClickListener != null) {
                itemClickListener.onMusicItemClick(position, musicInfo);
            }
        });
    }

    private void bindTwoColumnsViewHolder(TwoColumnsViewHolder holder, Record record) {
        List<MusicInfo> musicInfoList = record.getMusicInfoList();
        TwoColumnsAdapter adapter = new TwoColumnsAdapter(context, musicInfoList);
        holder.recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        holder.recyclerView.setAdapter(adapter);

        adapter.setItemClickListener((position, musicInfo) -> {
            if (itemClickListener != null) {
                itemClickListener.onMusicItemClick(position, musicInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data != null ? data.getRecords().size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (data == null || data.getRecords() == null || data.getRecords().size() <= position) {
            return super.getItemViewType(position);
        }

        Record record = data.getRecords().get(position);
        switch (record.getStyle()) {
            case 1:
                return VIEW_TYPE_BANNER;
            case 2:
                return VIEW_TYPE_HORIZONTAL_CARD;
            case 3:
                return VIEW_TYPE_ONE_COLUMN;
            case 4:
                return VIEW_TYPE_TWO_COLUMNS;
            default:
                return super.getItemViewType(position);
        }
    }

    // ViewHolder类
    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ViewPager2 bannerViewPager;
        TabLayout bannerIndicator;

        BannerViewHolder(View itemView) {
            super(itemView);
            bannerViewPager = itemView.findViewById(R.id.banner_view_pager);
            bannerIndicator = itemView.findViewById(R.id.banner_indicator);
        }
    }

    static class HorizontalCardViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;

        HorizontalCardViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
        }
    }

    static class OneColumnViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;

        OneColumnViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
        }
    }

    static class TwoColumnsViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;

        TwoColumnsViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
        }
    }

    /**
     * 释放资源，避免内存泄漏
     */
    public void release() {

        // 2. 移除点击监听器引用
        itemClickListener = null;

        // 3. 释放Context引用
        context = null;

        // 4. 取消所有Glide加载
        if (context != null) {
            Glide.with(context).pauseRequests();
        }
    }
}