package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.ThemeHelper;
import com.example.myapplication.util.WeatherApi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * CalendarActivity — 日历视图页
 * 功能：月份日历、天气显示、有笔记日期标记、点击显示当日笔记列表
 */
public class CalendarActivity extends AppCompatActivity {

    private MonthCalendarView calendarView;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerDayNotes;
    private TextView tvDateLabel, tvNoNotes;
    private NoteAdapter dayAdapter;
    private Set<String> markedDates = new HashSet<>();

    // 天气视图
    private View layoutWeather;
    private TextView tvWeatherCity, tvWeatherInfo, tvWeatherTemp;
    private TextView tvWeatherWind, tvWeatherAir, tvWeatherTips;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ThemeHelper themeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeHelper = ThemeHelper.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        themeHelper.applyToActivity(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        calendarView   = findViewById(R.id.calendar_view);
        swipeRefresh   = findViewById(R.id.swipe_refresh_calendar);
        calendarView.setPalette(themeHelper.getPalette());
        tvDateLabel    = findViewById(R.id.tv_date_label);
        tvNoNotes      = findViewById(R.id.tv_no_notes);
        recyclerDayNotes = findViewById(R.id.recycler_day_notes);
        recyclerDayNotes.setLayoutManager(new LinearLayoutManager(this));

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(() -> {
            loadMarkedDates();
            loadWeather();
            // 同时刷新当前选中日期的笔记
            Calendar selected = calendarView.getSelectedDate();
            if (selected != null) {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(selected.getTime());
                loadNotesForDate(currentDate);
            }
            swipeRefresh.setRefreshing(false);
        });

        // 天气视图引用
        layoutWeather  = findViewById(R.id.layout_weather);
        tvWeatherCity  = findViewById(R.id.tv_weather_city);
        tvWeatherInfo  = findViewById(R.id.tv_weather_info);
        tvWeatherTemp  = findViewById(R.id.tv_weather_temp);
        tvWeatherWind  = findViewById(R.id.tv_weather_wind);
        tvWeatherAir   = findViewById(R.id.tv_weather_air);
        tvWeatherTips  = findViewById(R.id.tv_weather_tips);

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
        dayAdapter.setPalette(themeHelper.getPalette());
        recyclerDayNotes.setAdapter(dayAdapter);

        // 日历日期变化监听
        calendarView.setOnDateSelectedListener((year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadNotesForDate(date);
        });

        // 底部导航
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // 导航在左侧，新页面从左侧滑入
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                // 导航在右侧，新页面从右侧滑入
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return true;
        });

        // 加载所有有笔记的日期
        loadMarkedDates();

        // 加载天气
        loadWeather();

        // 默认显示今天的笔记
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        loadNotesForDate(today);
    }

    /** 加载天气 */
    private void loadWeather() {
        SharedPreferences prefs = getSharedPreferences("notelink_prefs", MODE_PRIVATE);
        String city = prefs.getString("weather_city", "北京");

        // 先显示加载状态
        tvWeatherCity.setText(city);
        tvWeatherInfo.setText(R.string.label_weather_loading);
        tvWeatherTemp.setText("");
        tvWeatherWind.setText("");
        tvWeatherAir.setText("");
        tvWeatherTips.setText("");

        executor.execute(() -> {
            WeatherApi.WeatherData wd = WeatherApi.fetchSync(city);
            mainHandler.post(() -> {
                if (wd.success) {
                    tvWeatherCity.setText(wd.city);
                    tvWeatherInfo.setText(wd.weather);
                    tvWeatherTemp.setText(wd.temperature);
                    tvWeatherWind.setText(wd.wind);
                    if (wd.airQuality != null && !wd.airQuality.isEmpty()) {
                        tvWeatherAir.setText("空气 " + wd.airQuality);
                        tvWeatherAir.setVisibility(View.VISIBLE);
                    } else {
                        tvWeatherAir.setVisibility(View.GONE);
                    }
                    if (wd.tips != null && !wd.tips.isEmpty()) {
                        tvWeatherTips.setText(wd.tips);
                        tvWeatherTips.setVisibility(View.VISIBLE);
                    } else {
                        tvWeatherTips.setVisibility(View.GONE);
                    }
                } else {
                    tvWeatherInfo.setText(R.string.label_weather_failed);
                }
            });
        });
    }

    private void loadMarkedDates() {
        executor.execute(() -> {
            List<String> dates = AppDatabase.getInstance(this).noteDao().getAllDates();
            markedDates.clear();
            if (dates != null) markedDates.addAll(dates);
            runOnUiThread(() -> calendarView.setMarkedDates(markedDates));
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
        // 重置底部导航选中状态
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
        loadMarkedDates();
        themeHelper.applyToActivity(this);
        // 将主题色板传给自定义日历和 Adapter
        calendarView.setPalette(themeHelper.getPalette());
        dayAdapter.setPalette(themeHelper.getPalette());
        loadWeather();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
