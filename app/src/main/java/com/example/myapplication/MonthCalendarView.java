package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.util.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * MonthCalendarView — 自定义月历组件
 * 功能：月份导航（◀ ▶）、点击月份标题弹出年月快速选择器、
 *       有笔记的日期以深黑粗体高亮、主题色板动态切换
 */
public class MonthCalendarView extends LinearLayout {

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int dayOfMonth);
    }

    private Calendar currentMonth;   // 当前展示月份的第1天
    private Calendar selectedDate;   // 用户选中的日期
    private final Calendar today;    // 今天
    private Set<String> markedDates = new HashSet<>();
    private ThemeHelper.Palette palette;
    private OnDateSelectedListener listener;

    private TextView tvMonthYear;
    private TextView btnPrev, btnNext;
    private LinearLayout gridContainer;
    private View divider;

    private static final String[] WEEKDAYS = {"日", "一", "二", "三", "四", "五", "六"};

    public MonthCalendarView(Context context) {
        super(context);
        today = Calendar.getInstance();
        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate = (Calendar) today.clone();
        init(context);
    }

    /** XML inflate 需要的双参数构造函数 — 缺少此构造函数会导致 InflateException 闪退 */
    public MonthCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        today = Calendar.getInstance();
        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate = (Calendar) today.clone();
        init(context);
    }

    /** 三参数构造函数（defStyleAttr） */
    public MonthCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        today = Calendar.getInstance();
        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate = (Calendar) today.clone();
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);

        // ===== 头部：◀ 月份标题 ▶ =====
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dpToPx(context, 12);
        int padV = dpToPx(context, 8);
        headerRow.setPadding(padH, padV, padH, padV);

        btnPrev = new TextView(context);
        btnPrev.setText("◀");
        btnPrev.setTextSize(14);
        btnPrev.setPadding(dpToPx(context, 12), dpToPx(context, 4),
                dpToPx(context, 12), dpToPx(context, 4));
        btnPrev.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            rebuildCalendar();
        });
        headerRow.addView(btnPrev);

        tvMonthYear = new TextView(context);
        tvMonthYear.setTextSize(18);
        tvMonthYear.setTypeface(null, Typeface.BOLD);
        tvMonthYear.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvMonthYear.setLayoutParams(lpTitle);
        tvMonthYear.setOnClickListener(v -> showYearMonthPicker());
        headerRow.addView(tvMonthYear);

        btnNext = new TextView(context);
        btnNext.setText("▶");
        btnNext.setTextSize(14);
        btnNext.setPadding(dpToPx(context, 12), dpToPx(context, 4),
                dpToPx(context, 12), dpToPx(context, 4));
        btnNext.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            rebuildCalendar();
        });
        headerRow.addView(btnNext);

        addView(headerRow);

        // ===== 分割线 =====
        divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 1)));
        addView(divider);

        // ===== 日期网格容器 =====
        gridContainer = new LinearLayout(context);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        gridContainer.setPadding(dpToPx(context, 8), dpToPx(context, 4),
                dpToPx(context, 8), dpToPx(context, 8));
        addView(gridContainer);

        rebuildCalendar();
    }

    // ==================== 公共方法 ====================

    /** 设置主题色板 — 由 Activity 调用 */
    public void setPalette(ThemeHelper.Palette palette) {
        this.palette = palette;
        rebuildCalendar();
    }

    /** 设置有笔记的日期集合 (yyyy-MM-dd) */
    public void setMarkedDates(Set<String> dates) {
        this.markedDates = dates != null ? dates : new HashSet<>();
        rebuildCalendar();
    }

    /** 设置日期选择监听 */
    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    /** 跳转到指定年月并重绘 */
    public void goToDate(int year, int month) {
        currentMonth.set(Calendar.YEAR, year);
        currentMonth.set(Calendar.MONTH, month);
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        rebuildCalendar();
    }

    /** 获取当前选中的日期（Calendar 对象） */
    public Calendar getSelectedDate() {
        return (Calendar) selectedDate.clone();
    }

    /** 设置选中日期 */
    public void setSelectedDate(int year, int month, int dayOfMonth) {
        selectedDate.set(Calendar.YEAR, year);
        selectedDate.set(Calendar.MONTH, month);
        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        rebuildCalendar();
    }

    // ==================== 内部实现 ====================

    /** 弹出年月选择器 */
    private void showYearMonthPicker() {
        new YearMonthPickerDialog(getContext(),
                currentMonth.get(Calendar.YEAR),
                currentMonth.get(Calendar.MONTH),
                palette,
                (year, month) -> goToDate(year, month)
        ).show();
    }

    /** 完整重建日历网格 */
    @SuppressLint("SetTextI18n")
    private void rebuildCalendar() {
        if (palette == null) return;

        // 更新头部颜色
        tvMonthYear.setTextColor(palette.textPrimary);
        btnPrev.setTextColor(palette.primary);
        btnNext.setTextColor(palette.primary);

        // 月份标题：使用默认 Locale 格式化
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));

        // 分割线颜色
        divider.setBackgroundColor(palette.tagBg);

        // 清空网格
        gridContainer.removeAllViews();

        int cellSize = dpToPx(getContext(), 40);

        // ---- 星期标题行 ----
        LinearLayout weekRow = new LinearLayout(getContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String wd : WEEKDAYS) {
            TextView tv = new TextView(getContext());
            tv.setText(wd);
            tv.setTextSize(12);
            tv.setTextColor(palette.textHint);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, cellSize, 1f));
            weekRow.addView(tv);
        }
        gridContainer.addView(weekRow);

        // ---- 计算网格 ----
        Calendar cal = (Calendar) currentMonth.clone();
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        // 回退到周日
        cal.add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - 1));

        int thisMonth = currentMonth.get(Calendar.MONTH);
        int thisYear = currentMonth.get(Calendar.YEAR);

        // 最多6行
        for (int r = 0; r < 6; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            boolean rowHasContent = false;

            for (int c = 0; c < 7; c++) {
                final int year = cal.get(Calendar.YEAR);
                final int month = cal.get(Calendar.MONTH);
                final int day = cal.get(Calendar.DAY_OF_MONTH);

                TextView tv = new TextView(getContext());
                tv.setText(String.valueOf(day));
                tv.setTextSize(14);
                tv.setGravity(Gravity.CENTER);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, cellSize, 1f));

                boolean isCurrentMonth = (month == thisMonth && year == thisYear);
                boolean isToday = (year == today.get(Calendar.YEAR)
                        && month == today.get(Calendar.MONTH)
                        && day == today.get(Calendar.DAY_OF_MONTH));
                boolean isSelected = (year == selectedDate.get(Calendar.YEAR)
                        && month == selectedDate.get(Calendar.MONTH)
                        && day == selectedDate.get(Calendar.DAY_OF_MONTH));
                String dateStr = formatDate(year, month, day);
                boolean hasNotes = markedDates.contains(dateStr);

                if (isCurrentMonth) rowHasContent = true;

                // 样式应用
                if (isSelected) {
                    // 选中日期：主色圆形背景 + 背景色文字
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setColor(palette.primary);
                    int circleSize = dpToPx(getContext(), 32);
                    bg.setSize(circleSize, circleSize);
                    tv.setBackground(bg);
                    tv.setTextColor(palette.bg);
                    tv.setTypeface(null, Typeface.BOLD);
                } else if (hasNotes && isCurrentMonth) {
                    // 有笔记的日期：深黑色粗体高亮（夜间模式用纯白粗体代替）
                    boolean isNightMode = Color.red(palette.bg) < 64;
                    tv.setTextColor(isNightMode ? 0xFFFFFFFF : 0xFF000000);
                    tv.setTypeface(null, Typeface.BOLD);
                } else if (isToday && isCurrentMonth) {
                    // 今天：主色文字 + 粗体
                    tv.setTextColor(palette.primary);
                    tv.setTypeface(null, Typeface.BOLD);
                } else if (!isCurrentMonth) {
                    // 非本月日期：暗淡化
                    tv.setTextColor(palette.textHint);
                    tv.setAlpha(0.4f);
                } else {
                    // 普通日期
                    tv.setTextColor(palette.textPrimary);
                }

                // 点击事件
                tv.setOnClickListener(v -> {
                    selectedDate.set(year, month, day);
                    rebuildCalendar();
                    if (listener != null) {
                        listener.onDateSelected(year, month, day);
                    }
                });

                row.addView(tv);
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // 如果整行都不在当前月且后面也不会有 → 停止
            if (!rowHasContent) {
                // 检查是否已经完全没有当前月的日期
                if (cal.get(Calendar.MONTH) != thisMonth
                        || cal.get(Calendar.YEAR) != thisYear) {
                    // 确保当前月已经全部展示完毕
                    if (cal.get(Calendar.DAY_OF_MONTH) > 7) {
                        break;
                    }
                }
            }

            gridContainer.addView(row);
        }
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
    }

    private static int dpToPx(Context ctx, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                ctx.getResources().getDisplayMetrics());
    }
}
