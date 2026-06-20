package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.RegionData;
import com.example.myapplication.util.ThemeHelper;
import com.example.myapplication.util.ThemedArrayAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SettingsActivity — 设置页
 * 功能：分类管理、主题切换、天气城市、每日提醒、数据导出
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "notelink_prefs";
    private static final String KEY_REMINDER  = "reminder_enabled";
    private static final String KEY_REMIND_H  = "reminder_hour";
    private static final String KEY_REMIND_M  = "reminder_minute";
    private static final String KEY_WEATHER   = "weather_city";

    private SwitchMaterial switchReminder;
    private SwitchMaterial switchNightMode;
    private View layoutTimePicker;
    private MaterialButton btnPickTime;
    private SharedPreferences prefs;
    private int reminderHour = 21, reminderMinute = 0;

    // 主题
    private Spinner spinnerTheme;
    private ThemeHelper themeHelper;

    // 天气城市
    private MaterialCardView cardWeatherCity;
    private TextView tvWeatherRegion, tvWeatherProvince;
    private String currentCity = "北京";
    private String currentProvince = "北京市";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeHelper = ThemeHelper.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        themeHelper.applyToActivity(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // ---- 日夜模式切换 ----
        switchNightMode = findViewById(R.id.switch_night_mode);
        switchNightMode.setChecked(themeHelper.isNightMode());
        switchNightMode.setOnCheckedChangeListener((btn, isChecked) -> {
            themeHelper.setNightMode(isChecked);
            // 立即重新应用主题（无需重启 Activity）
            themeHelper.applyToActivity(this);
            refreshSpinnerPopups();
            Toast.makeText(this,
                    isChecked ? "已切换为夜间模式" : "已切换为日间模式",
                    Toast.LENGTH_SHORT).show();
        });

        // ---- 主题切换 ----
        spinnerTheme = findViewById(R.id.spinner_theme);
        ThemeHelper.Palette palette = themeHelper.getPalette();
        java.util.List<String> themeNameList = java.util.Arrays.asList(ThemeHelper.THEME_NAMES);
        ThemedArrayAdapter<String> themeAdapter = new ThemedArrayAdapter<>(
                this, palette, themeNameList);
        spinnerTheme.setAdapter(themeAdapter);
        spinnerTheme.setPopupBackgroundDrawable(ThemeHelper.createSpinnerPopupBg(palette));
        spinnerTheme.setSelection(themeHelper.getCurrentThemeIndex());
        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != themeHelper.getCurrentThemeIndex()) {
                    themeHelper.setTheme(position);
                    // 立即应用主题到当前页面
                    themeHelper.applyToActivity(SettingsActivity.this);
                    refreshSpinnerPopups();
                    Toast.makeText(SettingsActivity.this,
                            "风格已切换为: " + ThemeHelper.THEME_NAMES[position],
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ---- 天气城市 ----
        cardWeatherCity = findViewById(R.id.card_weather_city);
        tvWeatherRegion = findViewById(R.id.tv_weather_region);
        tvWeatherProvince = findViewById(R.id.tv_weather_province);

        String savedCity = prefs.getString(KEY_WEATHER, "北京");
        currentCity = savedCity;
        currentProvince = RegionData.getProvinceForCity(savedCity);
        updateRegionDisplay();

        cardWeatherCity.setOnClickListener(v -> showRegionPicker());

        // ---- 分类管理 ----
        TextView tvCategories = findViewById(R.id.tv_categories);
        loadCategories(tvCategories);

        MaterialButton btnManageCat = findViewById(R.id.btn_manage_categories);
        btnManageCat.setOnClickListener(v -> showCategoryManageDialog());

        // ---- 提醒设置 ----
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

        // ---- 数据导出 ----
        MaterialButton btnExport    = findViewById(R.id.btn_export);
        MaterialButton btnExportTxt = findViewById(R.id.btn_export_txt);
        btnExport.setOnClickListener(v -> exportNotes("json"));
        btnExportTxt.setOnClickListener(v -> exportNotes("txt"));
    }

    private void loadCategories(TextView tv) {
        executor.execute(() -> {
            List<String> cats = AppDatabase.getInstance(this).noteDao().getAllCategories();
            List<String> allCats = new ArrayList<>();
            if (cats != null) allCats.addAll(cats);

            // 合并 SharedPreferences 中用户手动创建的空分类
            SharedPreferences catPrefs = getSharedPreferences("notelink_categories", MODE_PRIVATE);
            java.util.Set<String> userCats = catPrefs.getStringSet("user_categories", new java.util.HashSet<>());
            for (String uc : userCats) {
                if (!allCats.contains(uc)) allCats.add(uc);
            }

            String display = allCats.isEmpty() ? "暂无分类" : String.join("、", allCats);
            runOnUiThread(() -> tv.setText(display));
        });
    }

    /** 更新城市显示 */
    private void updateRegionDisplay() {
        tvWeatherRegion.setText(currentCity);
        tvWeatherProvince.setText(currentProvince);
    }

    /** 显示省-市二级联动选择器 */
    private void showRegionPicker() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_region_picker, null);

        Spinner spinnerProvince = dialogView.findViewById(R.id.spinner_province);
        Spinner spinnerCity = dialogView.findViewById(R.id.spinner_city);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm_city);

        // 省份列表
        java.util.List<String> provinces = RegionData.getProvinces();
        ThemeHelper.Palette p = themeHelper.getPalette();
        ThemedArrayAdapter<String> provinceAdapter = new ThemedArrayAdapter<>(
                this, p, provinces);
        spinnerProvince.setAdapter(provinceAdapter);
        spinnerProvince.setPopupBackgroundDrawable(ThemeHelper.createSpinnerPopupBg(p));

        // 城市列表（根据省份动态更新）
        ThemedArrayAdapter<String> cityAdapter = new ThemedArrayAdapter<>(
                this, p, new ArrayList<>());
        spinnerCity.setAdapter(cityAdapter);
        spinnerCity.setPopupBackgroundDrawable(ThemeHelper.createSpinnerPopupBg(p));

        // 恢复当前选择
        int provinceIdx = RegionData.getProvinceIndex(currentCity);
        if (provinceIdx >= 0 && provinceIdx < provinces.size()) {
            spinnerProvince.setSelection(provinceIdx);
        }

        // 省份变化 → 更新城市列表
        spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String province = provinces.get(position);
                String[] cities = RegionData.getCities(province);
                cityAdapter.clear();
                if (cities != null) {
                    for (String c : cities) cityAdapter.add(c);
                }
                cityAdapter.notifyDataSetChanged();
                // 如果当前城市属于该省份，选中它
                int cityIdx = RegionData.getCityIndex(province, currentCity);
                if (cityIdx >= 0 && cityIdx < cityAdapter.getCount()) {
                    spinnerCity.setSelection(cityIdx);
                } else {
                    spinnerCity.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(themeHelper.getPalette().card));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String selectedProvince = (String) spinnerProvince.getSelectedItem();
            String selectedCity = (String) spinnerCity.getSelectedItem();
            if (selectedProvince == null || selectedCity == null) {
                Toast.makeText(this, "请选择省份和城市", Toast.LENGTH_SHORT).show();
                return;
            }
            currentProvince = selectedProvince;
            currentCity = selectedCity;
            prefs.edit().putString(KEY_WEATHER, currentCity).apply();
            updateRegionDisplay();
            Toast.makeText(this, "天气城市已设为: " + currentCity, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    /** 显示分类管理对话框 — 列表 + 删除 / 重命名 */
    private void showCategoryManageDialog() {
        // 合并数据库分类 + SharedPreferences 用户分类
        executor.execute(() -> {
            List<String> dbCats = AppDatabase.getInstance(this).noteDao().getAllCategories();
            List<String> allCats = new ArrayList<>();
            if (dbCats != null) allCats.addAll(dbCats);
            SharedPreferences catPrefs = getSharedPreferences("notelink_categories", MODE_PRIVATE);
            java.util.Set<String> userCats = catPrefs.getStringSet("user_categories", new java.util.HashSet<>());
            for (String uc : userCats) {
                if (!allCats.contains(uc)) allCats.add(uc);
            }

            runOnUiThread(() -> {
                if (allCats.isEmpty()) {
                    Toast.makeText(this, "暂无分类", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] catArray = allCats.toArray(new String[0]);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("管理分类");
                builder.setItems(catArray, (dialog, which) -> {
                    String selected = catArray[which];
                    showCategoryActionDialog(selected);
                });
                builder.setNegativeButton("关闭", null);
                builder.show();
            });
        });
    }

    /** 对选中的分类显示操作选项：重命名 / 删除 */
    private void showCategoryActionDialog(String category) {
        new AlertDialog.Builder(this)
                .setTitle("分类: " + category)
                .setItems(new String[]{"重命名", "删除"}, (dialog, which) -> {
                    if (which == 0) {
                        showRenameCategoryDialog(category);
                    } else {
                        showDeleteCategoryDialog(category);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 重命名分类对话框 */
    private void showRenameCategoryDialog(String oldName) {
        EditText etInput = new EditText(this);
        etInput.setText(oldName);
        etInput.setPadding(48, 24, 48, 24);
        etInput.setSelection(oldName.length());

        new AlertDialog.Builder(this)
                .setTitle("重命名分类")
                .setView(etInput)
                .setPositiveButton("确定", (d, w) -> {
                    String newName = etInput.getText().toString().trim();
                    if (newName.isEmpty() || newName.equals(oldName)) return;
                    renameCategory(oldName, newName);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 执行分类重命名：更新 SharedPreferences + DB */
    private void renameCategory(String oldName, String newName) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            // 更新数据库中该分类的所有笔记
            List<Note> notesInCat = db.noteDao().queryByCategory(oldName);
            if (notesInCat != null) {
                for (Note n : notesInCat) {
                    n.category = newName;
                    n.updatedAt = System.currentTimeMillis();
                    db.noteDao().update(n);
                }
            }
            // 更新 SharedPreferences 用户分类
            SharedPreferences catPrefs = getSharedPreferences("notelink_categories", MODE_PRIVATE);
            java.util.Set<String> userCats = new java.util.HashSet<>(
                    catPrefs.getStringSet("user_categories", new java.util.HashSet<>()));
            if (userCats.contains(oldName)) {
                userCats.remove(oldName);
                userCats.add(newName);
                catPrefs.edit().putStringSet("user_categories", userCats).apply();
            }

            runOnUiThread(() -> {
                TextView tvCategories = findViewById(R.id.tv_categories);
                loadCategories(tvCategories);
                Toast.makeText(this, "分类已重命名为: " + newName, Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** 删除分类确认对话框 */
    private void showDeleteCategoryDialog(String category) {
        new AlertDialog.Builder(this)
                .setTitle("删除分类: " + category)
                .setMessage("该分类下的所有笔记将变为「未分类」，确定删除？")
                .setPositiveButton("删除", (d, w) -> deleteCategory(category))
                .setNegativeButton("取消", null)
                .show();
    }

    /** 执行分类删除：移除 SharedPreferences 条目 + DB 笔记归入未分类 */
    private void deleteCategory(String category) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            // 将该分类下笔记归入「未分类」
            List<Note> notesInCat = db.noteDao().queryByCategory(category);
            if (notesInCat != null) {
                for (Note n : notesInCat) {
                    n.category = "未分类";
                    n.updatedAt = System.currentTimeMillis();
                    db.noteDao().update(n);
                }
            }
            // 从 SharedPreferences 移除
            SharedPreferences catPrefs = getSharedPreferences("notelink_categories", MODE_PRIVATE);
            java.util.Set<String> userCats = new java.util.HashSet<>(
                    catPrefs.getStringSet("user_categories", new java.util.HashSet<>()));
            if (userCats.contains(category)) {
                userCats.remove(category);
                catPrefs.edit().putStringSet("user_categories", userCats).apply();
            }

            runOnUiThread(() -> {
                TextView tvCategories = findViewById(R.id.tv_categories);
                loadCategories(tvCategories);
                Toast.makeText(this, "分类\"" + category + "\"已删除", Toast.LENGTH_SHORT).show();
            });
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置页无底部导航栏，无需重置
        themeHelper.applyToActivity(this);
        // 刷新 Spinner 弹窗背景（popup 不在 view tree 中）
        refreshSpinnerPopups();
    }

    /** 刷新所有 Spinner 的弹窗背景和适配器色板 */
    private void refreshSpinnerPopups() {
        ThemeHelper.Palette p = themeHelper.getPalette();
        android.graphics.drawable.Drawable popupBg = ThemeHelper.createSpinnerPopupBg(p);
        if (spinnerTheme != null) {
            // 重新创建适配器以更新调色板
            java.util.List<String> themeNameList = java.util.Arrays.asList(ThemeHelper.THEME_NAMES);
            ThemedArrayAdapter<String> adapter = new ThemedArrayAdapter<>(
                    this, p, themeNameList);
            int currentSel = spinnerTheme.getSelectedItemPosition();
            spinnerTheme.setAdapter(adapter);
            spinnerTheme.setSelection(Math.max(currentSel, 0));
            spinnerTheme.setPopupBackgroundDrawable(popupBg);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
