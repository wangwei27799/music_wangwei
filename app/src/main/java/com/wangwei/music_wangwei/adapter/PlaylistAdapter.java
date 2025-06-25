package com.wangwei.music_wangwei.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.activity.PlaylistDialog;
import com.wangwei.music_wangwei.entity.MusicInfo;

import java.util.List;

public class PlaylistAdapter extends BaseAdapter {

    private Context context;
    private List<MusicInfo> musicList;
    private int currentPosition;

    public PlaylistAdapter(Context context, List<MusicInfo> musicList, int currentPosition) {
        this.context = context;
        this.musicList = musicList;
        this.currentPosition = currentPosition;
    }

    @Override
    public int getCount() {
        return musicList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
            holder = new ViewHolder();
            holder.tvSongName = convertView.findViewById(R.id.tv_playlist_song_name);
            holder.tvArtist = convertView.findViewById(R.id.tv_playlist_artist);
            holder.ivDelete = convertView.findViewById(R.id.iv_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MusicInfo musicInfo = musicList.get(position);
        holder.tvSongName.setText(musicInfo.getMusicName());
        holder.tvArtist.setText(musicInfo.getAuthor());

        holder.ivDelete.setOnClickListener(v -> {
            PlaylistDialog dialog = new PlaylistDialog(context, musicList, currentPosition);
            dialog.deleteItem(position);
            dialog.show(); // 显示对话框
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView tvSongName;
        TextView tvArtist;
        ImageView ivDelete;
    }
}