package com.wangwei.music_wangwei.entity;

import java.util.List;

/**
 * 首页模块记录类
 */
public class Record {
    private int moduleConfigId;
    private String moduleName;
    private int style;
    private List<MusicInfo> musicInfoList;

    // Getters and Setters
    public int getModuleConfigId() { return moduleConfigId; }
    public void setModuleConfigId(int moduleConfigId) { this.moduleConfigId = moduleConfigId; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public int getStyle() { return style; }
    public void setStyle(int style) { this.style = style; }
    public List<MusicInfo> getMusicInfoList() { return musicInfoList; }
    public void setMusicInfoList(List<MusicInfo> musicInfoList) { this.musicInfoList = musicInfoList; }
}