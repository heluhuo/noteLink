package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.dao.LinkDao;
import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Link;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.LinkParser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * EditActivity — 新建 / 编辑笔记页
 * 功能：富文本输入、内链实时检测提示、日期选择、保存
 */
public class EditActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDate, etCategory, etTags;
    private EditText etContent;
    private View tvLinkHint;
    private int editNoteId = -1; // -1 表示新建

    private static final Pattern LINK_DETECT_PATTERN = Pattern.compile("\\[\\[");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 视图引用
        etTitle    = findViewById(R.id.et_title);
        etDate     = findViewById(R.id.et_date);
        etCategory = findViewById(R.id.et_category);
        etTags     = findViewById(R.id.et_tags);
        etContent  = findViewById(R.id.et_content);
        tvLinkHint = findViewById(R.id.tv_link_hint);

        // 日期选择器
        etDate.setFocusable(false);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(today);
        etDate.setOnClickListener(v -> showDatePicker());

        // 内链提示：输入 [[ 时显示提示条
        etContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasLink = LINK_DETECT_PATTERN.matcher(s).find();
                tvLinkHint.setVisibility(hasLink ? View.VISIBLE : View.GONE);
            }
        });

        // 保存按钮
        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveNote());

        // 检查是否为编辑模式
        editNoteId = getIntent().getIntExtra("note_id", -1);
        if (editNoteId != -1) {
            toolbar.setTitle(R.string.title_edit);
            loadNoteForEdit(editNoteId);
        } else {
            toolbar.setTitle(R.string.title_new_note);
            if (getIntent().hasExtra("preset_date")) {
                etDate.setText(getIntent().getStringExtra("preset_date"));
            }
        }
    }

    private void showDatePicker() {
        String current = etDate.getText() != null ? etDate.getText().toString() : "";
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            cal.setTime(sdf.parse(current));
        } catch (Exception ignored) {}
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            etDate.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadNoteForEdit(int noteId) {
        executor.execute(() -> {
            Note note = AppDatabase.getInstance(this).noteDao().findById(noteId);
            if (note != null) {
                runOnUiThread(() -> {
                    etTitle.setText(note.title);
                    etDate.setText(note.date);
                    etCategory.setText(note.category);
                    etTags.setText(note.tags);
                    etContent.setText(note.content);
                });
            }
        });
    }

    private void saveNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.msg_title_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String content  = etContent.getText() != null ? etContent.getText().toString() : "";
        String date     = etDate.getText() != null ? etDate.getText().toString() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "日记";
        String tags     = etTags.getText() != null ? etTags.getText().toString().trim() : "";
        if (category.isEmpty()) category = "日记";
        if (date.isEmpty()) {
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        final String finalDate     = date;
        final String finalCategory = category;

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            com.example.myapplication.data.dao.NoteDao noteDao = db.noteDao();
            LinkDao linkDao = db.linkDao();

            Note note;
            if (editNoteId == -1) {
                // 新建
                note = new Note(title, content, finalDate, finalCategory, tags);
                int newId = (int) db.noteDao().insert(note);
                note.id = newId;
            } else {
                // 更新
                note = db.noteDao().findById(editNoteId);
                if (note == null) return;
                note.title    = title;
                note.content  = content;
                note.date     = finalDate;
                note.category = finalCategory;
                note.tags     = tags;
                note.updatedAt = System.currentTimeMillis();
                db.noteDao().update(note);
                // 先删旧的内链记录
                linkDao.deleteByFromNoteId(editNoteId);
            }

            // 解析并保存内链
            List<String> linkTitles = LinkParser.extractLinkTitles(content);
            for (String lt : linkTitles) {
                linkDao.insert(new Link(note.id, lt));
            }

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.msg_save_success, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
