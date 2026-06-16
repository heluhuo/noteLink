package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SettingsActivity — 设置页
 * 功能：分类管理、每日提醒、数据导出
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "notelink_prefs";
    private static final String KEY_REMINDER  = "reminder_enabled";
    private static final String KEY_REMIND_H  = "reminder_hour";
    private static final String KEY_REMIND_M  = "reminder_minute";

    private SwitchMaterial switchReminder;
    private View layoutTimePicker;
    private MaterialButton btnPickTime;
    private SharedPreferences prefs;
    private int reminderHour = 21, reminderMinute = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 分类列表
        TextView tvCategories = findViewById(R.id.tv_categories);
        loadCategories(tvCategories);

        MaterialButton btnManageCat = findViewById(R.id.btn_manage_categories);
        btnManageCat.setOnClickListener(v ->
                Toast.makeText(this, "分类管理：在编辑页直接输入分类名即可", Toast.LENGTH_LONG).show()
        );

        // 提醒设置
        switchReminder  = findViewById(R.id.switch_reminder);
        layoutTimePicker = findViewById(R.id.layout_time_picker);
        btnPickTime     = findViewById(R.id.btn_pick_time);

        boolean enabled = prefs.getBoolean(KEY_REMINDER, false);
        reminderHour    = prefs.getInt(KEY_REMIND_H, 21);
        reminderMinute  = prefs.getInt(KEY_REMIND_M, 0);

        switchReminder.setChecked(enabled);
        layoutTimePicker.setVisibility(enabled ? View.VISIBLE : View.GONE);
        updateTimeButton();

        switchReminder.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_REMINDER, isChecked).apply();
            layoutTimePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) scheduleReminder(); else cancelReminder();
        });

        btnPickTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                reminderHour   = hour;
                reminderMinute = minute;
                prefs.edit().putInt(KEY_REMIND_H, hour).putInt(KEY_REMIND_M, minute).apply();
                updateTimeButton();
                if (switchReminder.isChecked()) scheduleReminder();
            }, reminderHour, reminderMinute, true).show();
        });

        // 数据导出
        MaterialButton btnExport    = findViewById(R.id.btn_export);
        MaterialButton btnExportTxt = findViewById(R.id.btn_export_txt);
        btnExport.setOnClickListener(v -> exportNotes("json"));
        btnExportTxt.setOnClickListener(v -> exportNotes("txt"));
    }

    private void loadCategories(TextView tv) {
        executor.execute(() -> {
            List<String> cats = AppDatabase.getInstance(this).noteDao().getAllCategories();
            String display = (cats == null || cats.isEmpty()) ? "暂无分类" : String.join("、", cats);
            runOnUiThread(() -> tv.setText(display));
        });
    }

    private void updateTimeButton() {
        btnPickTime.setText(String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute));
    }

    /** 设置每日定时通知 */
    private void scheduleReminder() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, reminderHour);
        cal.set(Calendar.MINUTE, reminderMinute);
        cal.set(Calendar.SECOND, 0);
        // 如果今天的时间已过，从明天开始
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        if (am != null) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
            Toast.makeText(this, "已设置每日 " + btnPickTime.getText() + " 提醒", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelReminder() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
        Toast.makeText(this, "已关闭每日提醒", Toast.LENGTH_SHORT).show();
    }

    /** 导出笔记 */
    private void exportNotes(String format) {
        executor.execute(() -> {
            List<Note> notes = AppDatabase.getInstance(this).noteDao().getAll();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String filename = "notelink_export_" + timestamp + "." + format;
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getFilesDir();
            File file = new File(dir, filename);
            try (FileWriter fw = new FileWriter(file)) {
                if ("json".equals(format)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    fw.write(gson.toJson(notes));
                } else {
                    for (Note n : notes) {
                        fw.write("==============================\n");
                        fw.write("标题：" + n.title + "\n");
                        fw.write("日期：" + n.date + "\n");
                        fw.write("分类：" + n.category + "\n");
                        fw.write("标签：" + n.tags + "\n\n");
                        fw.write(n.content + "\n\n");
                    }
                }
                final String msg = getString(R.string.msg_export_success, file.getAbsolutePath());
                runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
