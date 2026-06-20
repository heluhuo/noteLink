package com.example.myapplication.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.myapplication.R;

import java.util.List;

/**
 * 带主题支持的 ArrayAdapter
 * Spinner 的 dropdown 视图不在 Activity view tree 中，
 * ThemeHelper.applyToView 无法触及，因此需要适配器自行应用主题。
 */
public class ThemedArrayAdapter<T> extends ArrayAdapter<T> {

    private final ThemeHelper.Palette palette;

    public ThemedArrayAdapter(@NonNull Context context, @NonNull ThemeHelper.Palette palette,
                              @NonNull List<T> items) {
        super(context, R.layout.item_spinner_dropdown, items);
        this.palette = palette;
        setDropDownViewResource(R.layout.item_spinner_dropdown);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        applyColors(v);
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = super.getDropDownView(position, convertView, parent);
        applyColors(v);
        // 下拉项背景色
        v.setBackgroundColor(palette.card);
        return v;
    }

    private void applyColors(View v) {
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(palette.textPrimary);
        }
    }
}
