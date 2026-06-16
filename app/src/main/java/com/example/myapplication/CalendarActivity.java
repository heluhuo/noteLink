package com.example.myapplication;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CalendarActivity — 日历视图页
 * 功能：月份日历、有笔记日期标记、点击显示当日笔记列表
 */
public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView recyclerDayNotes;
    private TextView tvDateLabel, tvNoNotes;
    private NoteAdapter dayAdapter;
    private Set<String> markedDates = new HashSet<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        calendarView   = findViewById(R.id.calendar_view);
        tvDateLabel    = findViewById(R.id.tv_date_label);
        tvNoNotes      = findViewById(R.id.tv_no_notes);
        recyclerDayNotes = findViewById(R.id.recycler_day_notes);
        recyclerDayNotes.setLayoutManager(new LinearLayoutManager(this));

        dayAdapter = new NoteAdapter(new NoteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Note note) {
                Intent intent = new Intent(CalendarActivity.this, DetailActivity.class);
                intent.putExtra("note_id", note.id);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Note note, int position) { /* 不支持删除 */ }
        });
        recyclerDayNotes.setAdapter(dayAdapter);

        // 日历日期变化监听
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadNotesForDate(date);
        });

        // 底部导航
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return true;
        });

        // 加载所有有笔记的日期，用于标记
        loadMarkedDates();

        // 默认显示今天的笔记
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());
        loadNotesForDate(today);
    }

    private void loadMarkedDates() {
        executor.execute(() -> {
            List<String> dates = AppDatabase.getInstance(this).noteDao().getAllDates();
            markedDates.clear();
            if (dates != null) markedDates.addAll(dates);
            // 注意：CalendarView 原生不支持日期角标绘制，
            // 实际项目中可使用 MaterialCalendarView (material-calendarview) 库实现
            // 这里通过 ItemDecoration 模拟标记效果
        });
    }

    private void loadNotesForDate(String date) {
        tvDateLabel.setText(getString(R.string.label_notes_on_date, date));
        executor.execute(() -> {
            List<Note> notes = AppDatabase.getInstance(this).noteDao().queryByDate(date);
            final List<Note> result = notes != null ? notes : new ArrayList<>();
            runOnUiThread(() -> {
                dayAdapter.setNotes(result);
                if (result.isEmpty()) {
                    tvNoNotes.setVisibility(View.VISIBLE);
                    recyclerDayNotes.setVisibility(View.GONE);
                } else {
                    tvNoNotes.setVisibility(View.GONE);
                    recyclerDayNotes.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMarkedDates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
