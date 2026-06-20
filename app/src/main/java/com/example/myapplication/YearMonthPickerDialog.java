package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.myapplication.util.ThemeHelper;

import java.util.Calendar;

/**
 * YearMonthPickerDialog — 年月快速选择弹窗
 * 包含年份 NumberPicker + 月份 NumberPicker，确定后回调
 */
public class YearMonthPickerDialog {

    private final AlertDialog dialog;

    public interface OnYearMonthPickedListener {
        void onPicked(int year, int month);
    }

    public YearMonthPickerDialog(Context context, int currentYear, int currentMonth,
                                  ThemeHelper.Palette palette,
                                  OnYearMonthPickedListener listener) {
        // 使用代码构建对话框布局
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dpToPx(context, 16), dpToPx(context, 16),
                dpToPx(context, 16), dpToPx(context, 8));
        root.setGravity(android.view.Gravity.CENTER);

        // 年份选择器
        NumberPicker yearPicker = new NumberPicker(context);
        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(currentYear);
        yearPicker.setWrapSelectorWheel(false);
        setNumberPickerTextColor(yearPicker, palette.textPrimary);
        LinearLayout.LayoutParams lpYear = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        yearPicker.setLayoutParams(lpYear);
        root.addView(yearPicker);

        // 年份标签
        TextView tvYearLabel = new TextView(context);
        tvYearLabel.setText("年");
        tvYearLabel.setTextSize(16);
        tvYearLabel.setTextColor(palette.textPrimary);
        tvYearLabel.setPadding(dpToPx(context, 4), 0, dpToPx(context, 8), 0);
        root.addView(tvYearLabel);

        // 月份选择器
        NumberPicker monthPicker = new NumberPicker(context);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentMonth + 1);
        monthPicker.setWrapSelectorWheel(true);
        setNumberPickerTextColor(monthPicker, palette.textPrimary);
        LinearLayout.LayoutParams lpMonth = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        monthPicker.setLayoutParams(lpMonth);
        root.addView(monthPicker);

        // 月份标签
        TextView tvMonthLabel = new TextView(context);
        tvMonthLabel.setText("月");
        tvMonthLabel.setTextSize(16);
        tvMonthLabel.setTextColor(palette.textPrimary);
        tvMonthLabel.setPadding(dpToPx(context, 4), 0, 0, 0);
        root.addView(tvMonthLabel);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("选择年月");
        builder.setView(root);
        builder.setPositiveButton("确定", (d, w) -> {
            if (listener != null) {
                listener.onPicked(yearPicker.getValue(), monthPicker.getValue() - 1);
            }
        });
        builder.setNegativeButton("取消", null);

        dialog = builder.create();

        // 设置对话框背景色
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(palette.card));
        }
    }

    public void show() {
        dialog.show();
        // 设置按钮文字颜色
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                dialog.getContext().getResources().getColor(R.color.nl_primary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                dialog.getContext().getResources().getColor(R.color.nl_text_hint));
    }

    /** 设置 NumberPicker 的文字颜色 */
    private void setNumberPickerTextColor(NumberPicker picker, int color) {
        try {
            java.lang.reflect.Field selectorWheelPaintField =
                    NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            android.graphics.Paint paint = (android.graphics.Paint) selectorWheelPaintField.get(picker);
            if (paint != null) {
                paint.setColor(color);
            }
            picker.invalidate();
        } catch (Exception ignored) {
            // 反射失败则使用默认颜色
        }
    }

    private static int dpToPx(Context ctx, float dp) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, dp,
                ctx.getResources().getDisplayMetrics());
    }
}
