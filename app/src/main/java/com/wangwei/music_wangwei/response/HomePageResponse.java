package com.wangwei.music_wangwei.response;

import com.wangwei.music_wangwei.entity.Data;

import java.util.List;

/**
 * 获取首页数据接口的响应数据模型
 */
public class HomePageResponse {
    private int code;
    private String msg;
    private Data data;

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

}