package com.example.myapplication.util;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;

import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内链解析工具类
 * 将笔记内容中的 [[标题]] 解析为可点击的 SpannableString
 */
public class LinkParser {

    /** 匹配 [[任意内容]] 的正则 */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(.+?)\\]\\]");

    /**
     * 解析内链，返回带高亮可点击 Span 的 SpannableString
     * @param context   上下文（用于启动 DetailActivity）
     * @param rawText   原始文本
     * @param primaryColor 主题色（用于链接颜色）
     */
    public static SpannableString parse(Context context, String rawText, int primaryColor) {
        if (rawText == null) return new SpannableString("");
        SpannableString ss = new SpannableString(rawText);
        Matcher m = LINK_PATTERN.matcher(rawText);
        while (m.find()) {
            String linkTitle = m.group(1);
            int start = m.start();
            int end = m.end();
            ss.setSpan(new LinkSpan(context, linkTitle, primaryColor),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }

    /** 从文本中提取所有内链标题列表 */
    public static java.util.List<String> extractLinkTitles(String rawText) {
        java.util.List<String> titles = new java.util.ArrayList<>();
        if (rawText == null) return titles;
        Matcher m = LINK_PATTERN.matcher(rawText);
        while (m.find()) {
            titles.add(m.group(1));
        }
        return titles;
    }

    // ---- 内部 ClickableSpan 实现 ----

    private static class LinkSpan extends ClickableSpan {
        private final Context context;
        private final String targetTitle;
        private final int color;

        LinkSpan(Context context, String targetTitle, int color) {
            this.context = context;
            this.targetTitle = targetTitle;
            this.color = color;
        }

        @Override
        public void onClick(@NonNull View widget) {
            // 在 IO 线程查询目标笔记，然后跳转
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                Note target = db.noteDao().findByTitle(targetTitle);
                if (target != null) {
                    Intent intent = new Intent();
                    intent.setClassName(context, "com.example.myapplication.DetailActivity");
                    intent.putExtra("note_id", target.id);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(color);
            ds.setUnderlineText(true);
        }
    }
}
