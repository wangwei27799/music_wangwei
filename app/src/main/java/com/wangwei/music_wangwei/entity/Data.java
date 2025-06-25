package com.wangwei.music_wangwei.entity;

import java.util.Collection;
import java.util.List;

/**
 * 响应数据中的Data字段
 */
public class Data {
    private List<Record> records;
    private int total;
    private int size;
    private int current;
    private int pages;

    // Getters and Setters
    public List<Record> getRecords() { return records; }
    public void setRecords(List<Record> records) { this.records = records; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getCurrent() { return current; }
    public void setCurrent(int current) { this.current = current; }
    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }
}