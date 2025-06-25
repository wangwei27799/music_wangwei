package com.wangwei.music_wangwei.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsUtils {
    private static final String LYRIC_PATTERN = "\\[(\\d+:\\d+(\\.\\d+)?)\\](.*)";
    private static final String TAG = "LyricsUtils";

    // 解析歌词文本，返回带时间戳的歌词行列表
    public static List<LyricLine> parseLyrics(String rawLyrics) {
        List<LyricLine> lyricLines = new ArrayList<>();
        if (rawLyrics == null || rawLyrics.isEmpty()) {
            return lyricLines;
        }

        // 按行分割歌词文本
        String[] lines = rawLyrics.split("\\n");
        for (String line : lines) {
            // 跳过空行
            if (line.trim().isEmpty()) {
                continue;
            }

            // 处理带时间戳的歌词行
            Matcher matcher = Pattern.compile(LYRIC_PATTERN).matcher(line);
            while (matcher.find()) {
                String timeStr = matcher.group(1);
                String content = matcher.group(3).trim();

                // 跳过纯空白内容
                if (content.isEmpty()) {
                    continue;
                }

                long timestamp = parseTimeStamp(timeStr);
                lyricLines.add(new LyricLine(timestamp, content));
            }
        }

        // 按时间戳排序
        Collections.sort(lyricLines, Comparator.comparingLong(l -> l.time));

        // 计算每行歌词的结束时间（下一行的开始时间）
        for (int i = 0; i < lyricLines.size() - 1; i++) {
            lyricLines.get(i).endTime = lyricLines.get(i + 1).time;
        }

        // 最后一行的结束时间设为最大值
        if (!lyricLines.isEmpty()) {
            lyricLines.get(lyricLines.size() - 1).endTime = Long.MAX_VALUE;
        }

        return lyricLines;
    }

    // 解析时间戳字符串为毫秒值
    private static long parseTimeStamp(String timeStr) {
        try {
            String[] parts = timeStr.split("[:\\.]");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            int millis = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            // 处理超过100毫秒的情况（例如歌词中的0.50秒）
            if (millis >= 100) {
                millis = 0; // 避免错误解析
            }

            return minutes * 60000 + seconds * 1000 + millis * 10;
        } catch (Exception e) {
            Log.e(TAG, "解析时间戳失败: " + timeStr, e);
            return 0;
        }
    }

    // 获取当前时间对应的歌词行索引
    public static int getCurrentLineIndex(List<LyricLine> lyricLines, long currentTime) {
        if (lyricLines == null || lyricLines.isEmpty()) {
            return -1;
        }

        // 二分查找当前时间对应的歌词行
        int left = 0;
        int right = lyricLines.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = (left + right) / 2;
            LyricLine line = lyricLines.get(mid);

            if (currentTime >= line.time && currentTime < line.endTime) {
                return mid;
            } else if (currentTime < line.time) {
                right = mid - 1;
            } else {
                left = mid + 1;
                result = mid; // 记录最后一个可能的位置
            }
        }

        return result;
    }

    // 生成带高亮的歌词文本
    public static SpannableString getHighlightedLyrics(List<LyricLine> lyricLines, int highlightIndex, int highlightColor) {
        if (lyricLines == null || lyricLines.isEmpty()) {
            return new SpannableString("暂无歌词");
        }

        // 构建完整歌词文本
        StringBuilder fullText = new StringBuilder();
        for (LyricLine line : lyricLines) {
            fullText.append(line.content).append("\n");
        }

        SpannableString spannable = new SpannableString(fullText.toString());

        // 如果没有需要高亮的行，直接返回
        if (highlightIndex < 0 || highlightIndex >= lyricLines.size()) {
            return spannable;
        }

        // 计算高亮行的起始和结束位置
        int startIndex = 0;
        for (int i = 0; i < highlightIndex; i++) {
            startIndex = fullText.indexOf("\n", startIndex) + 1;
            if (startIndex <= 0) break; // 防止索引越界
        }

        int endIndex = fullText.indexOf("\n", startIndex);
        if (endIndex == -1) {
            endIndex = fullText.length(); // 最后一行
        }

        // 设置高亮颜色
        spannable.setSpan(
                new ForegroundColorSpan(highlightColor),
                startIndex, endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannable;
    }

    // 歌词行数据结构
    public static class LyricLine {
        public long time;     // 开始时间（毫秒）
        public long endTime;  // 结束时间（毫秒）
        public String content; // 歌词内容

        public LyricLine(long time, String content) {
            this.time = time;
            this.content = content;
        }

        @Override
        public String toString() {
            return "[" + formatTime(time) + "] " + content;
        }

        private String formatTime(long timeMs) {
            long minutes = timeMs / 60000;
            long seconds = (timeMs % 60000) / 1000;
            long millis = timeMs % 1000;
            return String.format("%02d:%02d.%03d", minutes, seconds, millis);
        }
    }
}