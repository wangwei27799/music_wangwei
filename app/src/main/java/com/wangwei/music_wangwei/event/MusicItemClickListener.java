// MusicItemClickListener.java
package com.wangwei.music_wangwei.event;

import com.wangwei.music_wangwei.entity.MusicInfo;

/**
 * 音乐列表点击事件统一接口
 */
public interface MusicItemClickListener {
    void onMusicItemClick(int position, MusicInfo musicInfo);
}