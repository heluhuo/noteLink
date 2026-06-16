package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.LinkParser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DetailActivity — 笔记详情/阅读页
 * 功能：渲染笔记内容，内链高亮可点击，支持跳转编辑
 */
public class DetailActivity extends AppCompatActivity {

    private int noteId = -1;
    private Note currentNote;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 编辑按钮
        MaterialButton btnEdit = findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(v -> {
            if (currentNote != null) {
                Intent intent = new Intent(this, EditActivity.class);
                intent.putExtra("note_id", currentNote.id);
                startActivity(intent);
            }
        });

        noteId = getIntent().getIntExtra("note_id", -1);
        loadNote();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNote(); // 从编辑页返回后刷新
    }

    private void loadNote() {
        if (noteId == -1) { finish(); return; }
        executor.execute(() -> {
            Note note = AppDatabase.getInstance(this).noteDao().findById(noteId);
            if (note == null) { finish(); return; }
            currentNote = note;
            runOnUiThread(() -> bindNote(note));
        });
    }

    private void bindNote(Note note) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(note.title != null ? note.title : "");

        TextView tvTitle   = findViewById(R.id.tv_detail_title);
        TextView tvDate    = findViewById(R.id.tv_detail_date);
        TextView tvCat     = findViewById(R.id.tv_detail_category);
        TextView tvTags    = findViewById(R.id.tv_detail_tags);
        TextView tvContent = findViewById(R.id.tv_detail_content);

        tvTitle.setText(note.title);
        tvDate.setText(note.date != null ? note.date : "");
        tvCat.setText(note.category != null ? note.category : "");
        tvTags.setText(note.tags != null && !note.tags.isEmpty() ? "🏷 " + note.tags : "");

        // 内链解析
        int linkColor = ContextCompat.getColor(this, R.color.nl_link);
        tvContent.setText(LinkParser.parse(this, note.content, linkColor));
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
